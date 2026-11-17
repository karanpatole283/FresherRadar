package com.fresherradar.service;

import com.fresherradar.classifier.ClassificationResult;
import com.fresherradar.classifier.JobClassifier;
import com.fresherradar.entity.ClassificationRecord;
import com.fresherradar.entity.Job;
import com.fresherradar.entity.JobStatus;
import com.fresherradar.repository.ClassificationRecordRepository;
import com.fresherradar.repository.JobRepository;
import com.fresherradar.util.TextUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
public class ClassificationService {

    private static final Logger log = LoggerFactory.getLogger(ClassificationService.class);
    static final int MAX_ATTEMPTS = 3;

    private final JobClassifier classifier;
    private final JobRepository jobs;
    private final ClassificationRecordRepository records;
    private final CostCalculator costCalculator;
    private final TransactionTemplate tx;
    private final int batchLimit;
    private final double minConfidence;
    private final String model;

    public ClassificationService(JobClassifier classifier, JobRepository jobs, ClassificationRecordRepository records,
                                 CostCalculator costCalculator, TransactionTemplate tx,
                                 com.fresherradar.classifier.LlmClient llm,
                                 @Value("${fresherradar.llm.batch-limit-per-run}") int batchLimit,
                                 @Value("${fresherradar.llm.min-confidence}") double minConfidence) {
        this.classifier = classifier;
        this.jobs = jobs;
        this.records = records;
        this.costCalculator = costCalculator;
        this.tx = tx;
        this.batchLimit = batchLimit;
        this.minConfidence = minConfidence;
        this.model = llm.modelName();
    }

    public record Summary(int attempted, int active, int rejected, int duplicates, int failed, boolean llmAvailable) {}

    public Summary classifyPending() {
        if (!classifier.isAvailable()) {
            log.warn("LLM_API_KEY is not set: collected jobs stay PENDING and are not shown to users.");
            return new Summary(0, 0, 0, 0, 0, false);
        }
        List<Job> pending = jobs.findByStatusAndClassificationAttemptsLessThanOrderByIdAsc(
                JobStatus.PENDING, MAX_ATTEMPTS, PageRequest.of(0, batchLimit));

        int active = 0, rejected = 0, duplicates = 0, failed = 0;
        for (Job job : pending) {
            try {
                JobStatus status = classifyOne(job);
                switch (status) {
                    case ACTIVE -> active++;
                    case REJECTED -> rejected++;
                    case DUPLICATE -> duplicates++;
                    default -> failed++;
                }
            } catch (Exception e) {
                failed++;
                log.warn("Classification of job {} failed: {}", job.getId(), e.getMessage());
                markAttemptFailed(job, e.getMessage());
            }
        }
        log.info("Classification finished: {} attempted, {} active, {} rejected, {} duplicates, {} failed",
                pending.size(), active, rejected, duplicates, failed);
        return new Summary(pending.size(), active, rejected, duplicates, failed, true);
    }

    private JobStatus classifyOne(Job job) {
        JobClassifier.Outcome outcome = classifier.classify(job.getTitle(), job.getCompany(), job.getLocation(), job.getDescription());

        return tx.execute(txStatus -> {
            ClassificationRecord rec = new ClassificationRecord();
            rec.setJobId(job.getId());
            rec.setModel(model);
            rec.setInputTokens(outcome.usage().inputTokens());
            rec.setOutputTokens(outcome.usage().outputTokens());
            rec.setCostUsd(costCalculator.cost(outcome.usage().inputTokens(), outcome.usage().outputTokens()));
            rec.setLatencyMs(outcome.usage().latencyMs());
            rec.setSuccess(outcome.success());
            rec.setCreatedAt(Instant.now());
            records.save(rec);

            job.setClassificationAttempts(job.getClassificationAttempts() + 1);
            if (!outcome.success()) {
                job.setClassificationReason(TextUtils.truncate(outcome.error(), 480));
                if (job.getClassificationAttempts() >= MAX_ATTEMPTS) job.setStatus(JobStatus.FAILED);
                jobs.save(job);
                return job.getStatus();
            }
            apply(job, outcome.result());
            jobs.save(job);
            return job.getStatus();
        });
    }

    private void apply(Job job, ClassificationResult r) {
        job.setClassifiedAt(Instant.now());
        job.setClassificationConfidence(r.confidence());
        job.setClassificationReason(r.reason());

        if (!r.fresherEligible() || r.confidence() < minConfidence) {
            job.setStatus(JobStatus.REJECTED);
            return;
        }
        if (!r.company().isBlank()) job.setCompany(TextUtils.truncate(r.company(), 200));
        String city = !r.city().isBlank() ? r.city()
                : (job.getLocation() != null && !job.getLocation().isBlank() ? job.getLocation() : "India");
        job.setLocation(TextUtils.truncate(city, 120));
        job.setCategory(r.category());
        job.setEmploymentType(r.employmentType());
        job.setExperience(r.experience());
        job.setSkills(new ArrayList<>(r.skills()));
        job.setEligibility(new ArrayList<>(r.eligibility()));
        job.setSearchText(buildSearchText(job));

        // Different sites spell the same employer/city differently; compare again on the cleaned values.
        String fingerprint = TextUtils.fingerprint(job.getTitle(), job.getCompany(), job.getLocation());
        job.setFingerprint(fingerprint);
        boolean duplicate = jobs.existsByFingerprintAndStatusAndIdNot(fingerprint, JobStatus.ACTIVE, job.getId());
        job.setStatus(duplicate ? JobStatus.DUPLICATE : JobStatus.ACTIVE);
    }

    static String buildSearchText(Job job) {
        StringBuilder sb = new StringBuilder();
        sb.append(job.getTitle()).append(' ').append(job.getCompany()).append(' ')
                .append(job.getCategory()).append(' ').append(job.getLocation());
        if (job.getSkills() != null) job.getSkills().forEach(s -> sb.append(' ').append(s));
        return sb.toString().toLowerCase(Locale.ROOT);
    }

    private void markAttemptFailed(Job job, String message) {
        try {
            tx.executeWithoutResult(s -> {
                job.setClassificationAttempts(job.getClassificationAttempts() + 1);
                job.setClassificationReason(TextUtils.truncate(message, 480));
                if (job.getClassificationAttempts() >= MAX_ATTEMPTS) job.setStatus(JobStatus.FAILED);
                jobs.save(job);
            });
        } catch (Exception e) {
            log.error("Could not record classification failure for job {}", job.getId(), e);
        }
    }
}

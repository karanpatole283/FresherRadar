
package com.fresherradar.service;

import com.fresherradar.collector.JobCollector;
import com.fresherradar.collector.RawJob;
import com.fresherradar.entity.CollectionRun;
import com.fresherradar.entity.Job;
import com.fresherradar.entity.JobStatus;
import com.fresherradar.repository.CollectionRunRepository;
import com.fresherradar.repository.JobRepository;
import com.fresherradar.util.IndiaLocationFilter;
import com.fresherradar.util.TextUtils;
import com.fresherradar.util.UrlUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
public class CollectionService {

    private static final Logger log =
            LoggerFactory.getLogger(CollectionService.class);

    private static final int IN_CLAUSE_CHUNK = 500;

    private final List<JobCollector> collectors;
    private final JobRepository jobs;
    private final CollectionRunRepository runs;
    private final ClassificationService classification;
    private final Duration maxAge;

    private final AtomicBoolean running = new AtomicBoolean(false);

    public CollectionService(
            List<JobCollector> collectors,
            JobRepository jobs,
            CollectionRunRepository runs,
            ClassificationService classification,
            @Value("${fresherradar.collection.max-job-age-days}")
            int maxAgeDays) {

        this.collectors = collectors;
        this.jobs = jobs;
        this.runs = runs;
        this.classification = classification;
        this.maxAge = Duration.ofDays(maxAgeDays);
    }

    public record RefreshSummary(
            boolean started,
            String message,
            int sourcesRun,
            int fetched,
            int inserted,
            int duplicates,
            ClassificationService.Summary classification) {
    }

    private record IngestResult(
            int inserted,
            int duplicates,
            int locationRejected,
            int staleOrInvalid) {
    }

    public boolean isRunning() {
        return running.get();
    }

    public RefreshSummary refreshAll() {

        if (!running.compareAndSet(false, true)) {
            return new RefreshSummary(
                    false,
                    "A refresh is already running.",
                    0, 0, 0, 0, null
            );
        }

        try {
            int sources = 0;
            int fetched = 0;
            int inserted = 0;
            int duplicates = 0;

            for (JobCollector collector : collectors) {

                if (!collector.isEnabled()) {
                    log.info(
                            "Source {} is disabled or not configured - skipped",
                            collector.name()
                    );
                    continue;
                }

                sources++;

                int[] counts = runCollector(collector);

                fetched += counts[0];
                inserted += counts[1];
                duplicates += counts[2];
            }

            ClassificationService.Summary summary =
                    classification.classifyPending();

            String message = sources == 0
                    ? "No source is enabled. Configure source settings."
                    : "Refresh completed.";

            return new RefreshSummary(
                    true,
                    message,
                    sources,
                    fetched,
                    inserted,
                    duplicates,
                    summary
            );

        } finally {
            running.set(false);
        }
    }

    private int[] runCollector(JobCollector collector) {

        CollectionRun run = new CollectionRun();

        run.setSource(collector.name());
        run.setStartedAt(Instant.now());
        run.setStatus("RUNNING");

        run = runs.save(run);

        try {

            List<RawJob> raw = collector.collect();

            IngestResult result = ingest(raw);

            run.setFetched(raw.size());
            run.setInserted(result.inserted());
            run.setDuplicates(result.duplicates());
            run.setLocationRejected(result.locationRejected());
            run.setStaleOrInvalid(result.staleOrInvalid());
            run.setStatus("SUCCESS");

            return new int[]{
                    raw.size(),
                    result.inserted(),
                    result.duplicates()
            };

        } catch (Exception e) {

            log.error(
                    "Collection from {} failed",
                    collector.name(),
                    e
            );

            run.setStatus("FAILED");
            run.setErrorMessage(
                    TextUtils.truncate(e.getMessage(), 900)
            );

            return new int[]{0, 0, 0};

        } finally {

            run.setFinishedAt(Instant.now());
            runs.save(run);
        }
    }

    private IngestResult ingest(List<RawJob> raw) {

        Instant now = Instant.now();
        Instant oldest = now.minus(maxAge);

        Map<String, Job> byUrlKey = new LinkedHashMap<>();
        Set<String> fingerprintsInBatch = new HashSet<>();

        int duplicates = 0;
        int locationRejected = 0;
        int staleOrInvalid = 0;

        for (RawJob r : raw) {

            if (r.title() == null
                    || r.title().isBlank()
                    || !UrlUtils.isHttpUrl(r.applyUrl())) {

                staleOrInvalid++;
                continue;
            }

            // Unknown publication dates are not treated as fresh jobs.
            if (r.postedAt() == null) {
                staleOrInvalid++;
                continue;
            }

            // Reject jobs older than the configured freshness window.
            if (r.postedAt().isBefore(oldest)) {
                staleOrInvalid++;
                continue;
            }

            // Reject invalid future publication dates.
            if (r.postedAt().isAfter(
                    now.plus(Duration.ofDays(1)))) {

                staleOrInvalid++;
                continue;
            }

            if (!IndiaLocationFilter.isIndiaBased(r.location())) {
                locationRejected++;
                continue;
            }

            String urlKey = TextUtils.sha256(
                    UrlUtils.canonicalize(r.applyUrl())
            );

            String company = r.company() == null
                    || r.company().isBlank()
                    ? "Unknown company"
                    : r.company().trim();

            String fingerprint = TextUtils.fingerprint(
                    r.title(),
                    company,
                    r.location()
            );

            if (byUrlKey.containsKey(urlKey)
                    || !fingerprintsInBatch.add(fingerprint)) {

                duplicates++;
                continue;
            }

            Job job = new Job();

            job.setTitle(
                    TextUtils.truncate(r.title().trim(), 300)
            );

            job.setCompany(
                    TextUtils.truncate(company, 200)
            );

            job.setLocation(
                    TextUtils.truncate(r.location(), 120)
            );

            job.setSource(
                    TextUtils.truncate(r.source(), 80)
            );

            job.setSourceJobId(
                    TextUtils.truncate(r.sourceJobId(), 200)
            );

            job.setApplyUrl(
                    TextUtils.truncate(r.applyUrl().trim(), 2000)
            );

            job.setApplyUrlKey(urlKey);
            job.setFingerprint(fingerprint);

            job.setDescription(
                    TextUtils.truncate(r.description(), 8000)
            );

            job.setPostedAt(r.postedAt());
            job.setCollectedAt(now);
            job.setFirstSeenAt(now);

            job.setStatus(JobStatus.PENDING);

            job.setSearchText(
                    ClassificationService.buildSearchText(job)
            );

            byUrlKey.put(urlKey, job);
        }

        Set<String> existingKeys = existing(
                new ArrayList<>(byUrlKey.keySet()),
                true
        );

        List<String> fingerprints = byUrlKey.values()
                .stream()
                .map(Job::getFingerprint)
                .distinct()
                .toList();

        Set<String> existingFingerprints = existing(
                fingerprints,
                false
        );

        List<Job> toSave = new ArrayList<>();

        for (Map.Entry<String, Job> entry : byUrlKey.entrySet()) {

            if (existingKeys.contains(entry.getKey())
                    || existingFingerprints.contains(
                    entry.getValue().getFingerprint())) {

                duplicates++;

            } else {
                toSave.add(entry.getValue());
            }
        }

        jobs.saveAll(toSave);

        log.info(
                "Ingest: {} raw, {} stored, {} duplicates, "
                        + "{} non-India locations rejected, "
                        + "{} stale/invalid/unknown-date jobs skipped",
                raw.size(),
                toSave.size(),
                duplicates,
                locationRejected,
                staleOrInvalid
        );

        return new IngestResult(
                toSave.size(),
                duplicates,
                locationRejected,
                staleOrInvalid
        );
    }

    private Set<String> existing(
            List<String> values,
            boolean urlKeys) {

        Set<String> found = new HashSet<>();

        for (int i = 0; i < values.size(); i += IN_CLAUSE_CHUNK) {

            List<String> chunk = values.subList(
                    i,
                    Math.min(values.size(), i + IN_CLAUSE_CHUNK)
            );

            found.addAll(
                    urlKeys
                            ? jobs.findExistingApplyUrlKeys(chunk)
                            : jobs.findExistingFingerprints(chunk)
            );
        }

        return found;
    }
}

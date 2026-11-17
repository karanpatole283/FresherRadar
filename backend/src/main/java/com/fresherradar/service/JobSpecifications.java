package com.fresherradar.service;

import com.fresherradar.dto.JobSearchCriteria;
import com.fresherradar.entity.Job;
import com.fresherradar.entity.JobStatus;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class JobSpecifications {

    private JobSpecifications() {}

    public static Specification<Job> from(JobSearchCriteria c) {
        return (root, query, cb) -> {
            List<Predicate> p = new ArrayList<>();
            p.add(cb.equal(root.get("status"), JobStatus.ACTIVE));

            if (notBlank(c.location())) p.add(cb.equal(root.get("location"), c.location().trim()));
            if (notBlank(c.category())) p.add(cb.equal(root.get("category"), c.category().trim()));
            if (notBlank(c.employmentType())) p.add(cb.equal(root.get("employmentType"), c.employmentType().trim()));
            if (notBlank(c.experience())) p.add(cb.equal(root.get("experience"), c.experience().trim()));
            if (notBlank(c.company())) p.add(cb.equal(cb.lower(root.get("company")), c.company().trim().toLowerCase(Locale.ROOT)));

            if (c.postedWithinHours() != null) {
                Instant since = Instant.now().minus(c.postedWithinHours(), ChronoUnit.HOURS);
                p.add(cb.or(
                        cb.greaterThanOrEqualTo(root.<Instant>get("postedAt"), since),
                        cb.and(cb.isNull(root.get("postedAt")),
                                cb.greaterThanOrEqualTo(root.<Instant>get("collectedAt"), since))));
            }

            if (notBlank(c.search())) {
                // every word must appear somewhere in title/company/category/location/skills (same rule as the frontend)
                for (String term : c.search().trim().toLowerCase(Locale.ROOT).split("\\s+")) {
                    String escaped = term.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
                    p.add(cb.like(root.<String>get("searchText"), "%" + escaped + "%", '\\'));
                }
            }
            return cb.and(p.toArray(new Predicate[0]));
        };
    }

    private static boolean notBlank(String s) { return s != null && !s.isBlank(); }
}

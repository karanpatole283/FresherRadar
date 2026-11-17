package com.fresherradar.repository;

import com.fresherradar.entity.Job;
import com.fresherradar.entity.JobStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Set;

public interface JobRepository extends JpaRepository<Job, Long>, JpaSpecificationExecutor<Job> {

    @Query("select j.applyUrlKey from Job j where j.applyUrlKey in :keys")
    Set<String> findExistingApplyUrlKeys(@Param("keys") Collection<String> keys);

    @Query("select j.fingerprint from Job j where j.fingerprint in :fingerprints")
    Set<String> findExistingFingerprints(@Param("fingerprints") Collection<String> fingerprints);

    boolean existsByFingerprintAndStatusAndIdNot(String fingerprint, JobStatus status, Long id);

    List<Job> findByStatusAndClassificationAttemptsLessThanOrderByIdAsc(JobStatus status, int maxAttempts, Pageable pageable);

    long countByStatus(JobStatus status);

    @Query("select max(j.collectedAt) from Job j where j.status = com.fresherradar.entity.JobStatus.ACTIVE")
    Instant findLatestCollectedAt();

    @Query("select distinct j.location from Job j where j.status = com.fresherradar.entity.JobStatus.ACTIVE and j.location is not null order by j.location")
    List<String> findDistinctLocations();

    @Query("select distinct j.category from Job j where j.status = com.fresherradar.entity.JobStatus.ACTIVE and j.category is not null order by j.category")
    List<String> findDistinctCategories();
}

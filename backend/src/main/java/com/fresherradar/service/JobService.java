package com.fresherradar.service;

import com.fresherradar.dto.JobDto;
import com.fresherradar.dto.JobListResponse;
import com.fresherradar.dto.JobSearchCriteria;
import com.fresherradar.dto.MetaResponse;
import com.fresherradar.entity.Job;
import com.fresherradar.entity.JobStatus;
import com.fresherradar.exception.ResourceNotFoundException;
import com.fresherradar.mapper.JobMapper;
import com.fresherradar.repository.CollectionRunRepository;
import com.fresherradar.repository.JobRepository;
import com.fresherradar.util.Taxonomy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@Transactional(readOnly = true)
public class JobService {

    private final JobRepository jobs;
    private final CollectionRunRepository runs;
    private final JobMapper mapper;

    public JobService(JobRepository jobs, CollectionRunRepository runs, JobMapper mapper) {
        this.jobs = jobs;
        this.runs = runs;
        this.mapper = mapper;
    }

    public JobListResponse search(JobSearchCriteria criteria, int page, int size) {
        boolean oldestFirst = "oldest".equalsIgnoreCase(criteria.sort());
        Sort.Order primary = oldestFirst ? Sort.Order.asc("postedAt").nullsLast() : Sort.Order.desc("postedAt").nullsLast();
        Sort sort = Sort.by(primary, oldestFirst ? Sort.Order.asc("id") : Sort.Order.desc("id"));

        Page<Job> result = jobs.findAll(JobSpecifications.from(criteria), PageRequest.of(page, size, sort));
        return new JobListResponse(lastUpdated(),
                result.getContent().stream().map(mapper::toDto).toList(),
                result.getNumber(), result.getSize(), result.getTotalElements(), result.getTotalPages());
    }

    public JobDto get(Long id) {
        Job job = jobs.findById(id)
                .filter(j -> j.getStatus() == JobStatus.ACTIVE)
                .orElseThrow(() -> new ResourceNotFoundException("Job " + id + " was not found."));
        return mapper.toDto(job);
    }

    public MetaResponse meta() {
        return new MetaResponse(jobs.findDistinctLocations(), jobs.findDistinctCategories(),
                Taxonomy.EMPLOYMENT_TYPES, Taxonomy.EXPERIENCE_LEVELS);
    }

    /** When the data was last refreshed: last successful collection run, else newest collected job, else null. */
    public Instant lastUpdated() {
        Instant finished = runs.findLastSuccessfulFinish();
        return finished != null ? finished : jobs.findLatestCollectedAt();
    }
}

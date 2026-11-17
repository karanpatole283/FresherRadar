package com.fresherradar.mapper;

import com.fresherradar.dto.JobDto;
import com.fresherradar.entity.Job;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class JobMapper {

    public JobDto toDto(Job job) {
        return new JobDto(
                job.getId(),
                job.getTitle(),
                job.getCompany(),
                job.getLocation(),
                job.getCategory(),
                job.getEmploymentType(),
                job.getExperience(),
                // the frontend sorts on postedAt, so fall back to the collection time when the source gave none
                job.getPostedAt() != null ? job.getPostedAt() : job.getCollectedAt(),
                job.getCollectedAt(),
                job.getSource(),
                job.getApplyUrl(),
                job.getDescription(),
                job.getSkills() == null ? List.of() : job.getSkills(),
                job.getEligibility() == null ? List.of() : job.getEligibility());
    }
}

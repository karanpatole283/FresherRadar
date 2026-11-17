package com.fresherradar.dto;

import java.time.Instant;
import java.util.List;

/** `lastUpdated` and `jobs` are what the frontend reads; the rest is pagination metadata. */
public record JobListResponse(
        Instant lastUpdated,
        List<JobDto> jobs,
        int page,
        int size,
        long totalElements,
        int totalPages) {
}

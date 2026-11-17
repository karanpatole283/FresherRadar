package com.fresherradar.dto;

import java.time.Instant;
import java.util.List;

/** Field names are the contract with the Stage 1 frontend. */
public record JobDto(
        Long id,
        String title,
        String company,
        String location,
        String category,
        String employmentType,
        String experience,
        Instant postedAt,
        Instant collectedAt,
        String source,
        String applyUrl,
        String description,
        List<String> skills,
        List<String> eligibility) {
}

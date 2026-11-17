package com.fresherradar.dto;

public record JobSearchCriteria(
        String search,
        String location,
        String category,
        String employmentType,
        String experience,
        String company,
        Integer postedWithinHours,
        String sort) {
}

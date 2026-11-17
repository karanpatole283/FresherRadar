package com.fresherradar.classifier;

import java.util.List;

/** Validated output of the LLM. Every enum-like field already matches the frontend vocabulary. */
public record ClassificationResult(
        boolean fresherEligible,
        String employmentType,
        String experience,
        String category,
        String city,
        String company,
        List<String> skills,
        List<String> eligibility,
        double confidence,
        String reason) {
}

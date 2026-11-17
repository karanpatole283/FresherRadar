package com.fresherradar.dto;

import java.math.BigDecimal;
import java.util.List;

public record EvaluationResult(
        int samples,
        int classified,
        int failed,
        double fresherEligibleAccuracy,
        double categoryAccuracy,
        double employmentTypeAccuracy,
        BigDecimal costUsd,
        BigDecimal costUsdPer1000Jobs,
        List<String> mismatches) {
}

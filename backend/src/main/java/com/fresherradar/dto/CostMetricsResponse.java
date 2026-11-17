package com.fresherradar.dto;

import java.math.BigDecimal;

/** Measured from classification_records. Nothing here is estimated or hard-coded. */
public record CostMetricsResponse(
        long successfulCalls,
        long failedCalls,
        long totalInputTokens,
        long totalOutputTokens,
        BigDecimal totalCostUsd,
        BigDecimal costUsdPer1000Jobs,
        String note) {
}

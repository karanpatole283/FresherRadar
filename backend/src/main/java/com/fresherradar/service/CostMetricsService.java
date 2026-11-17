package com.fresherradar.service;

import com.fresherradar.dto.CostMetricsResponse;
import com.fresherradar.repository.ClassificationRecordRepository;
import com.fresherradar.repository.CostMetricsAggregate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
public class CostMetricsService {

    private final ClassificationRecordRepository records;
    private final CostCalculator calculator;

    public CostMetricsService(ClassificationRecordRepository records, CostCalculator calculator) {
        this.records = records;
        this.calculator = calculator;
    }

    /** Cost per 1,000 LLM calls, including failed calls. */
    public CostMetricsResponse compute() {

        CostMetricsAggregate metrics = records.getCostMetricsAggregate();

        long totalCalls = metrics.getTotalCalls();
        long successfulCalls = metrics.getSuccessfulCalls();
        long failedCalls = totalCalls - successfulCalls;

        long inputTokens = metrics.getTotalInputTokens();
        long outputTokens = metrics.getTotalOutputTokens();

        BigDecimal totalCost = metrics.getTotalCostUsd();

        BigDecimal per1000 = totalCalls == 0
                ? BigDecimal.ZERO
                : totalCost.multiply(BigDecimal.valueOf(1000))
                    .divide(BigDecimal.valueOf(totalCalls), 4, RoundingMode.HALF_UP);

        String note;

        if (totalCalls == 0) {
            note = "No LLM calls recorded yet. Run a refresh with LLM_API_KEY configured.";
        } else if (!calculator.pricesConfigured()) {
            note = "Token usage is measured, but LLM_INPUT_PRICE_PER_M / LLM_OUTPUT_PRICE_PER_M are 0, so cost shows 0. Set your provider's current prices.";
        } else {
            note = "Computed from provider-reported token usage and the configured per-million-token prices.";
        }

        return new CostMetricsResponse(
                successfulCalls,
                failedCalls,
                inputTokens,
                outputTokens,
                totalCost.setScale(6, RoundingMode.HALF_UP),
                per1000,
                note
        );
    }
}
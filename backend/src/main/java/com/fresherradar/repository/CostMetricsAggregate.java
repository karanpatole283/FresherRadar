package com.fresherradar.repository;

import java.math.BigDecimal;

public interface CostMetricsAggregate {

    Long getTotalCalls();

    Long getSuccessfulCalls();

    Long getTotalInputTokens();

    Long getTotalOutputTokens();

    BigDecimal getTotalCostUsd();
}
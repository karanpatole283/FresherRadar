package com.fresherradar.repository;

import com.fresherradar.entity.ClassificationRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ClassificationRecordRepository extends JpaRepository<ClassificationRecord, Long> {

    List<ClassificationRecord> findBySuccessTrue();

    long countBySuccessFalse();

    @Query(value = """
            SELECT
                COUNT(*) AS "totalCalls",
                COUNT(*) FILTER (WHERE success = true) AS "successfulCalls",
                COALESCE(SUM(input_tokens), 0) AS "totalInputTokens",
                COALESCE(SUM(output_tokens), 0) AS "totalOutputTokens",
                COALESCE(SUM(cost_usd), 0) AS "totalCostUsd"
            FROM classification_records
            """, nativeQuery = true)
    CostMetricsAggregate getCostMetricsAggregate();
}
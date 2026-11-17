package com.fresherradar.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

/** One row per LLM call. Source of truth for the cost-per-1,000-jobs metric. */
@Entity
@Table(name = "classification_records")
public class ClassificationRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "job_id") private Long jobId;
    @Column(nullable = false, length = 100) private String model;
    @Column(name = "input_tokens", nullable = false) private int inputTokens;
    @Column(name = "output_tokens", nullable = false) private int outputTokens;
    @Column(name = "cost_usd", nullable = false, precision = 14, scale = 8) private BigDecimal costUsd = BigDecimal.ZERO;
    @Column(nullable = false) private boolean success;
    @Column(name = "latency_ms") private Long latencyMs;
    @Column(name = "created_at", nullable = false) private Instant createdAt;

    public Long getId() { return id; }
    public Long getJobId() { return jobId; }
    public void setJobId(Long jobId) { this.jobId = jobId; }
    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }
    public int getInputTokens() { return inputTokens; }
    public void setInputTokens(int inputTokens) { this.inputTokens = inputTokens; }
    public int getOutputTokens() { return outputTokens; }
    public void setOutputTokens(int outputTokens) { this.outputTokens = outputTokens; }
    public BigDecimal getCostUsd() { return costUsd; }
    public void setCostUsd(BigDecimal costUsd) { this.costUsd = costUsd; }
    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }
    public Long getLatencyMs() { return latencyMs; }
    public void setLatencyMs(Long latencyMs) { this.latencyMs = latencyMs; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}

package com.fresherradar.dto;

import java.time.Instant;

public record HealthResponse(String status, String database, long activeJobs, long pendingJobs,
                             Instant lastCollectionAt, boolean llmConfigured, Instant serverTime) {
}

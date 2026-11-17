package com.fresherradar.controller;

import com.fresherradar.classifier.LlmClient;
import com.fresherradar.dto.HealthResponse;
import com.fresherradar.entity.JobStatus;
import com.fresherradar.repository.CollectionRunRepository;
import com.fresherradar.repository.JobRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

@RestController
@RequestMapping("/api")
public class HealthController {

    private final JobRepository jobs;
    private final CollectionRunRepository runs;
    private final LlmClient llm;

    public HealthController(JobRepository jobs, CollectionRunRepository runs, LlmClient llm) {
        this.jobs = jobs;
        this.runs = runs;
        this.llm = llm;
    }

    @GetMapping("/health")
    public ResponseEntity<HealthResponse> health() {
        try {
            long active = jobs.countByStatus(JobStatus.ACTIVE);
            long pending = jobs.countByStatus(JobStatus.PENDING);
            return ResponseEntity.ok(new HealthResponse("UP", "UP", active, pending,
                    runs.findLastSuccessfulFinish(), llm.isConfigured(), Instant.now()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(new HealthResponse("DEGRADED", "DOWN", 0, 0, null, llm.isConfigured(), Instant.now()));
        }
    }
}

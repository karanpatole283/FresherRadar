
package com.fresherradar.controller;

import com.fresherradar.dto.CostMetricsResponse;
import com.fresherradar.dto.EvaluationResult;
import com.fresherradar.dto.CollectionRunDto;
import com.fresherradar.repository.CollectionRunRepository;
import com.fresherradar.service.CollectionService;
import com.fresherradar.service.ClassificationService;
import com.fresherradar.service.CostMetricsService;
import com.fresherradar.service.EvaluationService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private static final Logger log =
            LoggerFactory.getLogger(AdminController.class);

    private final CollectionService collection;
    private final ClassificationService classification;
    private final CostMetricsService metrics;
    private final EvaluationService evaluation;
    private final CollectionRunRepository runs;
    private final String adminToken;

    public AdminController(
            CollectionService collection,
            ClassificationService classification,
            CostMetricsService metrics,
            EvaluationService evaluation,
            CollectionRunRepository runs,
            @Value("${fresherradar.admin.token}") String adminToken) {

        this.collection = collection;
        this.classification = classification;
        this.metrics = metrics;
        this.evaluation = evaluation;
        this.runs = runs;
        this.adminToken = adminToken;
    }

    /**
     * Starts a refresh in the background and returns immediately.
     * This also collects new jobs.
     */
    @PostMapping("/refresh")
    public ResponseEntity<Map<String, Object>> refresh(
            @RequestHeader(
                    value = "X-Admin-Token",
                    required = false
            ) String token) {

        authorize(token);

        if (collection.isRunning()) {
            return ResponseEntity
                    .status(HttpStatus.CONFLICT)
                    .body(Map.of(
                            "message",
                            "A refresh is already running."
                    ));
        }

        CompletableFuture.runAsync(() -> {
            try {
                collection.refreshAll();
            } catch (Exception e) {
                log.error("Manual refresh failed", e);
            }
        });

        return ResponseEntity
                .accepted()
                .body(Map.of(
                        "message",
                        "Refresh started. Check /api/admin/runs and /api/health."
                ));
    }

    /**
     * Classifies existing pending jobs only.
     * Does not collect new jobs.
     */
    @PostMapping("/classify")
    public ClassificationService.Summary classify(
            @RequestHeader(
                    value = "X-Admin-Token",
                    required = false
            ) String token) {

        authorize(token);

        if (collection.isRunning()) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Cannot classify while a collection refresh is running."
            );
        }

        return classification.classifyPending();
    }

    /**
     * Returns the latest 20 collection runs.
     */
    @GetMapping("/runs")
    public List<CollectionRunDto> recentRuns(
            @RequestHeader(
                    value = "X-Admin-Token",
                    required = false
            ) String token) {

        authorize(token);

        return runs.findTop20ByOrderByStartedAtDesc()
                .stream()
                .map(r -> new CollectionRunDto(
                        r.getId(),
                        r.getSource(),
                        r.getStartedAt(),
                        r.getFinishedAt(),
                        r.getStatus(),
                        r.getFetched(),
                        r.getInserted(),
                        r.getDuplicates(),
                        r.getLocationRejected(),
                        r.getStaleOrInvalid(),
                        r.getErrorMessage()
                ))
                .toList();
    }

    /**
     * Returns LLM cost metrics.
     */
    @GetMapping("/metrics/cost")
    public CostMetricsResponse cost(
            @RequestHeader(
                    value = "X-Admin-Token",
                    required = false
            ) String token) {

        authorize(token);

        return metrics.compute();
    }

    /**
     * Runs the labelled-job evaluation.
     */
    @PostMapping("/evaluate")
    public EvaluationResult evaluate(
            @RequestHeader(
                    value = "X-Admin-Token",
                    required = false
            ) String token) {

        authorize(token);

        return evaluation.run();
    }

    /**
     * Validates the admin token.
     */
    private void authorize(String provided) {

        if (adminToken == null || adminToken.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Admin endpoints are disabled (ADMIN_TOKEN is not set)."
            );
        }

        boolean ok = provided != null && MessageDigest.isEqual(
                adminToken.getBytes(StandardCharsets.UTF_8),
                provided.getBytes(StandardCharsets.UTF_8)
        );

        if (!ok) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Missing or invalid admin token."
            );
        }
    }
}

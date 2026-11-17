
package com.fresherradar.collector;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Component
public class GreenhouseCollector implements JobCollector {

    private static final Logger log =
            LoggerFactory.getLogger(GreenhouseCollector.class);

    private static final Map<String, String> COMPANY_NAMES = Map.ofEntries(
            Map.entry("instawork", "Instawork"),
            Map.entry("karya", "Karya"),
            Map.entry("groww", "Groww"),
            Map.entry("formaaiinc3", "Forma.ai"),
            Map.entry("zinnov", "Zinnov"),
            Map.entry("inmobi", "InMobi"),
            Map.entry("druva", "Druva"),
            Map.entry("devrev", "DevRev"),
            Map.entry("netradyne", "Netradyne"),
            Map.entry("observeai", "Observe.AI"),
            Map.entry("airbnb", "Airbnb"),
            Map.entry("rubrik", "Rubrik"),
            Map.entry("discord", "Discord"),
            Map.entry("coinbase", "Coinbase"),
            Map.entry("bloomreach", "Bloomreach"),
            Map.entry("elastic", "Elastic"),
            Map.entry("stripe", "Stripe"),
            Map.entry("fivetran", "Fivetran"),
            Map.entry("gitlab", "GitLab"),
            Map.entry("worldquant", "WorldQuant"),
            Map.entry("twilio", "Twilio")
    );

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final boolean enabled;
    private final List<String> boards;

    public GreenhouseCollector(
            ObjectMapper objectMapper,
            @Value("${fresherradar.sources.greenhouse.enabled:false}")
            boolean enabled,
            @Value("${fresherradar.sources.greenhouse.boards:}")
            String boardConfig) {

        this.objectMapper = objectMapper;
        this.enabled = enabled;

        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(15))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();

        this.boards = Arrays.stream(boardConfig.split(","))
                .map(String::trim)
                .filter(board -> board.matches("[a-zA-Z0-9_-]+"))
                .distinct()
                .toList();
    }

    @Override
    public String name() {
        return "greenhouse";
    }

    @Override
    public boolean isEnabled() {
        return enabled && !boards.isEmpty();
    }

    @Override
    public List<RawJob> collect() {

        List<RawJob> allJobs = new ArrayList<>();

        for (String board : boards) {
            try {
                List<RawJob> boardJobs = fetchBoard(board);
                allJobs.addAll(boardJobs);

                log.info("Greenhouse board {} returned {} jobs",
                        board, boardJobs.size());

            } catch (Exception e) {
                log.warn("Greenhouse board {} failed: {}",
                        board, e.getMessage());
            }
        }

        log.info("Greenhouse bulk collection completed. Total jobs: {}",
                allJobs.size());

        return allJobs;
    }

    private List<RawJob> fetchBoard(String board) throws Exception {

        String url = "https://boards-api.greenhouse.io/v1/boards/"
                + board + "/jobs?content=true";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(20))
                .header("Accept", "application/json")
                .header("User-Agent", "FresherRadar/1.0")
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(
                request,
                HttpResponse.BodyHandlers.ofString()
        );

        if (response.statusCode() != 200) {
            throw new IllegalStateException(
                    "HTTP " + response.statusCode());
        }

        JsonNode root = objectMapper.readTree(response.body());
        JsonNode jobs = root.path("jobs");

        List<RawJob> results = new ArrayList<>();

        if (!jobs.isArray()) {
            return results;
        }

        for (JsonNode job : jobs) {

            String id = job.path("id").asText("");
            String title = job.path("title").asText("");
            String location = job.path("location")
                    .path("name").asText("");

            String applyUrl = job.path("absolute_url").asText("");

            String content = job.path("content").asText("");

            String description = org.jsoup.parser.Parser
                    .unescapeEntities(content, false);

            description = org.jsoup.Jsoup.parse(description).text();

            if (id.isBlank() || title.isBlank() || applyUrl.isBlank()) {
                continue;
            }

            String companyName =
                    COMPANY_NAMES.getOrDefault(board, board);

            Instant postedAt = parsePublishedDate(
                    job.path("first_published").asText(null),
                    board,
                    id
            );

            results.add(new RawJob(
                    "greenhouse",
                    board + "-" + id,
                    title,
                    companyName,
                    location,
                    description,
                    applyUrl,
                    postedAt
            ));
        }

        return results;
    }

    private Instant parsePublishedDate(
            String publishedDate,
            String board,
            String jobId) {

        if (publishedDate == null || publishedDate.isBlank()) {
            log.debug(
                    "No first_published date for Greenhouse job {} on {}",
                    jobId,
                    board
            );
            return null;
        }

        try {
            return OffsetDateTime.parse(publishedDate).toInstant();

        } catch (Exception e) {
            log.warn(
                    "Invalid first_published date for job {} on {}: {}",
                    jobId,
                    board,
                    publishedDate
            );
            return null;
        }
    }
}

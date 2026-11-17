package com.fresherradar.collector;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fresherradar.util.TextUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Official Adzuna job search API (https://developer.adzuna.com/). Requires your own app id and key.
 * Adzuna's terms require attribution ("Jobs by Adzuna") wherever its listings are displayed.
 */
@Component
public class AdzunaCollector implements JobCollector {

    private static final Logger log = LoggerFactory.getLogger(AdzunaCollector.class);

    private final HttpClient http;
    private final ObjectMapper mapper;
    private final boolean enabled;
    private final String appId;
    private final String appKey;
    private final String country;
    private final List<String> queries;
    private final int pagesPerQuery;
    private final int resultsPerPage;
    private final Duration timeout;

    public AdzunaCollector(HttpClient http, ObjectMapper mapper,
                           @Value("${fresherradar.sources.adzuna.enabled}") boolean enabled,
                           @Value("${fresherradar.sources.adzuna.app-id}") String appId,
                           @Value("${fresherradar.sources.adzuna.app-key}") String appKey,
                           @Value("${fresherradar.sources.adzuna.country}") String country,
                           @Value("${fresherradar.sources.adzuna.queries}") List<String> queries,
                           @Value("${fresherradar.sources.adzuna.pages-per-query}") int pagesPerQuery,
                           @Value("${fresherradar.sources.adzuna.results-per-page}") int resultsPerPage,
                           @Value("${fresherradar.collection.http-timeout-seconds}") int timeoutSeconds) {
        this.http = http;
        this.mapper = mapper;
        this.enabled = enabled;
        this.appId = appId;
        this.appKey = appKey;
        this.country = country;
        this.queries = queries;
        this.pagesPerQuery = pagesPerQuery;
        this.resultsPerPage = resultsPerPage;
        this.timeout = Duration.ofSeconds(timeoutSeconds);
    }

    @Override
    public String name() { return "Adzuna"; }

    @Override
    public boolean isEnabled() { return enabled && !appId.isBlank() && !appKey.isBlank(); }

    @Override
    public List<RawJob> collect() throws Exception {
        List<RawJob> jobs = new ArrayList<>();
        for (String query : queries) {
            for (int page = 1; page <= pagesPerQuery; page++) {
                JsonNode root = fetchPage(query.trim(), page);
                JsonNode results = root.path("results");
                if (!results.isArray() || results.isEmpty()) break;       // no more pages for this query
                for (JsonNode r : results) {
                    String url = r.path("redirect_url").asText("");
                    String title = r.path("title").asText("").trim();
                    if (url.isBlank() || title.isBlank()) continue;
                    Instant posted = null;
                    String created = r.path("created").asText("");
                    if (!created.isBlank()) {
                        try { posted = Instant.parse(created); } catch (Exception ignored) { /* leave null */ }
                    }
                    jobs.add(new RawJob(name(),
                            r.path("id").asText(null),
                            TextUtils.htmlToText(title),
                            r.path("company").path("display_name").asText("Unknown company"),
                            r.path("location").path("display_name").asText(""),
                            TextUtils.htmlToText(r.path("description").asText("")),
                            url,
                            posted));
                }
            }
        }
        log.info("Adzuna returned {} raw listings", jobs.size());
        return jobs;
    }

    private JsonNode fetchPage(String query, int page) throws Exception {
        String url = "https://api.adzuna.com/v1/api/jobs/" + country + "/search/" + page
                + "?app_id=" + enc(appId) + "&app_key=" + enc(appKey)
                + "&results_per_page=" + resultsPerPage
                + "&what=" + enc(query) + "&sort_by=date&content-type=application/json";
        HttpRequest request = HttpRequest.newBuilder(URI.create(url)).timeout(timeout)
                .header("Accept", "application/json").GET().build();
        HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            // never put the URL (it contains the key) into the message
            throw new IllegalStateException("Adzuna responded with HTTP " + response.statusCode());
        }
        return mapper.readTree(response.body());
    }

    private static String enc(String v) { return URLEncoder.encode(v, StandardCharsets.UTF_8); }
}

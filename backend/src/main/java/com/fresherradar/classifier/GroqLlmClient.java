package com.fresherradar.classifier;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fresherradar.exception.LlmException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.time.Duration;

@Component
@ConditionalOnProperty(
        name = "fresherradar.llm.provider",
        havingValue = "groq"
)
public class GroqLlmClient implements LlmClient {

    private static final int MAX_ATTEMPTS = 3;
    private static final long RETRY_DELAY_MS = 2000;

    private final HttpClient http;
    private final ObjectMapper mapper;
    private final String apiKey;
    private final String baseUrl;
    private final String model;
    private final int maxOutputTokens;

    public GroqLlmClient(
            HttpClient http,
            ObjectMapper mapper,
            @Value("${fresherradar.llm.groq-api-key:}") String apiKey,
            @Value("${fresherradar.llm.groq-base-url:https://api.groq.com/openai/v1}") String baseUrl,
            @Value("${fresherradar.llm.groq-model:openai/gpt-oss-120b}") String model,
            @Value("${fresherradar.llm.max-output-tokens:700}") int maxOutputTokens) {

        this.http = http;
        this.mapper = mapper;
        this.apiKey = apiKey;

        this.baseUrl = baseUrl.endsWith("/")
                ? baseUrl.substring(0, baseUrl.length() - 1)
                : baseUrl;

        this.model = model;
        this.maxOutputTokens = maxOutputTokens;

        System.out.println("GROQ MODEL IN USE: " + this.model);
    }

    @Override
    public boolean isConfigured() {
        return apiKey != null && !apiKey.isBlank();
    }

    @Override
    public String modelName() {
        return model;
    }

    @Override
    public LlmResponse complete(String systemPrompt, String userPrompt) {

        if (!isConfigured()) {
            throw new LlmException(
                    "Groq API key is not configured. Set GROQ_API_KEY."
            );
        }

        ObjectNode body = mapper.createObjectNode();

        body.put("model", model);
        body.put("temperature", 0.1);
        body.put("max_completion_tokens", maxOutputTokens);

        body.putArray("messages")
                .addObject()
                .put("role", "system")
                .put("content", systemPrompt);

        body.withArray("messages")
                .addObject()
                .put("role", "user")
                .put("content", userPrompt);

        body.put("reasoning_format", "hidden");
        body.putObject("response_format")
                .put("type", "json_object");

        HttpRequest request;

        try {
            request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/chat/completions"))
                    .timeout(Duration.ofSeconds(90))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + apiKey)
                    .POST(HttpRequest.BodyPublishers.ofString(
                            mapper.writeValueAsString(body)
                    ))
                    .build();

        } catch (Exception e) {
            throw new LlmException(
                    "Could not build Groq request: " + e.getMessage(), e
            );
        }

        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {

            long start = System.nanoTime();

            try {
                HttpResponse<String> response = http.send(
                        request,
                        HttpResponse.BodyHandlers.ofString()
                );

                long latencyMs = (System.nanoTime() - start) / 1_000_000;
                int statusCode = response.statusCode();

                if (statusCode != 200) {

                    String errorMessage = response.body();

                    try {
                        JsonNode errorRoot = mapper.readTree(response.body());

                        if (errorRoot.has("error")) {
                            JsonNode error = errorRoot.path("error");

                            errorMessage = error.path("message")
                                    .asText(response.body());
                        }

                    } catch (Exception ignored) {
                        // Keep original response body.
                    }

                    if (isRetryable(statusCode) && attempt < MAX_ATTEMPTS) {
                        waitBeforeRetry(attempt);
                        continue;
                    }

                    throw new LlmException(
                            "Groq API responded with HTTP "
                                    + statusCode + ": " + errorMessage
                    );
                }

                JsonNode root = mapper.readTree(response.body());

                String generatedText = root.path("choices")
                        .path(0)
                        .path("message")
                        .path("content")
                        .asText("");

                if (generatedText.isBlank()) {
                    throw new LlmException(
                            "Groq returned no text. Response details: "
                                    + root.toPrettyString()
                    );
                }

                JsonNode usage = root.path("usage");

                return new LlmResponse(
                        generatedText,
                        usage.path("prompt_tokens").asInt(0),
                        usage.path("completion_tokens").asInt(0),
                        latencyMs
                );

            } catch (HttpTimeoutException e) {

                if (attempt < MAX_ATTEMPTS) {
                    waitBeforeRetry(attempt);
                    continue;
                }

                throw new LlmException(
                        "Groq request timed out after "
                                + MAX_ATTEMPTS + " attempts", e
                );

            } catch (InterruptedException e) {

                Thread.currentThread().interrupt();

                throw new LlmException(
                        "Groq request interrupted", e
                );

            } catch (LlmException e) {
                throw e;

            } catch (Exception e) {

                throw new LlmException(
                        "Groq API call failed: "
                                + e.getClass().getSimpleName()
                                + ": " + e.getMessage(),
                        e
                );
            }
        }

        throw new LlmException(
                "Groq request failed after all retry attempts."
        );
    }

    private boolean isRetryable(int statusCode) {
        return statusCode == 429
                || statusCode == 500
                || statusCode == 502
                || statusCode == 503
                || statusCode == 504;
    }

    private void waitBeforeRetry(int attempt) {
        try {
            Thread.sleep(RETRY_DELAY_MS * attempt);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new LlmException(
                    "Groq retry interrupted", e
            );
        }
    }
}

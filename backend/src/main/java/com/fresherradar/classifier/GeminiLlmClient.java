
package com.fresherradar.classifier;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fresherradar.exception.LlmException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.time.Duration;

@Component
public class GeminiLlmClient implements LlmClient {

    private static final int MAX_ATTEMPTS = 2;
    private static final long RETRY_DELAY_MS = 2000;

    private final HttpClient http;
    private final ObjectMapper mapper;
    private final String apiKey;
    private final String baseUrl;
    private final String model;
    private final int maxOutputTokens;

    public GeminiLlmClient(
            HttpClient http,
            ObjectMapper mapper,
            @Value("${fresherradar.llm.api-key:}") String apiKey,
            @Value("${fresherradar.llm.gemini-base-url:https://generativelanguage.googleapis.com/v1beta}") String baseUrl,
            @Value("${fresherradar.llm.gemini-model:gemini-2.5-flash}") String model,
            @Value("${fresherradar.llm.max-output-tokens:700}") int maxOutputTokens) {

        this.http = http;
        this.mapper = mapper;
        this.apiKey = apiKey;

        this.baseUrl = baseUrl.endsWith("/")
                ? baseUrl.substring(0, baseUrl.length() - 1)
                : baseUrl;

        this.model = model;
        System.out.println("GEMINI MODEL IN USE: " + this.model);
        this.maxOutputTokens = maxOutputTokens;
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
            throw new LlmException("Gemini API key is not configured. Set LLM_API_KEY.");
        }

        ObjectNode body = mapper.createObjectNode();

        body.putObject("systemInstruction")
                .putArray("parts")
                .addObject()
                .put("text", systemPrompt);

        body.putArray("contents")
                .addObject()
                .put("role", "user")
                .putArray("parts")
                .addObject()
                .put("text", userPrompt);

        ObjectNode generationConfig = body.putObject("generationConfig");
        generationConfig.put("maxOutputTokens", maxOutputTokens);
        generationConfig.put("responseMimeType", "application/json");

        generationConfig.putObject("thinkingConfig")
                .put("thinkingLevel", "LOW");

        String endpoint = baseUrl + "/models/" + model + ":generateContent";

        HttpRequest request;

        try {
            request = HttpRequest.newBuilder()
                    .uri(URI.create(endpoint))
                    .timeout(Duration.ofSeconds(60))
                    .header("Content-Type", "application/json")
                    .header("x-goog-api-key", apiKey)
                    .POST(HttpRequest.BodyPublishers.ofString(
                            mapper.writeValueAsString(body)))
                    .build();
        } catch (Exception e) {
            throw new LlmException("Could not build Gemini request: " + e.getMessage(), e);
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
                            errorMessage = errorRoot.path("error")
                                    .path("message")
                                    .asText(response.body());
                        }
                    } catch (Exception ignored) {
                        // Keep original response body.
                    }

                    if (isRetryable(statusCode) && attempt < MAX_ATTEMPTS) {
                        waitBeforeRetry();
                        continue;
                    }

                    throw new LlmException(
                            "Gemini API responded with HTTP "
                                    + statusCode + ": " + errorMessage
                    );
                }

                JsonNode root = mapper.readTree(response.body());
                StringBuilder generatedText = new StringBuilder();

                for (JsonNode candidate : root.path("candidates")) {
                    for (JsonNode part : candidate.path("content").path("parts")) {
                        if (part.has("text")) {
                            generatedText.append(part.path("text").asText());
                        }
                    }
                }

                if (generatedText.toString().isBlank()) {
                    throw new LlmException(
                            "Gemini returned no text. Response details: "
                                    + root.toPrettyString()
                    );
                }

                JsonNode usage = root.path("usageMetadata");

                return new LlmResponse(
                        generatedText.toString(),
                        usage.path("promptTokenCount").asInt(0),
                        usage.path("candidatesTokenCount").asInt(0),
                        latencyMs
                );

            } catch (HttpTimeoutException e) {
                if (attempt < MAX_ATTEMPTS) {
                    waitBeforeRetry();
                    continue;
                }

                throw new LlmException(
                        "Gemini request timed out after "
                                + MAX_ATTEMPTS + " attempts", e
                );

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new LlmException("LLM call interrupted", e);

            } catch (LlmException e) {
                throw e;

            } catch (Exception e) {
                throw new LlmException(
                        "Gemini API call failed: "
                                + e.getClass().getSimpleName()
                                + ": " + e.getMessage(),
                        e
                );
            }
        }

        throw new LlmException("Gemini request failed after all retry attempts.");
    }

    private boolean isRetryable(int statusCode) {
        return statusCode == 429
                || statusCode == 500
                || statusCode == 502
                || statusCode == 503
                || statusCode == 504;
    }

    private void waitBeforeRetry() {
        try {
            Thread.sleep(RETRY_DELAY_MS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new LlmException("Gemini retry interrupted", e);
        }
    }
}


package com.fresherradar.classifier;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.net.http.HttpClient;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class GeminiLlmClientTest {

    @Test
    @EnabledIfEnvironmentVariable(
            named = "RUN_GEMINI_INTEGRATION_TEST",
            matches = "true"
    )
    void shouldCallGeminiSuccessfully() {

        String apiKey = System.getenv("LLM_API_KEY");

        assertNotNull(apiKey, "LLM_API_KEY environment variable is missing");
        assertFalse(apiKey.isBlank(), "LLM_API_KEY is blank");

        String baseUrl = System.getenv().getOrDefault(
                "GEMINI_BASE_URL",
                "https://generativelanguage.googleapis.com/v1beta"
        );

     String model = System.getenv().getOrDefault(
        "GEMINI_MODEL",
        "gemini-2.5-flash"
);

        GeminiLlmClient client = new GeminiLlmClient(
                HttpClient.newHttpClient(),
                new ObjectMapper(),
                apiKey,
                baseUrl,
                model,
                700
        );

        LlmClient.LlmResponse response = client.complete(
                "Return only valid JSON.",
                """
                Classify this synthetic job posting.
                Return JSON with fields fresher_eligible and reason.

                Title: Junior Java Developer
                Experience: 0 years
                Company: Example Test Company
                """
        );

        assertNotNull(response);
        assertFalse(response.text().isBlank(), "Gemini returned an empty response");

        System.out.println("Model: " + client.modelName());
        System.out.println("Response: " + response.text());
        System.out.println("Input tokens: " + response.inputTokens());
        System.out.println("Output tokens: " + response.outputTokens());
        System.out.println("Latency: " + response.latencyMs() + " ms");
    }
}

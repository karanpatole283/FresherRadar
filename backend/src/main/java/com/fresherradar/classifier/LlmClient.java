package com.fresherradar.classifier;

/** Provider-independent LLM contract. Add another implementation to switch provider. */
public interface LlmClient {

    boolean isConfigured();

    String modelName();

    LlmResponse complete(String systemPrompt, String userPrompt);

    record LlmResponse(String text, int inputTokens, int outputTokens, long latencyMs) {}
}

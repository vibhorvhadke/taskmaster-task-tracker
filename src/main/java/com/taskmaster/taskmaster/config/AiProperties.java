package com.taskmaster.taskmaster.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Generative AI settings (OpenRouter), loaded from application.properties (prefix "app.ai").
 *
 * app.ai.api-key         -> apiKey         (from the OPENROUTER_API_KEY environment variable; blank = AI disabled)
 * app.ai.base-url        -> baseUrl
 * app.ai.model           -> model          (e.g. "openrouter/free" or "meta-llama/llama-3.3-70b-instruct:free")
 * app.ai.timeout-seconds -> timeoutSeconds
 * app.ai.app-name        -> appName        (sent as X-OpenRouter-Title for attribution)
 */
@ConfigurationProperties(prefix = "app.ai")
public record AiProperties(
        String apiKey,
        @DefaultValue("https://openrouter.ai/api/v1") String baseUrl,
        @DefaultValue("openrouter/free") String model,
        @DefaultValue("60") int timeoutSeconds,
        @DefaultValue("TaskMaster") String appName
) {

    /** Never print the API key in logs. */
    @Override
    public String toString() {
        return "AiProperties[baseUrl=" + baseUrl + ", model=" + model
                + ", timeoutSeconds=" + timeoutSeconds + ", apiKey=" + (apiKey == null || apiKey.isBlank() ? "<not set>" : "****") + "]";
    }
}
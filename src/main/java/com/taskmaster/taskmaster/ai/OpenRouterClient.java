package com.taskmaster.taskmaster.ai;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.taskmaster.taskmaster.config.AiProperties;
import com.taskmaster.taskmaster.exception.AiServiceException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.List;

/**
 * Thin client for the OpenRouter Chat Completions API (OpenAI-compatible).
 *
 * POST {base-url}/chat/completions
 *   Authorization: Bearer <OPENROUTER_API_KEY>
 *   { "model": "...", "messages": [ {role, content}, ... ], "max_tokens": ..., "temperature": ... }
 * Response text: choices[0].message.content
 *
 * All OpenRouter errors are converted to AiServiceException with a safe message;
 * the raw error body is only logged, never returned to the client.
 */
@Slf4j
@Component
public class OpenRouterClient {

    private final RestClient restClient;
    private final AiProperties properties;

    public OpenRouterClient(AiProperties properties) {
        this.properties = properties;

        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(Duration.ofSeconds(properties.timeoutSeconds()));

        // Built directly: Spring Boot 4 provides a RestClient.Builder bean only with
        // the separate spring-boot-starter-restclient module, which this project doesn't need.
        this.restClient = RestClient.builder()
                .baseUrl(properties.baseUrl())
                .requestFactory(requestFactory)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + properties.apiKey())
                .defaultHeader("X-OpenRouter-Title", properties.appName())
                .build();

        log.info("AI (OpenRouter) {} - model: {}",
                isConfigured() ? "enabled" : "disabled (OPENROUTER_API_KEY not set)", properties.model());
    }

    public boolean isConfigured() {
        return StringUtils.hasText(properties.apiKey());
    }

    /**
     * Sends one system + one user message and returns the model's reply.
     */
    public AiCompletion complete(String systemPrompt, String userPrompt, int maxTokens, double temperature) {
        if (!isConfigured()) {
            throw new AiServiceException(HttpStatus.SERVICE_UNAVAILABLE,
                    "AI feature is not configured. Set the OPENROUTER_API_KEY environment variable");
        }

        ChatRequest request = new ChatRequest(
                properties.model(),
                List.of(new ChatMessage("system", systemPrompt), new ChatMessage("user", userPrompt)),
                maxTokens,
                temperature
        );

        ChatResponse response;
        try {
            response = restClient.post()
                    .uri("/chat/completions")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(ChatResponse.class);
        } catch (RestClientResponseException ex) {
            log.warn("OpenRouter returned HTTP {}: {}", ex.getStatusCode().value(), ex.getResponseBodyAsString());
            throw mapError(ex.getStatusCode().value());
        } catch (ResourceAccessException ex) {
            log.warn("OpenRouter not reachable: {}", ex.getMessage());
            throw new AiServiceException(HttpStatus.GATEWAY_TIMEOUT,
                    "The AI service did not respond in time. Please try again");
        }

        String content = extractContent(response);
        if (!StringUtils.hasText(content)) {
            throw new AiServiceException(HttpStatus.BAD_GATEWAY,
                    "The AI model returned an empty response. Please try again");
        }
        return new AiCompletion(content.trim(), response.model());
    }

    private AiServiceException mapError(int statusCode) {
        return switch (statusCode) {
            case 401, 403 -> new AiServiceException(HttpStatus.SERVICE_UNAVAILABLE,
                    "The AI service rejected the API key. Check OPENROUTER_API_KEY");
            case 402 -> new AiServiceException(HttpStatus.SERVICE_UNAVAILABLE,
                    "The AI service account has insufficient credits");
            case 429 -> new AiServiceException(HttpStatus.TOO_MANY_REQUESTS,
                    "AI rate limit reached. Please try again in a moment");
            default -> new AiServiceException(HttpStatus.BAD_GATEWAY,
                    "The AI service returned an error. Please try again");
        };
    }

    private String extractContent(ChatResponse response) {
        if (response == null || response.choices() == null || response.choices().isEmpty()) {
            return null;
        }
        ChatMessage message = response.choices().getFirst().message();
        return message != null ? message.content() : null;
    }

    // ---------------------------------------------------------------------
    // Request / response shapes (only the fields we use)
    // ---------------------------------------------------------------------

    /** The reply text and the model that actually answered (useful with "openrouter/free"). */
    public record AiCompletion(String text, String model) {
    }

    record ChatRequest(
            String model,
            List<ChatMessage> messages,
            @JsonProperty("max_tokens") int maxTokens,
            double temperature
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record ChatMessage(String role, String content) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record ChatResponse(String model, List<Choice> choices) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Choice(ChatMessage message) {
    }
}
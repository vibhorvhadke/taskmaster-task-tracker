package com.taskmaster.taskmaster.dto.ai;

/**
 * AI-generated text plus the model that produced it.
 * Example: { "text": "Configure GitHub Actions to ...", "model": "meta-llama/llama-3.3-70b-instruct:free" }
 */
public record AiTextResponse(
        String text,
        String model
) {
}
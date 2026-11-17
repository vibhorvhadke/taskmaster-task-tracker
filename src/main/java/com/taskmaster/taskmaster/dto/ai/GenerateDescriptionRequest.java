package com.taskmaster.taskmaster.dto.ai;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request body for POST /api/ai/task-description.
 * Example: { "title": "Set up CI pipeline", "notes": "GitHub Actions, run tests on every PR" }
 */
public record GenerateDescriptionRequest(

        @NotBlank(message = "Title is required")
        @Size(max = 200, message = "Title must be at most 200 characters")
        String title,

        @Size(max = 1000, message = "Notes must be at most 1000 characters")
        String notes
) {
}
package com.taskmaster.taskmaster.dto.comment;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request body for POST /api/tasks/{taskId}/comments.
 */
public record CreateCommentRequest(

        @NotBlank(message = "Comment content is required")
        @Size(max = 2000, message = "Comment must be at most 2000 characters")
        String content
) {
}
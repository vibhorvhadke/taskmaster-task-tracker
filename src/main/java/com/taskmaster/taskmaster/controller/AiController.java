package com.taskmaster.taskmaster.controller;

import com.taskmaster.taskmaster.dto.ai.AiTextResponse;
import com.taskmaster.taskmaster.dto.ai.GenerateDescriptionRequest;
import com.taskmaster.taskmaster.security.AuthenticatedUser;
import com.taskmaster.taskmaster.service.AiService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Generative AI endpoints (powered by OpenRouter). All require a valid token.
 *
 * POST /api/ai/task-description       -> 200 OK + generated description (nothing is saved;
 *                                        the client puts the text into POST/PATCH /api/tasks)
 * POST /api/ai/tasks/{taskId}/summary -> 200 OK + summary of the task and its comments
 */
@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AiController {

    private final AiService aiService;

    @PostMapping("/task-description")
    public ResponseEntity<AiTextResponse> generateDescription(@Valid @RequestBody GenerateDescriptionRequest request) {
        return ResponseEntity.ok(aiService.generateDescription(request));
    }

    @PostMapping("/tasks/{taskId}/summary")
    public ResponseEntity<AiTextResponse> summarizeTask(@AuthenticationPrincipal AuthenticatedUser currentUser,
                                                        @PathVariable Long taskId) {
        return ResponseEntity.ok(aiService.summarizeTask(taskId, currentUser.id()));
    }
}
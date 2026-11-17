package com.taskmaster.taskmaster.controller;

import com.taskmaster.taskmaster.dto.comment.CommentResponse;
import com.taskmaster.taskmaster.dto.comment.CreateCommentRequest;
import com.taskmaster.taskmaster.security.AuthenticatedUser;
import com.taskmaster.taskmaster.service.CommentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Comment endpoints. All require a valid token.
 *
 * POST   /api/tasks/{taskId}/comments              -> 201 Created + comment
 * GET    /api/tasks/{taskId}/comments              -> 200 OK + comments (oldest first)
 * DELETE /api/tasks/{taskId}/comments/{commentId}  -> 204 No Content (author or team OWNER)
 */
@RestController
@RequestMapping("/api/tasks/{taskId}/comments")
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;

    @PostMapping
    public ResponseEntity<CommentResponse> addComment(@AuthenticationPrincipal AuthenticatedUser currentUser,
                                                      @PathVariable Long taskId,
                                                      @Valid @RequestBody CreateCommentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(commentService.addComment(taskId, currentUser.id(), request));
    }

    @GetMapping
    public ResponseEntity<List<CommentResponse>> getComments(@AuthenticationPrincipal AuthenticatedUser currentUser,
                                                             @PathVariable Long taskId) {
        return ResponseEntity.ok(commentService.getComments(taskId, currentUser.id()));
    }

    @DeleteMapping("/{commentId}")
    public ResponseEntity<Void> deleteComment(@AuthenticationPrincipal AuthenticatedUser currentUser,
                                              @PathVariable Long taskId,
                                              @PathVariable Long commentId) {
        commentService.deleteComment(taskId, commentId, currentUser.id());
        return ResponseEntity.noContent().build();
    }
}
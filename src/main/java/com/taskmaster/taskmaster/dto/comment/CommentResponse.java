package com.taskmaster.taskmaster.dto.comment;

import com.taskmaster.taskmaster.dto.user.UserSummary;
import com.taskmaster.taskmaster.entity.Comment;

import java.time.Instant;

/**
 * A comment on a task.
 */
public record CommentResponse(
        Long id,
        Long taskId,
        UserSummary author,
        String content,
        Instant createdAt
) {

    public static CommentResponse from(Comment comment) {
        return new CommentResponse(
                comment.getId(),
                comment.getTask().getId(),
                UserSummary.from(comment.getAuthor()),
                comment.getContent(),
                comment.getCreatedAt()
        );
    }
}
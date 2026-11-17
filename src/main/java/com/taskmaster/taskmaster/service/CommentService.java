package com.taskmaster.taskmaster.service;

import com.taskmaster.taskmaster.dto.comment.CommentResponse;
import com.taskmaster.taskmaster.dto.comment.CreateCommentRequest;
import com.taskmaster.taskmaster.entity.Comment;
import com.taskmaster.taskmaster.entity.Task;
import com.taskmaster.taskmaster.entity.User;
import com.taskmaster.taskmaster.enums.TeamRole;
import com.taskmaster.taskmaster.exception.ForbiddenException;
import com.taskmaster.taskmaster.exception.ResourceNotFoundException;
import com.taskmaster.taskmaster.notification.NotificationPublisher;
import com.taskmaster.taskmaster.repository.CommentRepository;
import com.taskmaster.taskmaster.repository.TeamMemberRepository;
import com.taskmaster.taskmaster.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Comments on tasks.
 *
 * Permissions:
 *  - add / view : anyone who can see the task (TaskService.requireTaskAccess)
 *  - delete     : the comment author, or the team OWNER
 */
@Service
@RequiredArgsConstructor
public class CommentService {

    private final CommentRepository commentRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final UserRepository userRepository;
    private final TaskService taskService;
    private final NotificationPublisher notificationPublisher;

    @Transactional
    public CommentResponse addComment(Long taskId, Long userId, CreateCommentRequest request) {
        Task task = taskService.requireTaskAccess(taskId, userId);
        User author = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        Comment comment = commentRepository.saveAndFlush(Comment.builder()
                .task(task)
                .author(author)
                .content(request.content().trim())
                .build());

        notificationPublisher.commentAdded(task, userId);
        return CommentResponse.from(comment);
    }

    @Transactional(readOnly = true)
    public List<CommentResponse> getComments(Long taskId, Long userId) {
        taskService.requireTaskAccess(taskId, userId);
        return commentRepository.findByTaskIdOrderByCreatedAtAsc(taskId).stream()
                .map(CommentResponse::from)
                .toList();
    }

    @Transactional
    public void deleteComment(Long taskId, Long commentId, Long userId) {
        Task task = taskService.requireTaskAccess(taskId, userId);

        Comment comment = commentRepository.findById(commentId)
                // A comment ID from a different task is treated as "not found" for this task
                .filter(c -> c.getTask().getId().equals(taskId))
                .orElseThrow(() -> new ResourceNotFoundException("Comment", commentId));

        boolean isAuthor = comment.getAuthor().getId().equals(userId);
        if (!isAuthor && !isTeamOwner(task, userId)) {
            throw new ForbiddenException("Only the comment author or the team owner can delete this comment");
        }

        commentRepository.delete(comment);
    }

    private boolean isTeamOwner(Task task, Long userId) {
        if (task.getTeam() == null) {
            return false;
        }
        return teamMemberRepository.findByTeamIdAndUserId(task.getTeam().getId(), userId)
                .map(membership -> membership.getRole() == TeamRole.OWNER)
                .orElse(false);
    }
}
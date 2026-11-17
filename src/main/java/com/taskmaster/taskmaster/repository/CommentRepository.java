package com.taskmaster.taskmaster.repository;

import com.taskmaster.taskmaster.entity.Comment;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Data access for task comments.
 */
public interface CommentRepository extends JpaRepository<Comment, Long> {

    /** All comments on a task, oldest first, with each author loaded in the same query. */
    @EntityGraph(attributePaths = "author")
    List<Comment> findByTaskIdOrderByCreatedAtAsc(Long taskId);
}

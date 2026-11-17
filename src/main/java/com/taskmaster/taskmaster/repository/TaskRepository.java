package com.taskmaster.taskmaster.repository;

import com.taskmaster.taskmaster.entity.Task;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.util.Optional;

/**
 * Data access for tasks.
 * <p>
 * JpaSpecificationExecutor lets us build dynamic queries at runtime
 * (filter by status, team, assignee; search by title/description)
 * without writing a separate method for every combination.
 */
public interface TaskRepository extends JpaRepository<Task, Long>, JpaSpecificationExecutor<Task> {
    @EntityGraph(attributePaths = {"team", "createdBy", "assignee"})
    Optional<Task> findWithDetailsById(Long id);

    @Override
    @EntityGraph(attributePaths = {"team", "createdBy", "assignee"})
    Page<Task> findAll(Specification<Task> spec, Pageable pageable);
}

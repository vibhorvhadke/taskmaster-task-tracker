package com.taskmaster.taskmaster.controller;

import com.taskmaster.taskmaster.dto.common.PageResponse;
import com.taskmaster.taskmaster.dto.task.AssignTaskRequest;
import com.taskmaster.taskmaster.dto.task.CreateTaskRequest;
import com.taskmaster.taskmaster.dto.task.TaskFilter;
import com.taskmaster.taskmaster.dto.task.TaskResponse;
import com.taskmaster.taskmaster.dto.task.UpdateTaskRequest;
import com.taskmaster.taskmaster.dto.task.UpdateTaskStatusRequest;
import com.taskmaster.taskmaster.enums.TaskPriority;
import com.taskmaster.taskmaster.enums.TaskScope;
import com.taskmaster.taskmaster.enums.TaskStatus;
import com.taskmaster.taskmaster.security.AuthenticatedUser;
import com.taskmaster.taskmaster.service.TaskService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

/**
 * Task endpoints. All require a valid token.
 *
 * POST /api/tasks           -> 201 Created + task (Location header points to the new task)
 * GET  /api/tasks           -> 200 OK + paginated list (filter / sort / search)
 * GET    /api/tasks/{taskId}           -> 200 OK + task
 * PATCH  /api/tasks/{taskId}           -> 200 OK + task   (creator or team OWNER)
 * PATCH  /api/tasks/{taskId}/status    -> 200 OK + task   (assignee, creator or team OWNER)
 * PATCH  /api/tasks/{taskId}/assignee  -> 200 OK + task   (assignee, creator or team OWNER)
 * DELETE /api/tasks/{taskId}           -> 204 No Content  (creator or team OWNER)
 */
@RestController
@RequestMapping("/api/tasks")
@RequiredArgsConstructor
public class TaskController {

    private final TaskService taskService;

    @PostMapping
    public ResponseEntity<TaskResponse> createTask(@AuthenticationPrincipal AuthenticatedUser currentUser,
                                                   @Valid @RequestBody CreateTaskRequest request) {
        TaskResponse task = taskService.createTask(currentUser.id(), request);

        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(task.id())
                .toUri();

        return ResponseEntity.created(location).body(task);
    }

    /**
     * Example: GET /api/tasks?scope=ASSIGNED_TO_ME&status=OPEN&search=home&sortBy=dueDate&direction=asc&page=0&size=20
     */
    @GetMapping
    public ResponseEntity<PageResponse<TaskResponse>> listTasks(
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @RequestParam(defaultValue = "ALL") TaskScope scope,
            @RequestParam(required = false) Long teamId,
            @RequestParam(required = false) TaskStatus status,
            @RequestParam(required = false) TaskPriority priority,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "dueDate") String sortBy,
            @RequestParam(defaultValue = "asc") String direction,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        TaskFilter filter = new TaskFilter(scope, teamId, status, priority, search);
        return ResponseEntity.ok(taskService.listTasks(currentUser.id(), filter, page, size, sortBy, direction));
    }

    @GetMapping("/{taskId}")
    public ResponseEntity<TaskResponse> getTask(@AuthenticationPrincipal AuthenticatedUser currentUser,
                                                @PathVariable Long taskId) {
        return ResponseEntity.ok(taskService.getTask(taskId, currentUser.id()));
    }

    @PatchMapping("/{taskId}")
    public ResponseEntity<TaskResponse> updateTask(@AuthenticationPrincipal AuthenticatedUser currentUser,
                                                   @PathVariable Long taskId,
                                                   @Valid @RequestBody UpdateTaskRequest request) {
        return ResponseEntity.ok(taskService.updateTask(taskId, currentUser.id(), request));
    }

    @PatchMapping("/{taskId}/status")
    public ResponseEntity<TaskResponse> updateStatus(@AuthenticationPrincipal AuthenticatedUser currentUser,
                                                     @PathVariable Long taskId,
                                                     @Valid @RequestBody UpdateTaskStatusRequest request) {
        return ResponseEntity.ok(taskService.updateStatus(taskId, currentUser.id(), request));
    }

    @PatchMapping("/{taskId}/assignee")
    public ResponseEntity<TaskResponse> assignTask(@AuthenticationPrincipal AuthenticatedUser currentUser,
                                                   @PathVariable Long taskId,
                                                   @RequestBody AssignTaskRequest request) {
        return ResponseEntity.ok(taskService.assignTask(taskId, currentUser.id(), request));
    }

    @DeleteMapping("/{taskId}")
    public ResponseEntity<Void> deleteTask(@AuthenticationPrincipal AuthenticatedUser currentUser,
                                           @PathVariable Long taskId) {
        taskService.deleteTask(taskId, currentUser.id());
        return ResponseEntity.noContent().build();
    }
}
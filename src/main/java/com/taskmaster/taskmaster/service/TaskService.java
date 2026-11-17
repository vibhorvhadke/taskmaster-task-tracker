package com.taskmaster.taskmaster.service;

import com.taskmaster.taskmaster.dto.common.PageResponse;
import com.taskmaster.taskmaster.dto.task.AssignTaskRequest;
import com.taskmaster.taskmaster.dto.task.CreateTaskRequest;
import com.taskmaster.taskmaster.dto.task.TaskFilter;
import com.taskmaster.taskmaster.dto.task.TaskResponse;
import com.taskmaster.taskmaster.dto.task.UpdateTaskRequest;
import com.taskmaster.taskmaster.dto.task.UpdateTaskStatusRequest;
import com.taskmaster.taskmaster.entity.Task;
import com.taskmaster.taskmaster.entity.Team;
import com.taskmaster.taskmaster.entity.TeamMember;
import com.taskmaster.taskmaster.entity.User;
import com.taskmaster.taskmaster.enums.TaskPriority;
import com.taskmaster.taskmaster.enums.TaskStatus;
import com.taskmaster.taskmaster.enums.TeamRole;
import com.taskmaster.taskmaster.exception.BadRequestException;
import com.taskmaster.taskmaster.exception.ForbiddenException;
import com.taskmaster.taskmaster.exception.ResourceNotFoundException;
import com.taskmaster.taskmaster.entity.Attachment;
import com.taskmaster.taskmaster.notification.NotificationPublisher;
import com.taskmaster.taskmaster.repository.AttachmentRepository;
import com.taskmaster.taskmaster.repository.TaskRepository;
import com.taskmaster.taskmaster.repository.TaskSpecifications;
import com.taskmaster.taskmaster.repository.TeamMemberRepository;
import com.taskmaster.taskmaster.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;
import java.util.Set;

/**
 * Task creation, viewing, listing (filter / sort / search / pagination),
 * updating, status changes, assignment, deletion, and task access checks.
 *
 * Permissions (on top of being able to see the task):
 *  - update details, delete : creator or team OWNER
 *  - change status, assign  : creator, team OWNER, or current assignee
 */
@Service
@RequiredArgsConstructor
public class TaskService {

    private static final Set<String> SORTABLE_FIELDS = Set.of("dueDate", "createdAt", "updatedAt", "title");
    private static final int MAX_PAGE_SIZE = 100;

    private final TaskRepository taskRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final UserRepository userRepository;
    private final TeamService teamService;
    private final NotificationPublisher notificationPublisher;
    private final AttachmentRepository attachmentRepository;
    private final FileStorageService fileStorageService;

    /**
     * Creates a personal task (no teamId) or a team task (teamId given).
     */
    @Transactional
    public TaskResponse createTask(Long userId, CreateTaskRequest request) {
        User creator = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        Team team = null;
        User assignee;

        if (request.teamId() == null) {
            // Personal task: always assigned to the creator
            if (request.assigneeId() != null && !request.assigneeId().equals(userId)) {
                throw new BadRequestException("Personal tasks can only be assigned to yourself");
            }
            assignee = creator;
        } else {
            // Team task: creator must be a member; assignee (if any) must be a member too
            TeamMember membership = teamService.requireMembership(request.teamId(), userId);
            team = membership.getTeam();
            assignee = resolveTeamAssignee(request.teamId(), request.assigneeId());
        }

        Task task = Task.builder()
                .title(request.title().trim())
                .description(trimToNull(request.description()))
                .dueDate(request.dueDate())
                .status(TaskStatus.OPEN)
                .priority(request.priority() != null ? request.priority() : TaskPriority.MEDIUM)
                .team(team)
                .createdBy(creator)
                .assignee(assignee)
                .build();

        Task saved = taskRepository.saveAndFlush(task);
        notificationPublisher.taskAssigned(saved, userId);
        return TaskResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public TaskResponse getTask(Long taskId, Long userId) {
        return TaskResponse.from(requireTaskAccess(taskId, userId));
    }

    /**
     * Lists the tasks the user can see, filtered, searched, sorted and paginated.
     *
     * @throws BadRequestException if page, size, sortBy or direction is invalid
     */
    @Transactional(readOnly = true)
    public PageResponse<TaskResponse> listTasks(Long userId, TaskFilter filter,
                                                int page, int size, String sortBy, String direction) {
        if (page < 0) {
            throw new BadRequestException("page must be 0 or greater");
        }
        if (size < 1 || size > MAX_PAGE_SIZE) {
            throw new BadRequestException("size must be between 1 and " + MAX_PAGE_SIZE);
        }
        if (!SORTABLE_FIELDS.contains(sortBy)) {
            throw new BadRequestException("sortBy must be one of: " + String.join(", ", SORTABLE_FIELDS));
        }
        Sort.Direction sortDirection = Sort.Direction.fromOptionalString(direction)
                .orElseThrow(() -> new BadRequestException("direction must be 'asc' or 'desc'"));

        // Filtering by a team: give a clear 404/403 instead of just an empty list
        if (filter.teamId() != null) {
            teamService.requireMembership(filter.teamId(), userId);
        }

        // Tasks without a due date go last; id as a tie-breaker keeps page order stable
        Sort sort = Sort.by(
                new Sort.Order(sortDirection, sortBy, Sort.NullHandling.NULLS_LAST),
                Sort.Order.asc("id")
        );

        Page<Task> tasks = taskRepository.findAll(
                TaskSpecifications.forUser(userId, filter),
                PageRequest.of(page, size, sort)
        );

        return PageResponse.from(tasks.map(TaskResponse::from));
    }

    /**
     * Partially updates title, description, due date and priority. Null fields are left unchanged.
     */
    @Transactional
    public TaskResponse updateTask(Long taskId, Long userId, UpdateTaskRequest request) {
        Task task = requireTaskAccess(taskId, userId);
        requireCanManage(task, userId);

        if (request.title() == null && request.description() == null
                && request.dueDate() == null && request.priority() == null) {
            throw new BadRequestException("Provide at least one field to update");
        }

        if (request.title() != null) {
            if (request.title().isBlank()) {
                throw new BadRequestException("Title cannot be blank");
            }
            task.setTitle(request.title().trim());
        }
        if (request.description() != null) {
            task.setDescription(trimToNull(request.description()));
        }
        if (request.dueDate() != null) {
            task.setDueDate(request.dueDate());
        }
        if (request.priority() != null) {
            task.setPriority(request.priority());
        }

        Task saved = taskRepository.saveAndFlush(task);
        notificationPublisher.taskUpdated(saved, userId);
        return TaskResponse.from(saved);
    }

    /**
     * Changes the status, e.g. marks the task as COMPLETED.
     */
    @Transactional
    public TaskResponse updateStatus(Long taskId, Long userId, UpdateTaskStatusRequest request) {
        Task task = requireTaskAccess(taskId, userId);
        requireCanManageOrIsAssignee(task, userId);

        task.setStatus(request.status());
        Task saved = taskRepository.saveAndFlush(task);
        notificationPublisher.taskStatusChanged(saved, userId);
        return TaskResponse.from(saved);
    }

    /**
     * Assigns the task to a team member, or unassigns it when assigneeId is null.
     */
    @Transactional
    public TaskResponse assignTask(Long taskId, Long userId, AssignTaskRequest request) {
        Task task = requireTaskAccess(taskId, userId);
        requireCanManageOrIsAssignee(task, userId);

        if (task.getTeam() == null) {
            throw new BadRequestException("Personal tasks cannot be assigned to other users");
        }

        Long previousAssigneeId = task.getAssignee() != null ? task.getAssignee().getId() : null;
        task.setAssignee(resolveTeamAssignee(task.getTeam().getId(), request.assigneeId()));
        Task saved = taskRepository.saveAndFlush(task);

        // Notify only when the task actually moved to a (different) person
        if (saved.getAssignee() != null && !saved.getAssignee().getId().equals(previousAssigneeId)) {
            notificationPublisher.taskAssigned(saved, userId);
        }
        return TaskResponse.from(saved);
    }

    /**
     * Deletes the task. Its comments and attachment records are removed by the
     * database (ON DELETE CASCADE in the Flyway script); the attachment files on
     * disk are removed only after the database delete has committed.
     */
    @Transactional
    public void deleteTask(Long taskId, Long userId) {
        Task task = requireTaskAccess(taskId, userId);
        requireCanManage(task, userId);

        // Read only the paths (not Attachment entities), so Hibernate does not track
        // attachments that would still point to the deleted task at flush time.

        List<String> filePaths = attachmentRepository.findStoragePathsByTaskId(taskId);

        taskRepository.delete(task);

        if (!filePaths.isEmpty()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    filePaths.forEach(fileStorageService::delete);
                }
            });
        }
    }

    // ---------------------------------------------------------------------
    // Access checks (reused by update, comments and attachments)
    // ---------------------------------------------------------------------

    /**
     * Loads a task and checks that the user may access it.
     *  - Team task     : user must be a member of the task's team
     *  - Personal task : user must be the creator
     *
     * @throws ResourceNotFoundException if the task does not exist (404)
     * @throws ForbiddenException        if the user may not access it (403)
     */
    @Transactional(readOnly = true)
    public Task requireTaskAccess(Long taskId, Long userId) {
        Task task = taskRepository.findWithDetailsById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Task", taskId));

        if (task.getTeam() != null) {
            teamService.requireMembership(task.getTeam().getId(), userId);
        } else if (!task.getCreatedBy().getId().equals(userId)) {
            throw new ForbiddenException("You do not have access to this task");
        }
        return task;
    }

    /** Creator or team OWNER. */
    private void requireCanManage(Task task, Long userId) {
        if (!isCreator(task, userId) && !isTeamOwner(task, userId)) {
            throw new ForbiddenException("Only the task creator or the team owner can perform this action");
        }
    }

    /** Creator, team OWNER, or current assignee. */
    private void requireCanManageOrIsAssignee(Task task, Long userId) {
        boolean isAssignee = task.getAssignee() != null && task.getAssignee().getId().equals(userId);
        if (!isAssignee && !isCreator(task, userId) && !isTeamOwner(task, userId)) {
            throw new ForbiddenException("Only the assignee, the task creator or the team owner can perform this action");
        }
    }

    private boolean isCreator(Task task, Long userId) {
        return task.getCreatedBy().getId().equals(userId);
    }

    private boolean isTeamOwner(Task task, Long userId) {
        if (task.getTeam() == null) {
            return false;
        }
        return teamMemberRepository.findByTeamIdAndUserId(task.getTeam().getId(), userId)
                .map(membership -> membership.getRole() == TeamRole.OWNER)
                .orElse(false);
    }

    /**
     * Returns the assignee for a team task, or null if none was given.
     *
     * @throws BadRequestException if the assignee is not a member of the team
     */
    User resolveTeamAssignee(Long teamId, Long assigneeId) {
        if (assigneeId == null) {
            return null;
        }
        return teamMemberRepository.findByTeamIdAndUserId(teamId, assigneeId)
                .map(TeamMember::getUser)
                .orElseThrow(() -> new BadRequestException("Assignee must be a member of the team"));
    }

    private String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
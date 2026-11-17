package com.taskmaster.taskmaster.service;

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
import com.taskmaster.taskmaster.enums.TaskScope;
import com.taskmaster.taskmaster.enums.TaskStatus;
import com.taskmaster.taskmaster.enums.TeamRole;
import com.taskmaster.taskmaster.exception.BadRequestException;
import com.taskmaster.taskmaster.exception.ForbiddenException;
import com.taskmaster.taskmaster.exception.ResourceNotFoundException;
import com.taskmaster.taskmaster.notification.NotificationPublisher;
import com.taskmaster.taskmaster.repository.AttachmentRepository;
import com.taskmaster.taskmaster.repository.TaskRepository;
import com.taskmaster.taskmaster.repository.TeamMemberRepository;
import com.taskmaster.taskmaster.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Unit tests for the task permission and validation rules in TaskService.
 * Repositories and collaborating services are mocked; no database or Spring context is needed.
 *
 * Test data: team "Website Launch" (id 10) with Priya (OWNER, id 1) and Amit (MEMBER, id 2).
 * Rahul (id 3) is not a member.
 */
@ExtendWith(MockitoExtension.class)
class TaskServiceTest {

    private static final Long TEAM_ID = 10L;
    private static final Long TASK_ID = 100L;

    @Mock private TaskRepository taskRepository;
    @Mock private TeamMemberRepository teamMemberRepository;
    @Mock private UserRepository userRepository;
    @Mock private TeamService teamService;
    @Mock private NotificationPublisher notificationPublisher;
    @Mock private AttachmentRepository attachmentRepository;
    @Mock private FileStorageService fileStorageService;

    @InjectMocks private TaskService taskService;

    private final User priya = user(1L, "Priya Sharma", "priya@example.com");
    private final User amit = user(2L, "Amit Verma", "amit@example.com");
    private final User rahul = user(3L, "Rahul Mehta", "rahul@example.com");
    private final Team team = Team.builder().id(TEAM_ID).name("Website Launch").createdBy(priya).build();

    // ---------------------------------------------------------------------
    // Create
    // ---------------------------------------------------------------------

    @Nested
    @DisplayName("createTask")
    class CreateTask {

        @Test
        @DisplayName("personal task is assigned to its creator, with OPEN status and MEDIUM priority by default")
        void personalTaskDefaults() {
            when(userRepository.findById(1L)).thenReturn(Optional.of(priya));
            when(taskRepository.saveAndFlush(any(Task.class))).thenAnswer(inv -> inv.getArgument(0));

            TaskResponse response = taskService.createTask(1L,
                    new CreateTaskRequest("  Prepare resume  ", null, null, null, null, null));

            assertThat(response.title()).isEqualTo("Prepare resume");
            assertThat(response.teamId()).isNull();
            assertThat(response.assignee().id()).isEqualTo(1L);
            assertThat(response.status()).isEqualTo(TaskStatus.OPEN);
            assertThat(response.priority()).isEqualTo(TaskPriority.MEDIUM);
        }

        @Test
        @DisplayName("personal task cannot be assigned to another user")
        void personalTaskAssignedToOtherUserIsRejected() {
            when(userRepository.findById(1L)).thenReturn(Optional.of(priya));

            assertThatThrownBy(() -> taskService.createTask(1L,
                    new CreateTaskRequest("Personal item", null, null, null, null, 2L)))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("Personal tasks can only be assigned to yourself");

            verify(taskRepository, never()).saveAndFlush(any());
        }

        @Test
        @DisplayName("team task can be assigned to a team member, who is notified")
        void teamTaskAssignedToMember() {
            when(userRepository.findById(1L)).thenReturn(Optional.of(priya));
            when(teamService.requireMembership(TEAM_ID, 1L)).thenReturn(membership(priya, TeamRole.OWNER));
            when(teamMemberRepository.findByTeamIdAndUserId(TEAM_ID, 2L))
                    .thenReturn(Optional.of(membership(amit, TeamRole.MEMBER)));
            when(taskRepository.saveAndFlush(any(Task.class))).thenAnswer(inv -> inv.getArgument(0));

            TaskResponse response = taskService.createTask(1L,
                    new CreateTaskRequest("Design home page", null, null, TaskPriority.HIGH, TEAM_ID, 2L));

            assertThat(response.teamId()).isEqualTo(TEAM_ID);
            assertThat(response.assignee().id()).isEqualTo(2L);
            assertThat(response.priority()).isEqualTo(TaskPriority.HIGH);
            verify(notificationPublisher).taskAssigned(any(Task.class), any());
        }

        @Test
        @DisplayName("team task cannot be assigned to a non-member")
        void teamTaskAssignedToNonMemberIsRejected() {
            when(userRepository.findById(1L)).thenReturn(Optional.of(priya));
            when(teamService.requireMembership(TEAM_ID, 1L)).thenReturn(membership(priya, TeamRole.OWNER));
            when(teamMemberRepository.findByTeamIdAndUserId(TEAM_ID, 3L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> taskService.createTask(1L,
                    new CreateTaskRequest("Write tests", null, null, null, TEAM_ID, 3L)))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("Assignee must be a member of the team");
        }

        @Test
        @DisplayName("a non-member cannot create a task in the team")
        void nonMemberCannotCreateTeamTask() {
            when(userRepository.findById(3L)).thenReturn(Optional.of(rahul));
            when(teamService.requireMembership(TEAM_ID, 3L))
                    .thenThrow(new ForbiddenException("You are not a member of this team"));

            assertThatThrownBy(() -> taskService.createTask(3L,
                    new CreateTaskRequest("Sneaky task", null, null, null, TEAM_ID, null)))
                    .isInstanceOf(ForbiddenException.class);

            verify(taskRepository, never()).saveAndFlush(any());
        }
    }

    // ---------------------------------------------------------------------
    // Access
    // ---------------------------------------------------------------------

    @Nested
    @DisplayName("requireTaskAccess")
    class RequireTaskAccess {

        @Test
        @DisplayName("missing task gives 404")
        void missingTask() {
            when(taskRepository.findWithDetailsById(TASK_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> taskService.requireTaskAccess(TASK_ID, 1L))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("personal task is visible to its creator")
        void personalTaskVisibleToCreator() {
            Task task = personalTask(priya);
            when(taskRepository.findWithDetailsById(TASK_ID)).thenReturn(Optional.of(task));

            assertThat(taskService.requireTaskAccess(TASK_ID, 1L)).isSameAs(task);
        }

        @Test
        @DisplayName("personal task is hidden from other users")
        void personalTaskHiddenFromOthers() {
            when(taskRepository.findWithDetailsById(TASK_ID)).thenReturn(Optional.of(personalTask(priya)));

            assertThatThrownBy(() -> taskService.requireTaskAccess(TASK_ID, 2L))
                    .isInstanceOf(ForbiddenException.class)
                    .hasMessageContaining("You do not have access to this task");
        }

        @Test
        @DisplayName("team task requires team membership")
        void teamTaskRequiresMembership() {
            when(taskRepository.findWithDetailsById(TASK_ID)).thenReturn(Optional.of(teamTask(priya, amit)));
            when(teamService.requireMembership(TEAM_ID, 3L))
                    .thenThrow(new ForbiddenException("You are not a member of this team"));

            assertThatThrownBy(() -> taskService.requireTaskAccess(TASK_ID, 3L))
                    .isInstanceOf(ForbiddenException.class);
        }
    }

    // ---------------------------------------------------------------------
    // Update, status, assign, delete
    // ---------------------------------------------------------------------

    @Nested
    @DisplayName("updateTask")
    class UpdateTask {

        @Test
        @DisplayName("a member who is neither creator nor owner cannot edit details")
        void memberCannotEdit() {
            givenTeamTaskVisibleTo(teamTask(priya, amit), 2L);
            when(teamMemberRepository.findByTeamIdAndUserId(TEAM_ID, 2L))
                    .thenReturn(Optional.of(membership(amit, TeamRole.MEMBER)));

            assertThatThrownBy(() -> taskService.updateTask(TASK_ID, 2L,
                    new UpdateTaskRequest("New title", null, null, null)))
                    .isInstanceOf(ForbiddenException.class)
                    .hasMessageContaining("Only the task creator or the team owner");
        }

        @Test
        @DisplayName("creator can change only the fields sent")
        void creatorUpdatesOnlySentFields() {
            Task task = teamTask(priya, amit);
            givenTeamTaskVisibleTo(task, 1L);
            when(taskRepository.saveAndFlush(task)).thenReturn(task);

            TaskResponse response = taskService.updateTask(TASK_ID, 1L,
                    new UpdateTaskRequest(null, null, null, TaskPriority.HIGH));

            assertThat(response.priority()).isEqualTo(TaskPriority.HIGH);
            assertThat(response.title()).isEqualTo("Design home page");
            verify(notificationPublisher).taskUpdated(task, 1L);
        }

        @Test
        @DisplayName("an empty update is rejected")
        void emptyUpdateRejected() {
            givenTeamTaskVisibleTo(teamTask(priya, amit), 1L);

            assertThatThrownBy(() -> taskService.updateTask(TASK_ID, 1L,
                    new UpdateTaskRequest(null, null, null, null)))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("Provide at least one field to update");
        }
    }

    @Nested
    @DisplayName("updateStatus")
    class UpdateStatus {

        @Test
        @DisplayName("the assignee can mark the task as COMPLETED")
        void assigneeCanComplete() {
            Task task = teamTask(priya, amit);
            givenTeamTaskVisibleTo(task, 2L);
            when(taskRepository.saveAndFlush(task)).thenReturn(task);

            TaskResponse response = taskService.updateStatus(TASK_ID, 2L,
                    new UpdateTaskStatusRequest(TaskStatus.COMPLETED));

            assertThat(response.status()).isEqualTo(TaskStatus.COMPLETED);
            verify(notificationPublisher).taskStatusChanged(task, 2L);
        }

        @Test
        @DisplayName("a member who is not assignee, creator or owner cannot change the status")
        void otherMemberCannotChangeStatus() {
            User neha = user(4L, "Neha Rao", "neha@example.com");
            givenTeamTaskVisibleTo(teamTask(priya, amit), 4L);
            when(teamMemberRepository.findByTeamIdAndUserId(TEAM_ID, 4L))
                    .thenReturn(Optional.of(membership(neha, TeamRole.MEMBER)));

            assertThatThrownBy(() -> taskService.updateStatus(TASK_ID, 4L,
                    new UpdateTaskStatusRequest(TaskStatus.COMPLETED)))
                    .isInstanceOf(ForbiddenException.class);
        }
    }

    @Nested
    @DisplayName("assignTask")
    class AssignTask {

        @Test
        @DisplayName("personal tasks cannot be assigned to other users")
        void personalTaskCannotBeReassigned() {
            when(taskRepository.findWithDetailsById(TASK_ID)).thenReturn(Optional.of(personalTask(priya)));

            assertThatThrownBy(() -> taskService.assignTask(TASK_ID, 1L, new AssignTaskRequest(2L)))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("Personal tasks cannot be assigned");
        }

        @Test
        @DisplayName("unassigning (null) does not send a notification")
        void unassignDoesNotNotify() {
            Task task = teamTask(priya, amit);
            givenTeamTaskVisibleTo(task, 1L);
            when(taskRepository.saveAndFlush(task)).thenReturn(task);

            TaskResponse response = taskService.assignTask(TASK_ID, 1L, new AssignTaskRequest(null));

            assertThat(response.assignee()).isNull();
            verifyNoInteractions(notificationPublisher);
        }
    }

    @Nested
    @DisplayName("deleteTask")
    class DeleteTask {

        @Test
        @DisplayName("the team owner can delete a task created by someone else")
        void ownerCanDelete() {
            Task task = teamTask(amit, null);   // created by Amit
            givenTeamTaskVisibleTo(task, 1L);
            when(teamMemberRepository.findByTeamIdAndUserId(TEAM_ID, 1L))
                    .thenReturn(Optional.of(membership(priya, TeamRole.OWNER)));
            when(attachmentRepository.findStoragePathsByTaskId(TASK_ID)).thenReturn(List.of());

            taskService.deleteTask(TASK_ID, 1L);

            verify(taskRepository).delete(task);
        }

        @Test
        @DisplayName("a member who did not create the task cannot delete it")
        void memberCannotDelete() {
            Task task = teamTask(priya, amit);
            givenTeamTaskVisibleTo(task, 2L);
            when(teamMemberRepository.findByTeamIdAndUserId(TEAM_ID, 2L))
                    .thenReturn(Optional.of(membership(amit, TeamRole.MEMBER)));

            assertThatThrownBy(() -> taskService.deleteTask(TASK_ID, 2L))
                    .isInstanceOf(ForbiddenException.class);

            verify(taskRepository, never()).delete(any(Task.class));
        }
    }

    // ---------------------------------------------------------------------
    // Listing parameters
    // ---------------------------------------------------------------------

    @Nested
    @DisplayName("listTasks parameter validation")
    class ListTasksValidation {

        private final TaskFilter allTasks = new TaskFilter(TaskScope.ALL, null, null, null, null);

        @Test
        @DisplayName("sorting by a field outside the whitelist is rejected")
        void invalidSortField() {
            assertThatThrownBy(() -> taskService.listTasks(1L, allTasks, 0, 20, "password", "asc"))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("sortBy must be one of");
        }

        @Test
        @DisplayName("page size above 100 is rejected")
        void pageSizeTooLarge() {
            assertThatThrownBy(() -> taskService.listTasks(1L, allTasks, 0, 101, "dueDate", "asc"))
                    .isInstanceOf(BadRequestException.class);
        }

        @Test
        @DisplayName("an unknown sort direction is rejected")
        void invalidDirection() {
            assertThatThrownBy(() -> taskService.listTasks(1L, allTasks, 0, 20, "dueDate", "sideways"))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("direction");
        }
    }

    // ---------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------

    private void givenTeamTaskVisibleTo(Task task, Long userId) {
        when(taskRepository.findWithDetailsById(TASK_ID)).thenReturn(Optional.of(task));
        when(teamService.requireMembership(TEAM_ID, userId)).thenReturn(membership(null, TeamRole.MEMBER));
    }

    private Task teamTask(User creator, User assignee) {
        return Task.builder()
                .id(TASK_ID)
                .title("Design home page")
                .status(TaskStatus.OPEN)
                .priority(TaskPriority.MEDIUM)
                .team(team)
                .createdBy(creator)
                .assignee(assignee)
                .build();
    }

    private Task personalTask(User owner) {
        return Task.builder()
                .id(TASK_ID)
                .title("Prepare resume")
                .status(TaskStatus.OPEN)
                .priority(TaskPriority.MEDIUM)
                .createdBy(owner)
                .assignee(owner)
                .build();
    }

    private TeamMember membership(User member, TeamRole role) {
        return TeamMember.builder().team(team).user(member).role(role).build();
    }

    private static User user(Long id, String fullName, String email) {
        return User.builder().id(id).fullName(fullName).email(email).password("hash").build();
    }
}
package com.taskscheduler.taskscheduler.service;

import com.taskscheduler.taskscheduler.dto.CreateTaskRequest;
import com.taskscheduler.taskscheduler.dto.StatusUpdateRequest;
import com.taskscheduler.taskscheduler.dto.UpdateTaskRequest;
import com.taskscheduler.taskscheduler.exception.BadRequestException;
import com.taskscheduler.taskscheduler.exception.TaskNotFoundException;
import com.taskscheduler.taskscheduler.model.Priority;
import com.taskscheduler.taskscheduler.model.Status;
import com.taskscheduler.taskscheduler.model.Task;
import com.taskscheduler.taskscheduler.repository.TaskRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TaskServiceTest {

    @Mock
    private TaskRepository taskRepository;

    private TaskService taskService;

    @BeforeEach
    void setUp() {
        taskService = new TaskService(taskRepository);
    }

    @Test
    void create_setsPendingAndNotDeleted() {
        CreateTaskRequest request = new CreateTaskRequest();
        request.setTitle("Test task");
        request.setDescription("Desc");
        request.setPriority(Priority.HIGH);

        Task saved = new Task(1L, "Test task", "Desc", Priority.HIGH, Status.PENDING, false, Instant.now(), Instant.now());
        when(taskRepository.save(any(Task.class))).thenAnswer(inv -> {
            Task t = inv.getArgument(0);
            t.setId(1L);
            return t;
        });

        Task result = taskService.create(request);

        assertThat(result.getTitle()).isEqualTo("Test task");
        assertThat(result.getDescription()).isEqualTo("Desc");
        assertThat(result.getPriority()).isEqualTo(Priority.HIGH);
        assertThat(result.getStatus()).isEqualTo(Status.PENDING);
        assertThat(result.isDeleted()).isFalse();
        assertThat(result.getCreatedAt()).isNotNull();
        assertThat(result.getUpdatedAt()).isNotNull();
    }

    @Test
    void getById_throwsWhenNotFound() {
        when(taskRepository.findById(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> taskService.getById(99L))
                .isInstanceOf(TaskNotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    void getById_throwsWhenSoftDeleted() {
        Task deleted = new Task(5L, "x", null, Priority.LOW, Status.PENDING, true, Instant.now(), Instant.now());
        when(taskRepository.findById(5L)).thenReturn(Optional.of(deleted));
        assertThatThrownBy(() -> taskService.getById(5L))
                .isInstanceOf(TaskNotFoundException.class)
                .hasMessageContaining("5");
    }

    @Test
    void getById_returnsTaskWhenActive() {
        Task task = new Task(1L, "t", null, Priority.MEDIUM, Status.PENDING, false, Instant.now(), Instant.now());
        when(taskRepository.findById(1L)).thenReturn(Optional.of(task));
        assertThat(taskService.getById(1L)).isSameAs(task);
    }

    @Test
    void update_throwsWhenStatusInBody() {
        Task task = new Task(1L, "t", null, Priority.MEDIUM, Status.PENDING, false, Instant.now(), Instant.now());
        when(taskRepository.findById(1L)).thenReturn(Optional.of(task));

        UpdateTaskRequest request = new UpdateTaskRequest();
        request.setTitle("New title");
        request.setDescription("New desc");
        request.setPriority(Priority.HIGH);
        request.setStatus(Status.COMPLETED);

        assertThatThrownBy(() -> taskService.update(1L, request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("PATCH /tasks/{id}/status");
    }

    @Test
    void update_updatesFieldsAndSaved() {
        Task task = new Task(1L, "old", "oldDesc", Priority.LOW, Status.PENDING, false, Instant.now(), Instant.now());
        when(taskRepository.findById(1L)).thenReturn(Optional.of(task));
        when(taskRepository.save(any(Task.class))).thenAnswer(inv -> inv.getArgument(0));

        UpdateTaskRequest request = new UpdateTaskRequest();
        request.setTitle("New title");
        request.setDescription("New desc");
        request.setPriority(Priority.HIGH);

        Task result = taskService.update(1L, request);

        assertThat(result.getTitle()).isEqualTo("New title");
        assertThat(result.getDescription()).isEqualTo("New desc");
        assertThat(result.getPriority()).isEqualTo(Priority.HIGH);
        assertThat(result.getStatus()).isEqualTo(Status.PENDING);
        verify(taskRepository).save(task);
    }

    @Test
    void softDelete_setsDeletedTrue() {
        Task task = new Task(1L, "t", null, Priority.MEDIUM, Status.PENDING, false, Instant.now(), Instant.now());
        when(taskRepository.findById(1L)).thenReturn(Optional.of(task));
        when(taskRepository.save(any(Task.class))).thenAnswer(inv -> inv.getArgument(0));

        Task result = taskService.softDelete(1L);

        assertThat(result.isDeleted()).isTrue();
        verify(taskRepository).save(task);
    }

    @Test
    void listActive_returnsFromRepository() {
        List<Task> tasks = List.of(
                new Task(1L, "a", null, Priority.HIGH, Status.PENDING, false, Instant.now(), Instant.now())
        );
        when(taskRepository.findAllActive()).thenReturn(tasks);
        assertThat(taskService.listActive()).isEqualTo(tasks);
    }

    @Test
    void updateStatus_pendingToInProgress_allowed() {
        Task task = new Task(1L, "t", null, Priority.MEDIUM, Status.PENDING, false, Instant.now(), Instant.now());
        when(taskRepository.findById(1L)).thenReturn(Optional.of(task));
        when(taskRepository.save(any(Task.class))).thenAnswer(inv -> inv.getArgument(0));

        StatusUpdateRequest request = new StatusUpdateRequest();
        request.setStatus(Status.IN_PROGRESS);

        Task result = taskService.updateStatus(1L, request);

        assertThat(result.getStatus()).isEqualTo(Status.IN_PROGRESS);
    }

    @Test
    void updateStatus_pendingToCancelled_allowed() {
        Task task = new Task(1L, "t", null, Priority.MEDIUM, Status.PENDING, false, Instant.now(), Instant.now());
        when(taskRepository.findById(1L)).thenReturn(Optional.of(task));
        when(taskRepository.save(any(Task.class))).thenAnswer(inv -> inv.getArgument(0));

        StatusUpdateRequest request = new StatusUpdateRequest();
        request.setStatus(Status.CANCELLED);

        Task result = taskService.updateStatus(1L, request);
        assertThat(result.getStatus()).isEqualTo(Status.CANCELLED);
    }

    @Test
    void updateStatus_inProgressToCompleted_allowed() {
        Task task = new Task(1L, "t", null, Priority.MEDIUM, Status.IN_PROGRESS, false, Instant.now(), Instant.now());
        when(taskRepository.findById(1L)).thenReturn(Optional.of(task));
        when(taskRepository.save(any(Task.class))).thenAnswer(inv -> inv.getArgument(0));

        StatusUpdateRequest request = new StatusUpdateRequest();
        request.setStatus(Status.COMPLETED);

        Task result = taskService.updateStatus(1L, request);
        assertThat(result.getStatus()).isEqualTo(Status.COMPLETED);
    }

    @Test
    void updateStatus_inProgressToCancelled_allowed() {
        Task task = new Task(1L, "t", null, Priority.MEDIUM, Status.IN_PROGRESS, false, Instant.now(), Instant.now());
        when(taskRepository.findById(1L)).thenReturn(Optional.of(task));
        when(taskRepository.save(any(Task.class))).thenAnswer(inv -> inv.getArgument(0));

        StatusUpdateRequest request = new StatusUpdateRequest();
        request.setStatus(Status.CANCELLED);

        Task result = taskService.updateStatus(1L, request);
        assertThat(result.getStatus()).isEqualTo(Status.CANCELLED);
    }

    @Test
    void updateStatus_pendingToCompleted_rejected() {
        Task task = new Task(1L, "t", null, Priority.MEDIUM, Status.PENDING, false, Instant.now(), Instant.now());
        when(taskRepository.findById(1L)).thenReturn(Optional.of(task));

        StatusUpdateRequest request = new StatusUpdateRequest();
        request.setStatus(Status.COMPLETED);

        assertThatThrownBy(() -> taskService.updateStatus(1L, request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Cannot transition from PENDING to COMPLETED");
    }

    @Test
    void updateStatus_completedToAny_rejected() {
        Task task = new Task(1L, "t", null, Priority.MEDIUM, Status.COMPLETED, false, Instant.now(), Instant.now());
        when(taskRepository.findById(1L)).thenReturn(Optional.of(task));

        StatusUpdateRequest request = new StatusUpdateRequest();
        request.setStatus(Status.PENDING);

        assertThatThrownBy(() -> taskService.updateStatus(1L, request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Cannot transition from COMPLETED");
    }

    @Test
    void updateStatus_cancelledToAny_rejected() {
        Task task = new Task(1L, "t", null, Priority.MEDIUM, Status.CANCELLED, false, Instant.now(), Instant.now());
        when(taskRepository.findById(1L)).thenReturn(Optional.of(task));

        StatusUpdateRequest request = new StatusUpdateRequest();
        request.setStatus(Status.IN_PROGRESS);

        assertThatThrownBy(() -> taskService.updateStatus(1L, request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Cannot transition from CANCELLED");
    }
}

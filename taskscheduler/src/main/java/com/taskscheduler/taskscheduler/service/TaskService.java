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
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
public class TaskService {

    private final TaskRepository taskRepository;

    public TaskService(TaskRepository taskRepository) {
        this.taskRepository = taskRepository;
    }

    public Task create(CreateTaskRequest request) {
        Instant now = Instant.now();
        Task task = new Task(
                null,
                request.getTitle().trim(),
                request.getDescription() != null ? request.getDescription().trim() : null,
                request.getPriority(),
                Status.PENDING,
                false,
                now,
                now
        );
        return taskRepository.save(task);
    }

    public Task getById(Long id) {
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new TaskNotFoundException(id));
        if (task.isDeleted()) {
            throw new TaskNotFoundException(id);
        }
        return task;
    }

    public Task update(Long id, UpdateTaskRequest request) {
        Task task = getById(id);
        if (request.getStatus() != null) {
            throw new BadRequestException("Use PATCH /tasks/{id}/status");
        }
        task.setTitle(request.getTitle().trim());
        task.setDescription(request.getDescription() != null ? request.getDescription().trim() : null);
        if (request.getPriority() != null) {
            task.setPriority(request.getPriority());
        }
        task.setUpdatedAt(Instant.now());
        return taskRepository.save(task);
    }

    public Task softDelete(Long id) {
        Task task = getById(id);
        task.setDeleted(true);
        task.setUpdatedAt(Instant.now());
        return taskRepository.save(task);
    }

    public List<Task> listActive() {
        return taskRepository.findAllActive();
    }

    public Task updateStatus(Long id, StatusUpdateRequest request) {
        Task task = getById(id);
        Status current = task.getStatus();
        Status requested = request.getStatus();

        if (!isTransitionAllowed(current, requested)) {
            throw new BadRequestException(
                    "Cannot transition from " + current + " to " + requested);
        }

        task.setStatus(requested);
        task.setUpdatedAt(Instant.now());
        return taskRepository.save(task);
    }

    private boolean isTransitionAllowed(Status current, Status next) {
        return switch (current) {
            case PENDING -> next == Status.IN_PROGRESS || next == Status.CANCELLED;
            case IN_PROGRESS -> next == Status.COMPLETED || next == Status.CANCELLED;
            case COMPLETED, CANCELLED -> false;
        };
    }
}

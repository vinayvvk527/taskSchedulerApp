package com.taskscheduler.taskscheduler.repository;

import com.taskscheduler.taskscheduler.model.Task;

import java.util.List;
import java.util.Optional;

public interface TaskRepository {
    Task save(Task task);
    Optional<Task> findById(Long id);
    List<Task> findAllActive();
    boolean existsByIdAndNotDeleted(Long id);
}

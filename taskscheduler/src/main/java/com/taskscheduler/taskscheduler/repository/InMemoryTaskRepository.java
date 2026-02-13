package com.taskscheduler.taskscheduler.repository;

import com.taskscheduler.taskscheduler.model.Task;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

@Repository
public class InMemoryTaskRepository implements TaskRepository {

    private final Map<Long, Task> store = new ConcurrentHashMap<>();
    private final AtomicLong idGenerator = new AtomicLong(1);

    @Override
    public Task save(Task task) {
        if (task.getId() == null) {
            task.setId(idGenerator.getAndIncrement());
        }
        store.put(task.getId(), task);
        return task;
    }

    @Override
    public Optional<Task> findById(Long id) {
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public List<Task> findAllActive() {
        return store.values().stream()
                .filter(t -> !t.isDeleted())
                .collect(Collectors.toList());
    }

    @Override
    public boolean existsByIdAndNotDeleted(Long id) {
        return Optional.ofNullable(store.get(id))
                .map(t -> !t.isDeleted())
                .orElse(false);
    }
}

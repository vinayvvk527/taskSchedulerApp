package com.taskscheduler.taskscheduler.controller;

import com.taskscheduler.taskscheduler.dto.CreateTaskRequest;
import com.taskscheduler.taskscheduler.dto.StatusUpdateRequest;
import com.taskscheduler.taskscheduler.dto.UpdateTaskRequest;
import com.taskscheduler.taskscheduler.model.Task;
import com.taskscheduler.taskscheduler.service.TaskService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/tasks")
public class TaskController {

    private final TaskService taskService;

    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    @PostMapping
    public ResponseEntity<Task> create(@Valid @RequestBody CreateTaskRequest request) {
        Task task = taskService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(task);
    }

//    @GetMapping("/hello")
//    public ResponseEntity<String> sayHi(){
//        return ResponseEntity.status(HttpStatus.OK).body("hello");
//    }

    @GetMapping("/{id}")
    public ResponseEntity<Task> getById(@PathVariable Long id) {
        Task task = taskService.getById(id);
        return ResponseEntity.ok(task);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Task> update(@PathVariable Long id, @Valid @RequestBody UpdateTaskRequest request) {
        Task task = taskService.update(id, request);
        return ResponseEntity.ok(task);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Task> softDelete(@PathVariable Long id) {
        Task task = taskService.softDelete(id);
        return ResponseEntity.ok(task);
    }

    @GetMapping
    public ResponseEntity<List<Task>> listActive() {
        List<Task> tasks = taskService.listActive();
        return ResponseEntity.ok(tasks);
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<Task> updateStatus(@PathVariable Long id, @Valid @RequestBody StatusUpdateRequest request) {
        Task task = taskService.updateStatus(id, request);
        return ResponseEntity.ok(task);
    }
}

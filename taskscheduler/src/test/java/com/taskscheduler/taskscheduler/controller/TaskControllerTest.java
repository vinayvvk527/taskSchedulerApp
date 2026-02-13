package com.taskscheduler.taskscheduler.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taskscheduler.taskscheduler.exception.TaskNotFoundException;
import com.taskscheduler.taskscheduler.model.Priority;
import com.taskscheduler.taskscheduler.model.Status;
import com.taskscheduler.taskscheduler.model.Task;
import com.taskscheduler.taskscheduler.exception.GlobalExceptionHandler;
import com.taskscheduler.taskscheduler.service.TaskService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(TaskController.class)
@Import(GlobalExceptionHandler.class)
class TaskControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private TaskService taskService;

    @Test
    void create_returns201AndTask() throws Exception {
        Task task = new Task(1L, "Implement login API", "Add JWT auth", Priority.HIGH, Status.PENDING, false,
                Instant.parse("2025-01-15T10:30:00Z"), Instant.parse("2025-01-15T10:30:00Z"));
        when(taskService.create(any())).thenReturn(task);

        String body = "{\"title\":\"Implement login API\",\"description\":\"Add JWT auth\",\"priority\":\"HIGH\"}";

        mockMvc.perform(post("/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.title").value("Implement login API"))
                .andExpect(jsonPath("$.description").value("Add JWT auth"))
                .andExpect(jsonPath("$.priority").value("HIGH"))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.deleted").value(false));
    }

    @Test
    void create_missingTitle_returns400() throws Exception {
        String body = "{\"description\":\"Add JWT auth\",\"priority\":\"HIGH\"}";
        mockMvc.perform(post("/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.message", containsString("title is required")));
    }

    @Test
    void create_titleTooLong_returns400() throws Exception {
        String longTitle = "a".repeat(101);
        String body = "{\"title\":\"" + longTitle + "\",\"priority\":\"HIGH\"}";
        mockMvc.perform(post("/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("100 characters")));
    }

    @Test
    void create_invalidPriority_returns400() throws Exception {
        String body = "{\"title\":\"Task\",\"priority\":\"INVALID\"}";
        mockMvc.perform(post("/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("BAD_REQUEST"));
    }

    @Test
    void getById_returns200AndTask() throws Exception {
        Task task = new Task(1L, "Implement login API", "Add JWT auth", Priority.HIGH, Status.PENDING, false,
                Instant.now(), Instant.now());
        when(taskService.getById(1L)).thenReturn(task);

        mockMvc.perform(get("/tasks/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.title").value("Implement login API"));
    }

    @Test
    void getById_notFound_returns404() throws Exception {
        when(taskService.getById(99L)).thenThrow(new TaskNotFoundException(99L));

        mockMvc.perform(get("/tasks/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("NOT_FOUND"))
                .andExpect(jsonPath("$.message", containsString("99")));
    }

    @Test
    void update_returns200AndTask() throws Exception {
        Task task = new Task(1L, "Implement login API v2", "Add OAuth 2.0", Priority.MEDIUM, Status.PENDING, false,
                Instant.now(), Instant.now());
        when(taskService.update(eq(1L), any())).thenReturn(task);

        String body = "{\"title\":\"Implement login API v2\",\"description\":\"Add OAuth 2.0\",\"priority\":\"MEDIUM\"}";
        mockMvc.perform(put("/tasks/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Implement login API v2"))
                .andExpect(jsonPath("$.priority").value("MEDIUM"));
    }

    @Test
    void softDelete_returns200AndTaskWithDeletedTrue() throws Exception {
        Task task = new Task(1L, "Implement login API", "Add JWT auth", Priority.HIGH, Status.PENDING, true,
                Instant.now(), Instant.now());
        when(taskService.softDelete(1L)).thenReturn(task);

        mockMvc.perform(delete("/tasks/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.deleted").value(true));
    }

    @Test
    void listActive_returns200AndArray() throws Exception {
        Task t1 = new Task(1L, "Task 1", null, Priority.HIGH, Status.PENDING, false, Instant.now(), Instant.now());
        when(taskService.listActive()).thenReturn(List.of(t1));

        mockMvc.perform(get("/tasks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].title").value("Task 1"));
    }

    @Test
    void listActive_empty_returns200EmptyArray() throws Exception {
        when(taskService.listActive()).thenReturn(List.of());

        mockMvc.perform(get("/tasks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void updateStatus_returns200AndUpdatedStatus() throws Exception {
        Task task = new Task(1L, "Implement login API", "Add JWT auth", Priority.HIGH, Status.IN_PROGRESS, false,
                Instant.now(), Instant.now());
        when(taskService.updateStatus(eq(1L), any())).thenReturn(task);

        mockMvc.perform(patch("/tasks/1/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"IN_PROGRESS\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"));
    }
}

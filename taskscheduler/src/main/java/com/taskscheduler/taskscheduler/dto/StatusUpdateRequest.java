package com.taskscheduler.taskscheduler.dto;

import com.taskscheduler.taskscheduler.model.Status;
import jakarta.validation.constraints.NotNull;

public class StatusUpdateRequest {
    @NotNull(message = "status must be a valid value")
    private Status status;

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }
}

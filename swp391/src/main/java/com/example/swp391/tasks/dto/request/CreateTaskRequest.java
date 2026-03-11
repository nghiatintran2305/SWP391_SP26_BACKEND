package com.example.swp391.tasks.dto.request;

import com.example.swp391.tasks.enums.TaskPriority;
import com.example.swp391.tasks.enums.TaskStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateTaskRequest {

    @NotBlank(message = "Task name is required")
    private String taskName;

    private String description;

    @NotNull(message = "Status is required")
    private TaskStatus status;

    @NotNull(message = "Priority is required")
    private TaskPriority priority;

    private String assignedToId;

    private LocalDateTime dueDate;

    private boolean isRequirement;
}

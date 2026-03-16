package com.example.swp391.tasks.dto.request;

import com.example.swp391.tasks.enums.TaskPriority;
import com.example.swp391.tasks.enums.TaskStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateTaskRequest {

    private String taskName;

    private String description;

    private TaskStatus status;

    private TaskPriority priority;

    private String assignedToId;

    private LocalDateTime dueDate;
}

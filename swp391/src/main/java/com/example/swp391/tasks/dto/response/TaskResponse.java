package com.example.swp391.tasks.dto.response;

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
public class TaskResponse {

    private String id;

    private String projectId;

    private String projectName;

    private String taskName;

    private String description;

    private String jiraIssueId;

    private String jiraIssueKey;

    private TaskStatus status;

    private TaskPriority priority;

    private String assignedToId;

    private String assignedToName;

    private String assignedToUsername;

    private String createdById;

    private String createdByName;

    private LocalDateTime createdAt;

    private LocalDateTime dueDate;

    private boolean isRequirement;
}

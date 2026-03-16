package com.example.swp391.tasks.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskProgressReport {

    private String projectId;

    private String projectName;

    private long totalTasks;

    private long completedTasks;

    private long inProgressTasks;

    private long todoTasks;

    private long blockedTasks;

    private double completionPercentage;

    private Map<String, Long> tasksByStatus;

    private Map<String, Long> tasksByPriority;
}

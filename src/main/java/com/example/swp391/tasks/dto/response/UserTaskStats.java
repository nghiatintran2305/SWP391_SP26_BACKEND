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
public class UserTaskStats {

    private String userId;

    private String userName;

    private long totalAssignedTasks;

    private long completedTasks;

    private long inProgressTasks;

    private long todoTasks;

    private double completionPercentage;

    private Map<String, Long> tasksByStatus;
}

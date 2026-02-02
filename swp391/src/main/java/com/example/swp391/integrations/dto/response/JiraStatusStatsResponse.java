package com.example.swp391.integrations.dto.response;

import java.util.Map;
import lombok.Getter;
import lombok.Setter;

/**
 * Simple aggregated stats for Jira issues in a group.
 * Example: {"To Do": 10, "In Progress": 5, "Done": 7}
 */
@Getter
@Setter
public class JiraStatusStatsResponse {
    private String groupId;
    private Map<String, Long> countByStatus;
}

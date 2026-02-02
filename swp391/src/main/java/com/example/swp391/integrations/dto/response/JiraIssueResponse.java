package com.example.swp391.integrations.dto.response;

import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

/**
 * DTO returned to FE for cached Jira issues.
 *
 * Note: assignee is Jira identity, we also provide mapped account (if exists).
 */
@Getter
@Setter
public class JiraIssueResponse {
    private String id;
    private String groupId;
    private String jiraIssueKey;
    private String issueType;
    private String summary;
    private String description;
    private String status;
    private String sprintId;
    private String sprintName;

    // Jira-side assignee
    private String assigneeJiraAccountId;

    // Local account mapping (nullable if unmapped)
    private String mappedAccountId;
    private String mappedUsername;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

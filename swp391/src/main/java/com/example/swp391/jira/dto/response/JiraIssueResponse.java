package com.example.swp391.jira.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JiraIssueResponse {

    private String id;

    private String key;

    private String summary;

    private String description;

    private String status;

    private String priority;

    private String assigneeAccountId;

    private String assigneeDisplayName;

    private String reporterAccountId;

    private String reporterDisplayName;

    private String issueType;

    private LocalDateTime created;

    private LocalDateTime updated;

    private LocalDateTime dueDate;
}

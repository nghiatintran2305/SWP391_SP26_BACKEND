package com.example.swp391.integrations.dto.response;

import java.util.List;
import lombok.Getter;
import lombok.Setter;

/**
 * Wrapper response for "my issues" endpoint.
 *
 * If the current user has no Jira identity mapping, mapped = false and issues = empty.
 */
@Getter
@Setter
public class MyJiraIssuesResponse {
    private boolean mapped;
    private String jiraAccountId;
    private List<JiraIssueResponse> issues;
}

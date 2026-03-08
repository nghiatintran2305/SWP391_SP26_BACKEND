package com.example.swp391.jira.service;

import com.example.swp391.jira.dto.response.JiraIssueResponse;
import com.example.swp391.jira.dto.response.JiraProjectResponse;

import java.util.List;

public interface IJiraService {

    JiraProjectResponse createProject(
            String projectKey,
            String projectName,
            String leadAccountId
    );

    void deleteProjectQuietly(String projectKey);

    void addUserToProjectRole(String projectKey, String roleId, String accountId);

    void removeUserFromProjectRole(String projectKey, String roleId, String accountId);

    // Task/Issue Management
    List<JiraIssueResponse> getProjectIssues(String projectKey);

    JiraIssueResponse getIssueByKey(String projectKey, String issueKey);

    JiraIssueResponse createIssue(
            String projectKey,
            String summary,
            String description,
            String issueType,
            String priority,
            String assigneeAccountId
    );

    JiraIssueResponse updateIssueStatus(String issueKey, String status);

    void deleteIssue(String issueKey);
}

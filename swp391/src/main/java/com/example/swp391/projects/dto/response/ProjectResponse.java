package com.example.swp391.projects.dto.response;

import com.example.swp391.projects.enums.ProjectStatus;
import lombok.Data;

@Data
public class ProjectResponse {

    private String id;

    private String lecturerId;

    private String projectName;

    private String jiraProjectId;

    private String jiraProjectKey;

    private String githubRepoName;

    private String githubRepoUrl;

    private ProjectStatus status;

    private long taskCount;

    private boolean canDelete;

    private String currentUserGroupRole;

}

package com.example.swp391.projects.dto.response;

import com.example.swp391.projects.enums.GroupStatus;
import lombok.Data;


@Data
public class ProjectGroupResponse {

    private String id;
    private String groupName;
    private String jiraGroupName;
    private String GithubTeamName;
    private String githubTeamSlug;
    private GroupStatus status;
}

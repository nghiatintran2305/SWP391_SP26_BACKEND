package com.example.swp391.projects.dto.request;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Data
public class CreateProjectGroupRequest {
    private String groupName;
    private String jiraGroupName;
    private String GithubTeamName;
}

package com.example.swp391.projects.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProjectMemberRoleResponse {

    private String projectId;

    private String role; // LEADER, MEMBER, or null
}

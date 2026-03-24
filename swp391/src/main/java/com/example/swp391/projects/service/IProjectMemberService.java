package com.example.swp391.projects.service;

import com.example.swp391.projects.dto.response.ProjectMemberRoleResponse;
import com.example.swp391.projects.dto.response.ProjectResponse;
import com.example.swp391.projects.enums.ProjectRole;

import java.util.List;

public interface IProjectMemberService {
    void addMemberToProject(String projectId, String accountId, ProjectRole role);

    void removeMemberFromProject(String projectId, String accountId);

    List<ProjectResponse> getProjectsByMemberId(String accountId);

    ProjectMemberRoleResponse getMemberRoleInProject(String projectId, String accountId);
}

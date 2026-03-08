package com.example.swp391.projects.service;

import com.example.swp391.projects.enums.ProjectRole;

public interface IProjectMemberService {
    void addMemberToProject(String projectId, String accountId, ProjectRole role);

    void removeMemberFromProject(String projectId, String accountId) ;
}

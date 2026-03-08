package com.example.swp391.projects.service;

import com.example.swp391.projects.dto.request.CreateProjectRequest;
import com.example.swp391.projects.dto.response.ProjectResponse;
import com.example.swp391.projects.enums.ProjectStatus;

import java.util.List;

public interface IProjectService {
    ProjectResponse createProject(CreateProjectRequest req);

    ProjectResponse getProjectById(String id);

    List<ProjectResponse> getAllProjects();

//    List<ProjectResponse> getProjectsByStatus(ProjectStatus status);

    ProjectResponse updateProject(String id, CreateProjectRequest req);

    void deleteProject(String id);

    ProjectResponse updateProjectStatus(String id, ProjectStatus status);
}


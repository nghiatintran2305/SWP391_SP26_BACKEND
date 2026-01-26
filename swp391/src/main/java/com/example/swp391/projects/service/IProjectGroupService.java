package com.example.swp391.projects.service;

import com.example.swp391.projects.dto.request.CreateProjectGroupRequest;
import com.example.swp391.projects.dto.response.ProjectGroupResponse;

public interface IProjectGroupService {

    ProjectGroupResponse createProjectGroup(CreateProjectGroupRequest request);

}


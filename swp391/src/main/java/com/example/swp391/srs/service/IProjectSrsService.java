package com.example.swp391.srs.service;

import com.example.swp391.srs.dto.request.UpsertProjectSrsRequest;
import com.example.swp391.srs.dto.response.ProjectSrsResponse;

public interface IProjectSrsService {
    ProjectSrsResponse getProjectSrs(String projectId, String currentUserId);

    ProjectSrsResponse saveProjectSrs(String projectId, String currentUserId, UpsertProjectSrsRequest request);
}

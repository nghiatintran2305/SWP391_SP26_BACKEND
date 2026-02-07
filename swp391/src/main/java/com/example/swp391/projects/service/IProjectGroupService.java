package com.example.swp391.projects.service;

import com.example.swp391.projects.dto.request.CreateProjectGroupRequest;
import com.example.swp391.projects.dto.request.UpsertGroupMembersRequest;
import com.example.swp391.projects.dto.response.GroupDetailResponse;
import com.example.swp391.projects.dto.response.GroupMemberResponse;
import com.example.swp391.projects.dto.response.MyProjectGroupResponse;
import com.example.swp391.projects.dto.response.ProjectGroupResponse;
import java.util.List;

public interface IProjectGroupService {
    ProjectGroupResponse createProjectGroup(CreateProjectGroupRequest request);

}


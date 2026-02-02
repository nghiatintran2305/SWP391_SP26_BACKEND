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

    /**
     * Get groups relevant to the current account.
     * - STUDENT: groups where the user is a member.
     * - LECTURER: groups assigned to the lecturer.
     * - ADMIN: all groups.
     */
    List<MyProjectGroupResponse> getMyGroups();

    /**
     * Get group detail (including lecturer info and my role if applicable).
     * Access is checked by GroupAccessService.
     */
    GroupDetailResponse getGroupDetail(String groupId);

    /**
     * List members of a group.
     */
    List<GroupMemberResponse> getGroupMembers(String groupId);

    /**
     * Admin adds/replaces members for a group.
     * (You can extend later to allow leader operations if needed.)
     */
    List<GroupMemberResponse> upsertMembers(String groupId, UpsertGroupMembersRequest request);

}


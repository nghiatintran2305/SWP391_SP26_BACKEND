package com.example.swp391.projects.controller;

import com.example.swp391.projects.dto.request.CreateProjectGroupRequest;
import com.example.swp391.projects.dto.request.UpsertGroupMembersRequest;
import com.example.swp391.projects.dto.response.GroupDetailResponse;
import com.example.swp391.projects.dto.response.GroupMemberResponse;
import com.example.swp391.projects.dto.response.MyProjectGroupResponse;
import com.example.swp391.projects.dto.response.ProjectGroupResponse;
import com.example.swp391.projects.service.IProjectGroupService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/project-groups")
@RequiredArgsConstructor
public class ProjectGroupController {

    private final IProjectGroupService projectGroupService;

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<ProjectGroupResponse> createProjectGroup(
            @Valid @RequestBody CreateProjectGroupRequest request
    ) {
        ProjectGroupResponse response = projectGroupService.createProjectGroup(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    /**
     * List groups for current user.
     * - STUDENT: groups where user is a member
     * - LECTURER: groups assigned to lecturer
     * - ADMIN: all groups
     */
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/me")
    public ResponseEntity<List<MyProjectGroupResponse>> getMyGroups() {
        return ResponseEntity.ok(projectGroupService.getMyGroups());
    }

    /**
     * Group detail (includes lecturer info and member list).
     */
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/{groupId}")
    public ResponseEntity<GroupDetailResponse> getGroupDetail(@PathVariable String groupId) {
        return ResponseEntity.ok(projectGroupService.getGroupDetail(groupId));
    }

    /**
     * List group members.
     */
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/{groupId}/members")
    public ResponseEntity<List<GroupMemberResponse>> getMembers(@PathVariable String groupId) {
        return ResponseEntity.ok(projectGroupService.getGroupMembers(groupId));
    }

    /**
     * Admin adds/updates members in a group.
     */
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/{groupId}/members")
    public ResponseEntity<List<GroupMemberResponse>> upsertMembers(
            @PathVariable String groupId,
            @Valid @RequestBody UpsertGroupMembersRequest request
    ) {
        return ResponseEntity.ok(projectGroupService.upsertMembers(groupId, request));
    }
}

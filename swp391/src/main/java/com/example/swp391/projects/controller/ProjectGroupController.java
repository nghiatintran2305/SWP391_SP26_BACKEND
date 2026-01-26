package com.example.swp391.projects.controller;

import com.example.swp391.projects.dto.request.CreateProjectGroupRequest;
import com.example.swp391.projects.dto.response.ProjectGroupResponse;
import com.example.swp391.projects.service.IProjectGroupService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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
}

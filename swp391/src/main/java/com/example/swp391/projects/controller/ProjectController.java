package com.example.swp391.projects.controller;

import com.example.swp391.projects.dto.request.CreateProjectRequest;
import com.example.swp391.projects.dto.response.ProjectResponse;
import com.example.swp391.projects.enums.ProjectStatus;
import com.example.swp391.projects.service.IProjectService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/projects")
@RequiredArgsConstructor
public class ProjectController {

    private final IProjectService groupService;

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<ProjectResponse> createProject(
            @Valid @RequestBody CreateProjectRequest request
    ) {
        ProjectResponse response = groupService.createProject(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<ProjectResponse>> getAllProjects() {
        List<ProjectResponse> responses = groupService.getAllProjects();
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProjectResponse> getProjectById(@PathVariable String id) {
        ProjectResponse response = groupService.getProjectById(id);
        return ResponseEntity.ok(response);
    }

//    @GetMapping("/status/{status}")
//    public ResponseEntity<List<ProjectResponse>> getProjectsByStatus(
//            @PathVariable ProjectStatus status
//    ) {
//        List<ProjectResponse> responses = groupService.getProjectsByStatus(status);
//        return ResponseEntity.ok(responses);
//    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}")
    public ResponseEntity<ProjectResponse> updateProject(
            @PathVariable String id,
            @Valid @RequestBody CreateProjectRequest request
    ) {
        ProjectResponse response = groupService.updateProject(id, request);
        return ResponseEntity.ok(response);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProject(@PathVariable String id) {
        groupService.deleteProject(id);
        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}/status")
    public ResponseEntity<ProjectResponse> updateProjectStatus(
            @PathVariable String id,
            @RequestParam ProjectStatus status
    ) {
        ProjectResponse response = groupService.updateProjectStatus(id, status);
        return ResponseEntity.ok(response);
    }
}

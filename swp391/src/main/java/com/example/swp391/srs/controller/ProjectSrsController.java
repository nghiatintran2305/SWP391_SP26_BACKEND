package com.example.swp391.srs.controller;

import com.example.swp391.accounts.repository.AccountRepository;
import com.example.swp391.configs.security.SecurityUtil;
import com.example.swp391.srs.dto.request.UpsertProjectSrsRequest;
import com.example.swp391.srs.dto.response.ProjectSrsResponse;
import com.example.swp391.srs.service.IProjectSrsService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/projects")
@RequiredArgsConstructor
public class ProjectSrsController {

    private final IProjectSrsService projectSrsService;
    private final AccountRepository accountRepository;

    @PreAuthorize("hasAnyRole('ADMIN','LECTURER','STUDENT','LEADER','MEMBER')")
    @GetMapping("/{projectId}/srs")
    public ResponseEntity<ProjectSrsResponse> getProjectSrs(@PathVariable String projectId) {
        String currentUserId = SecurityUtil.getCurrentUserId(accountRepository);
        ProjectSrsResponse response = projectSrsService.getProjectSrs(projectId, currentUserId);
        return ResponseEntity.ok(response);
    }

    @PreAuthorize("hasAnyRole('STUDENT','LEADER')")
    @PutMapping("/{projectId}/srs")
    public ResponseEntity<ProjectSrsResponse> saveProjectSrs(
            @PathVariable String projectId,
            @Valid @RequestBody UpsertProjectSrsRequest request
    ) {
        String currentUserId = SecurityUtil.getCurrentUserId(accountRepository);
        ProjectSrsResponse response = projectSrsService.saveProjectSrs(projectId, currentUserId, request);
        return ResponseEntity.ok(response);
    }
}

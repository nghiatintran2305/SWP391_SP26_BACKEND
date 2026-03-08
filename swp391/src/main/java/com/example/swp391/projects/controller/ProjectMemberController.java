package com.example.swp391.projects.controller;

import com.example.swp391.accounts.dto.response.AccountResponse;
import com.example.swp391.projects.dto.request.AddMemberRequest;
import com.example.swp391.projects.entity.ProjectMember;
import com.example.swp391.projects.enums.ProjectRole;
import com.example.swp391.projects.repository.ProjectMemberRepository;
import com.example.swp391.projects.service.IProjectMemberService;
import com.example.swp391.projects.service.IProjectService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/projects")
@RequiredArgsConstructor
public class ProjectMemberController {

    private final IProjectMemberService groupMemberService;
    private final ProjectMemberRepository projectMemberRepository;
    private final IProjectService projectService;

    //Thêm thành viên

    @PreAuthorize("hasAnyRole('ADMIN','LECTURER')")
    @PostMapping("/{projectId}/members")
    public ResponseEntity<Void> addMember(
            @PathVariable String projectId,
            @Valid @RequestBody AddMemberRequest request
    ) {
        groupMemberService.addMemberToProject(
                projectId,
                request.getAccountId(),
                request.getRole()
        );
        return ResponseEntity.ok().build();
    }

    //Xóa thành viên

    @PreAuthorize("hasAnyRole('ADMIN','LECTURER')")
    @DeleteMapping("/{projectId}/members/{accountId}")
    public ResponseEntity<Void> removeMember(
            @PathVariable String projectId,
            @PathVariable String accountId
    ) {
        groupMemberService.removeMemberFromProject(projectId, accountId);
        return ResponseEntity.noContent().build();
    }

    //Lấy danh sách thành viên

    @PreAuthorize("hasAnyRole('ADMIN', 'LECTURER', 'LEADER', 'MEMBER')")
    @GetMapping("/{projectId}/members")
    public ResponseEntity<List<AccountResponse>> getProjectMembers(
            @PathVariable String projectId
    ) {
        List<ProjectMember> members = projectMemberRepository.findAll().stream()
                .filter(m -> m.getProject().getId().equals(projectId))
                .collect(Collectors.toList());

        List<AccountResponse> responses = members.stream()
                .map(m -> {
                    AccountResponse response = new AccountResponse();
                    response.setId(m.getAccount().getId());
                    response.setUsername(m.getAccount().getUsername());
                    response.setEmail(m.getAccount().getEmail());
                    response.setRole(m.getAccount().getRole().getName());
                    response.setActive(m.getAccount().isActive());
                    return response;
                })
                .collect(Collectors.toList());

        return ResponseEntity.ok(responses);
    }
}

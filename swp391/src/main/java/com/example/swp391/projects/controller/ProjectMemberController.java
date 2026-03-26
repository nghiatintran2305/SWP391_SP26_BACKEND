package com.example.swp391.projects.controller;

import com.example.swp391.accounts.dto.response.AccountResponse;
import com.example.swp391.accounts.entity.Account;
import com.example.swp391.accounts.repository.AccountRepository;
import com.example.swp391.configs.security.SecurityUtil;
import com.example.swp391.exceptions.ForbiddenException;
import com.example.swp391.exceptions.NotFoundException;
import com.example.swp391.projects.dto.request.AddMemberRequest;
import com.example.swp391.projects.dto.response.ProjectMemberRoleResponse;
import com.example.swp391.projects.dto.response.ProjectResponse;
import com.example.swp391.projects.entity.Project;
import com.example.swp391.projects.entity.ProjectMember;
import com.example.swp391.projects.enums.ProjectRole;
import com.example.swp391.projects.repository.ProjectMemberRepository;
import com.example.swp391.projects.repository.ProjectRepository;
import com.example.swp391.projects.service.IProjectMemberService;
import com.example.swp391.projects.service.IProjectService;
import com.example.swp391.projects.service.ProjectReportExportService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
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
    private final ProjectRepository projectRepository;
    private final AccountRepository accountRepository;
    private final ProjectReportExportService projectReportExportService;

    //Add member

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

    //Remove member

    @PreAuthorize("hasAnyRole('ADMIN','LECTURER')")
    @DeleteMapping("/{projectId}/members/{accountId}")
    public ResponseEntity<Void> removeMember(
            @PathVariable String projectId,
            @PathVariable String accountId
    ) {
        groupMemberService.removeMemberFromProject(projectId, accountId);
        return ResponseEntity.noContent().build();
    }

    //Get list of members

    @PreAuthorize("hasAnyRole('ADMIN', 'LECTURER', 'STUDENT')")
    @GetMapping("/{projectId}/members")
    public ResponseEntity<List<AccountResponse>> getProjectMembers(
            @PathVariable String projectId,
            @RequestParam(defaultValue = "false") boolean includeLecturer
    ) {
        ensureCanViewProjectMembers(projectId);

        List<ProjectMember> members = projectMemberRepository.findByProjectId(projectId);

        List<AccountResponse> responses = members.stream()
                .map(m -> {
                    AccountResponse response = new AccountResponse();
                    response.setId(m.getAccount().getId());
                    response.setUsername(m.getAccount().getUsername());
                    response.setEmail(m.getAccount().getEmail());
                    response.setFullName(
                            m.getAccount().getDetails() != null
                                    ? m.getAccount().getDetails().getFullName()
                                    : null
                    );
                    response.setRole(m.getAccount().getRole().getName());
                    response.setActive(m.getAccount().isActive());
                    return response;
                })
                .collect(Collectors.toList());

        if (includeLecturer) {
            projectRepository.findById(projectId).ifPresent(project ->
                    accountRepository.findById(project.getLecturerId()).ifPresent(lecturer -> {
                        boolean alreadyIncluded = responses.stream()
                                .anyMatch(response -> lecturer.getId().equals(response.getId()));

                        if (!alreadyIncluded) {
                            AccountResponse lecturerResponse = new AccountResponse();
                            lecturerResponse.setId(lecturer.getId());
                            lecturerResponse.setUsername(lecturer.getUsername());
                            lecturerResponse.setEmail(lecturer.getEmail());
                            lecturerResponse.setFullName(
                                    lecturer.getDetails() != null
                                            ? lecturer.getDetails().getFullName()
                                            : null
                            );
                            lecturerResponse.setRole(lecturer.getRole().getName());
                            lecturerResponse.setActive(lecturer.isActive());
                            responses.add(0, lecturerResponse);
                        }
                    })
            );
        }

        return ResponseEntity.ok(responses);
    }

    //Get list of projects for current logged in member
    @GetMapping("/my-groups")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<List<ProjectResponse>> getMyGroups() {
        String currentUserId = SecurityUtil.getCurrentUserId(accountRepository);
        List<ProjectResponse> responses = groupMemberService.getProjectsByMemberId(currentUserId);
        return ResponseEntity.ok(responses);
    }

    //Get current user's role in a project
    @GetMapping("/{projectId}/my-role")
    public ResponseEntity<ProjectMemberRoleResponse> getMyRoleInProject(
            @PathVariable String projectId
    ) {
        String currentUserId = SecurityUtil.getCurrentUserId(accountRepository);
        ProjectMemberRoleResponse response = groupMemberService.getMemberRoleInProject(projectId, currentUserId);
        return ResponseEntity.ok(response);
    }

    @PreAuthorize("hasAnyRole('ADMIN','LECTURER')")
    @GetMapping("/{projectId}/report/export")
    public ResponseEntity<byte[]> exportProjectMembersReport(
            @PathVariable String projectId,
            @RequestParam(defaultValue = "csv") String format
    ) {
        ProjectReportExportService.ExportedProjectReport report =
                projectReportExportService.exportMembersReport(projectId, format);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + report.filename() + "\"")
                .contentType(MediaType.parseMediaType(report.contentType()))
                .body(report.content());
    }

    private void ensureCanViewProjectMembers(String projectId) {
        String currentUserId = SecurityUtil.getCurrentUserId(accountRepository);

        Account currentAccount = accountRepository.findById(currentUserId)
                .orElseThrow(() -> new NotFoundException("Current account not found"));

        String accountRole = currentAccount.getRole() != null ? currentAccount.getRole().getName() : null;
        if ("ADMIN".equals(accountRole)) {
            return;
        }

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new NotFoundException("Project not found with id: " + projectId));

        if ("LECTURER".equals(accountRole) && currentUserId.equals(project.getLecturerId())) {
            return;
        }

        if ("STUDENT".equals(accountRole) && projectMemberRepository.existsByProjectIdAndAccountId(projectId, currentUserId)) {
            return;
        }

        throw new ForbiddenException("You do not have permission to view members of this group.");
    }
}

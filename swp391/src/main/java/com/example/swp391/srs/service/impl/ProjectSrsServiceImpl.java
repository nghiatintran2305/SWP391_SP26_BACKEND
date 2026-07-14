package com.example.swp391.srs.service.impl;

import com.example.swp391.accounts.entity.Account;
import com.example.swp391.accounts.repository.AccountRepository;
import com.example.swp391.exceptions.ForbiddenException;
import com.example.swp391.exceptions.NotFoundException;
import com.example.swp391.projects.entity.Project;
import com.example.swp391.projects.entity.ProjectMember;
import com.example.swp391.projects.enums.ProjectRole;
import com.example.swp391.projects.repository.ProjectMemberRepository;
import com.example.swp391.projects.repository.ProjectRepository;
import com.example.swp391.srs.dto.request.UpsertProjectSrsRequest;
import com.example.swp391.srs.dto.response.ProjectSrsResponse;
import com.example.swp391.srs.entity.ProjectSrs;
import com.example.swp391.srs.repository.ProjectSrsRepository;
import com.example.swp391.srs.service.IProjectSrsService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Transactional
public class ProjectSrsServiceImpl implements IProjectSrsService {

    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final ProjectSrsRepository projectSrsRepository;
    private final AccountRepository accountRepository;

    @Override
    public ProjectSrsResponse getProjectSrs(String projectId, String currentUserId) {
        AccessContext accessContext = resolveAccess(projectId, currentUserId);
        return projectSrsRepository.findByProjectId(projectId)
                .map(srs -> mapToResponse(srs, accessContext.editable()))
                .orElseGet(() -> ProjectSrsResponse.builder()
                        .exists(false)
                        .editable(accessContext.editable())
                        .projectId(accessContext.project().getId())
                        .projectName(accessContext.project().getProjectName())
                        .build());
    }

    @Override
    public ProjectSrsResponse saveProjectSrs(String projectId, String currentUserId, UpsertProjectSrsRequest request) {
        AccessContext accessContext = resolveAccess(projectId, currentUserId);
        if (!accessContext.editable()) {
            throw new ForbiddenException("Only the group leader can create or edit the SRS document.");
        }

        Account currentAccount = accessContext.account();
        Project project = accessContext.project();

        ProjectSrs srs = projectSrsRepository.findByProjectId(projectId)
                .orElse(ProjectSrs.builder()
                        .project(project)
                        .createdBy(currentAccount)
                        .build());

        srs.setTitle(request.getTitle().trim());
        srs.setContent(request.getContent().trim());

        ProjectSrs saved = projectSrsRepository.save(srs);
        return mapToResponse(saved, true);
    }

    private AccessContext resolveAccess(String projectId, String currentUserId) {
        Account account = accountRepository.findById(currentUserId)
                .orElseThrow(() -> new NotFoundException("Current account not found."));

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new NotFoundException("Project not found with id: " + projectId));

        String accountRole = account.getRole() != null ? account.getRole().getName() : null;

        if ("ADMIN".equals(accountRole)) {
            return new AccessContext(account, project, false);
        }

        if ("LECTURER".equals(accountRole)) {
            if (currentUserId.equals(project.getLecturerId())) {
                return new AccessContext(account, project, false);
            }
            throw new ForbiddenException("You do not have permission to view this SRS document.");
        }

        if (!isStudentScopedRole(accountRole)) {
            throw new ForbiddenException("You do not have permission to view this SRS document.");
        }

        ProjectMember member = projectMemberRepository.findByProjectIdAndAccountId(projectId, currentUserId)
                .orElseThrow(() -> new ForbiddenException("You are not a member of this project."));

        boolean editable = member.getRoleInGroup() == ProjectRole.LEADER;
        return new AccessContext(account, project, editable);
    }

    private ProjectSrsResponse mapToResponse(ProjectSrs srs, boolean editable) {
        Account createdBy = srs.getCreatedBy();
        return ProjectSrsResponse.builder()
                .exists(true)
                .editable(editable)
                .id(srs.getId())
                .projectId(srs.getProject().getId())
                .projectName(srs.getProject().getProjectName())
                .title(srs.getTitle())
                .content(srs.getContent())
                .createdById(createdBy != null ? createdBy.getId() : null)
                .createdByUsername(createdBy != null ? createdBy.getUsername() : null)
                .createdByFullName(createdBy != null && createdBy.getDetails() != null ? createdBy.getDetails().getFullName() : null)
                .createdAt(srs.getCreatedAt())
                .updatedAt(srs.getUpdatedAt())
                .build();
    }

    private boolean isStudentScopedRole(String accountRole) {
        return "STUDENT".equals(accountRole)
                || "LEADER".equals(accountRole)
                || "MEMBER".equals(accountRole);
    }

    private record AccessContext(Account account, Project project, boolean editable) {
    }
}

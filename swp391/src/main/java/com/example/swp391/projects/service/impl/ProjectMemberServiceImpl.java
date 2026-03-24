package com.example.swp391.projects.service.impl;

import com.example.swp391.accounts.entity.Account;
import com.example.swp391.accounts.repository.AccountRepository;
import com.example.swp391.exceptions.BadRequestException;
import com.example.swp391.exceptions.NotFoundException;
import com.example.swp391.github.enums.GithubLinkStatus;
import com.example.swp391.github.repository.GithubUserMappingRepository;
import com.example.swp391.github.service.IGithubService;
import com.example.swp391.jira.enums.JiraLinkStatus;
import com.example.swp391.jira.repository.JiraUserMappingRepository;
import com.example.swp391.jira.service.IJiraService;
import com.example.swp391.projects.dto.response.ProjectMemberRoleResponse;
import com.example.swp391.projects.dto.response.ProjectResponse;
import com.example.swp391.projects.entity.Project;
import com.example.swp391.projects.entity.ProjectMember;
import com.example.swp391.projects.enums.ProjectRole;
import com.example.swp391.projects.repository.ProjectMemberRepository;
import com.example.swp391.projects.repository.ProjectRepository;
import com.example.swp391.projects.service.IProjectMemberService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ProjectMemberServiceImpl implements IProjectMemberService {

    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final AccountRepository accountRepository;

    private final IJiraService jiraService;
    private final IGithubService githubService;

    private final JiraUserMappingRepository jiraUserMappingRepository;
    private final GithubUserMappingRepository githubUserMappingRepository;

    @Override
    public void addMemberToProject(String projectId, String accountId, ProjectRole role) {

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new NotFoundException("Project not found with id: " + projectId));

        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new NotFoundException("Account not found with id: " + accountId));

        boolean exists = projectMemberRepository.existsByProjectIdAndAccountId(projectId, accountId);
        if (exists) {
            throw new BadRequestException("User is already a member of this project");
        }

        // validation rules
        validateMember(project, account, role);

        ProjectMember member = ProjectMember.builder()
                .project(project)
                .account(account)
                .roleInGroup(role)
                .build();

        projectMemberRepository.save(member);

        log.info("User {} added to project {} as {}", accountId, projectId, role);

        //JIRA SYNC
        jiraUserMappingRepository.findByAccountId(accountId)
                .filter(m -> m.getStatus() == JiraLinkStatus.LINKED)
                .ifPresent(mapping -> {

                    String roleId = mapToJiraRole(role);

                    try {
                        jiraService.addUserToProjectRole(
                                project.getJiraProjectKey(),
                                roleId,
                                mapping.getJiraAccountId()
                        );

                        log.info("Jira sync success for user {}", accountId);

                    } catch (Exception e) {
                        log.error("Failed to sync Jira for user {}", accountId, e);
                    }
                });

        //GITHUB SYNC
        githubUserMappingRepository.findByAccountId(accountId)
                .filter(m -> m.getStatus() == GithubLinkStatus.LINKED)
                .ifPresent(mapping -> {

                    try {

                        githubService.addCollaboratorToRepo(
                                project.getGithubRepoName(),
                                mapping.getGithubUsername()
                        );

                        log.info("GitHub repo access granted to {}", mapping.getGithubUsername());

                    } catch (Exception e) {
                        log.error("Failed to sync GitHub for user {}", accountId, e);
                    }
                });
    }

    @Override
    public void removeMemberFromProject(String projectId, String accountId) {

        ProjectMember member = projectMemberRepository
                .findByProjectIdAndAccountId(projectId, accountId)
                .orElseThrow(() -> new NotFoundException("Member not found in project"));

        Project project = member.getProject();

        projectMemberRepository.delete(member);

        log.info("User {} removed from project {}", accountId, projectId);

        //JIRA SYNC
        jiraUserMappingRepository.findByAccountId(accountId)
                .filter(m -> m.getStatus() == JiraLinkStatus.LINKED)
                .ifPresent(mapping -> {

                    String roleId = mapToJiraRole(member.getRoleInGroup());

                    try {

                        jiraService.removeUserFromProjectRole(
                                project.getJiraProjectKey(),
                                roleId,
                                mapping.getJiraAccountId()
                        );

                        log.info("Jira user removed {}", accountId);

                    } catch (Exception e) {
                        log.error("Failed to remove Jira user {}", accountId, e);
                    }
                });

        //GITHUB SYNC
        githubUserMappingRepository.findByAccountId(accountId)
                .filter(m -> m.getStatus() == GithubLinkStatus.LINKED)
                .ifPresent(mapping -> {

                    try {

                        githubService.removeCollaboratorFromRepo(
                                project.getGithubRepoName(),
                                mapping.getGithubUsername()
                        );

                        log.info("GitHub repo access removed for {}", mapping.getGithubUsername());

                    } catch (Exception e) {
                        log.error("Failed to remove GitHub collaborator {}", accountId, e);
                    }
                });
    }

    private String mapToJiraRole(ProjectRole role) {

        return switch (role) {

            case LEADER -> "10002";
            case MEMBER -> "10003";

        };
    }

    private void validateMember(Project project, Account account, ProjectRole role) {

        String accountRole = account.getRole().getName();

        // Rule 1: only student can be leader or member
        if ((role == ProjectRole.LEADER || role == ProjectRole.MEMBER)
                && !"STUDENT".equals(accountRole)) {

            throw new BadRequestException("Only student can be leader or member");
        }


        // Rule 2: each project can only have 1 leader
        if (role == ProjectRole.LEADER &&
                projectMemberRepository.existsByProjectIdAndRoleInGroup(
                        project.getId(),
                        ProjectRole.LEADER)) {

            throw new BadRequestException("Project already has a leader");
        }

        // Rule 5: student can only belong to 1 project
        if ("STUDENT".equals(accountRole) &&
                projectMemberRepository.existsByAccountId(account.getId())) {

            throw new BadRequestException("Student already belongs to another project");
        }
    }

    @Override
    public List<ProjectResponse> getProjectsByMemberId(String accountId) {
        List<ProjectMember> members = projectMemberRepository.findByAccountId(accountId);
        return members.stream()
                .map(member -> mapToResponse(member.getProject()))
                .collect(Collectors.toList());
    }

    private ProjectResponse mapToResponse(Project project) {
        ProjectResponse res = new ProjectResponse();
        res.setId(project.getId());
        res.setProjectName(project.getProjectName());
        res.setJiraProjectId(project.getJiraProjectId());
        res.setJiraProjectKey(project.getJiraProjectKey());
        res.setGithubRepoName(project.getGithubRepoName());
        res.setGithubRepoUrl(project.getGithubRepoUrl());
        res.setStatus(project.getStatus());
        return res;
    }

    @Override
    public ProjectMemberRoleResponse getMemberRoleInProject(String projectId, String accountId) {
        Optional<ProjectMember> member = projectMemberRepository.findByProjectIdAndAccountId(projectId, accountId);
        
        return ProjectMemberRoleResponse.builder()
                .projectId(projectId)
                .role(member.map(m -> m.getRoleInGroup().name()).orElse(null))
                .build();
    }
}
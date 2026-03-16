package com.example.swp391.projects.service.impl;

import com.example.swp391.accounts.entity.Account;
import com.example.swp391.accounts.repository.AccountRepository;
import com.example.swp391.configs.security.SecurityUtil;
import com.example.swp391.exceptions.BadRequestException;
import com.example.swp391.exceptions.NotFoundException;
import com.example.swp391.github.dto.response.GithubRepoResponse;
import com.example.swp391.github.entity.GithubUserMapping;
import com.example.swp391.github.repository.GithubUserMappingRepository;
import com.example.swp391.github.service.IGithubService;
import com.example.swp391.jira.dto.response.JiraProjectResponse;
import com.example.swp391.jira.entity.JiraUserMapping;
import com.example.swp391.jira.repository.JiraUserMappingRepository;
import com.example.swp391.jira.service.IJiraService;
import com.example.swp391.projects.dto.request.CreateProjectRequest;
import com.example.swp391.projects.dto.response.ProjectResponse;
import com.example.swp391.projects.entity.Project;
import com.example.swp391.projects.entity.ProjectMember;
import com.example.swp391.projects.enums.ProjectStatus;
import com.example.swp391.projects.repository.ProjectMemberRepository;
import com.example.swp391.projects.repository.ProjectRepository;
import com.example.swp391.projects.service.IProjectService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class ProjectServiceImpl implements IProjectService {

    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final AccountRepository accountRepository;
    private final JiraUserMappingRepository jiraUserMappingRepository;
    private final GithubUserMappingRepository githubUserMappingRepository;
    private final IJiraService jiraService;
    private final IGithubService githubService;

    @Override
    public ProjectResponse createProject(CreateProjectRequest req) {

        Account admin = new SecurityUtil(accountRepository).getCurrentAccount();

        Account lecturer = accountRepository.findById(req.getLecturerAccountId())
                .orElseThrow(() -> new NotFoundException("Lecturer not found with id: " + req.getLecturerAccountId()));

        // Validate lecturer role
        if (!"LECTURER".equals(lecturer.getRole().getName())) {
            throw new BadRequestException("Account is not a lecturer");
        }

        JiraUserMapping jiraUser = jiraUserMappingRepository
                .findByAccountId(lecturer.getId())
                .orElseThrow(() -> new BadRequestException("Lecturer has not linked Jira account"));

        GithubUserMapping githubUser = githubUserMappingRepository
                .findByAccountId(lecturer.getId())
                .orElseThrow(() -> new BadRequestException("Lecturer has not linked GitHub account"));

        String jiraKey = generateJiraKey(req.getProjectName());
        String repoName = generateRepoName(req.getProjectName());

        String jiraProjectId;
        String createdRepo = null;

        try {

            // CREATE JIRA PROJECT
            JiraProjectResponse jiraProject = jiraService.createProject(
                    jiraKey,
                    req.getProjectName(),
                    jiraUser.getJiraAccountId()
            );

            jiraProjectId = jiraProject.getId();

            // CREATE GITHUB REPO
            GithubRepoResponse repo = githubService.createRepo(repoName);

            createdRepo = repo.getName();
            String repoUrl = repo.getHtml_url();

            // ADD LECTURER INTO REPO
            githubService.addCollaboratorToRepo(
                    createdRepo,
                    githubUser.getGithubUsername()
            );

            // SAVE DB
            Project project = Project.builder()
                    .projectName(req.getProjectName())
                    .createdBy(admin)
                    .lecturerId(req.getLecturerAccountId())
                    .jiraProjectId(jiraProjectId)
                    .jiraProjectKey(jiraKey)
                    .githubRepoName(createdRepo)
                    .githubRepoUrl(repoUrl)
                    .status(ProjectStatus.CONFIGURED)
                    .build();

            projectRepository.save(project);

            return mapToResponse(project);

        } catch (Exception ex) {

            // rollback Jira
            jiraService.deleteProjectQuietly(jiraKey);

            // rollback GitHub
            if (createdRepo != null) {
                githubService.deleteRepoQuietly(createdRepo);
            }

            throw ex;
        }
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

    private String generateJiraKey(String name) {

        String key = name
                .replaceAll("[^A-Za-z0-9]", "")
                .toUpperCase();

        if (key.isEmpty()) {
            key = "PROJECT";
        }

        if (!Character.isLetter(key.charAt(0))) {
            key = "P" + key;
        }

        if (key.length() > 10) {
            key = key.substring(0, 10);
        }

        return key;
    }

    private String generateRepoName(String name) {

        return name
                .toLowerCase()
                .replaceAll("[^a-z0-9]", "-")
                .replaceAll("-+", "-");
    }

    @Override
    public ProjectResponse getProjectById(String id) {
        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Project not found with id: " + id));
        return mapToResponse(project);
    }

    @Override
    public List<ProjectResponse> getAllProjects() {
        return projectRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

//    @Override
//    public List<ProjectResponse> getProjectsByStatus(ProjectStatus status) {
//        return projectRepository.findAll().stream()
//                .filter(p -> p.getStatus() == status)
//                .map(this::mapToResponse)
//                .collect(Collectors.toList());
//    }

    @Override
    public ProjectResponse updateProject(String id, CreateProjectRequest req) {
        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Project not found with id: " + id));

        if (req.getProjectName() != null && !req.getProjectName().isEmpty()) {
            project.setProjectName(req.getProjectName());
        }

        Project saved = projectRepository.save(project);
        return mapToResponse(saved);
    }

    @Override
    public void deleteProject(String id) {
        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Project not found with id: " + id));

        // Delete from Jira and GitHub
        if (project.getJiraProjectKey() != null) {
            jiraService.deleteProjectQuietly(project.getJiraProjectKey());
        }
        if (project.getGithubRepoName() != null) {
            githubService.deleteRepoQuietly(project.getGithubRepoName());
        }

        projectRepository.delete(project);
    }

    @Override
    public ProjectResponse updateProjectStatus(String id, ProjectStatus status) {
        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Project not found with id: " + id));

        if (status == null) {
            throw new BadRequestException("Status is required");
        }

        project.setStatus(status);
        Project saved = projectRepository.save(project);
        return mapToResponse(saved);
    }

    @Override
    public List<ProjectResponse> getProjectsByLecturerId(String lecturerId) {
        List<ProjectMember> members = projectMemberRepository.findByAccountId(lecturerId);
        return members.stream()
                .map(member -> mapToResponse(member.getProject()))
                .collect(Collectors.toList());
    }
}
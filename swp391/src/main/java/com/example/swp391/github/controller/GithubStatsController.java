package com.example.swp391.github.controller;

import com.example.swp391.accounts.repository.AccountRepository;
import com.example.swp391.configs.security.SecurityUtil;
import com.example.swp391.exceptions.BadRequestException;
import com.example.swp391.exceptions.NotFoundException;
import com.example.swp391.github.dto.response.CommitDetail;
import com.example.swp391.github.dto.response.CommitStats;
import com.example.swp391.github.dto.response.CommitSummary;
import com.example.swp391.github.entity.GithubUserMapping;
import com.example.swp391.projects.entity.Project;
import com.example.swp391.projects.repository.ProjectRepository;
import com.example.swp391.github.service.IGithubService;
import com.example.swp391.github.repository.GithubUserMappingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class GithubStatsController {

    private final IGithubService githubService;
    private final GithubUserMappingRepository githubUserMappingRepository;
    private final ProjectRepository projectRepository;
    private final AccountRepository accountRepository;

    private Project getProjectOrThrow(String projectId) {
        return projectRepository.findById(projectId)
                .orElseThrow(() -> new NotFoundException("Project not found with id: " + projectId));
    }

    private String getProjectRepoName(String projectId) {
        Project project = getProjectOrThrow(projectId);
        if (project.getGithubRepoName() == null || project.getGithubRepoName().isBlank()) {
            throw new BadRequestException("Project does not have a GitHub repository configured");
        }
        return project.getGithubRepoName();
    }

    //Repo statistics (Admin/Lecturer)

    @PreAuthorize("hasAnyRole('ADMIN', 'LECTURER')")
    @GetMapping("/projects/{projectId}/github/stats")
    public ResponseEntity<CommitStats> getRepoStats(@PathVariable String projectId) {
        // Get project by ID and extract repo name
        // This is a simplified version - you would need to inject ProjectRepository
        return ResponseEntity.ok().build();
    }

    //Team total commits (Admin/Lecturer/Leader)

    @PreAuthorize("hasAnyRole('ADMIN', 'LECTURER', 'STUDENT')")
    @GetMapping("/projects/{projectId}/github/commits/team")
    public ResponseEntity<List<CommitSummary>> getTeamCommitSummary(
            @PathVariable String projectId
    ) {
        String repoName = getProjectRepoName(projectId);
        List<CommitSummary> summaries = githubService.getTeamCommitSummary(repoName);
        return ResponseEntity.ok(summaries);
    }

    //User commit statistics

    @GetMapping("/users/me/github/commits")
    public ResponseEntity<CommitStats> getMyCommitStats(
            @RequestParam String repoName,
            @RequestParam String githubUsername
    ) {
        CommitStats stats = githubService.getUserCommitStats(repoName, githubUsername);
        return ResponseEntity.ok(stats);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'LECTURER', 'STUDENT')")
    @GetMapping("/projects/{projectId}/github/commits/me")
    public ResponseEntity<CommitStats> getMyProjectCommitStats(@PathVariable String projectId) {
        String currentUserId = SecurityUtil.getCurrentUserId(accountRepository);
        GithubUserMapping mapping = githubUserMappingRepository.findByAccountId(currentUserId)
                .orElseThrow(() -> new BadRequestException("Current account has not linked GitHub"));
        String repoName = getProjectRepoName(projectId);
        CommitStats stats = githubService.getUserCommitStats(repoName, mapping.getGithubUsername());
        return ResponseEntity.ok(stats);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'LECTURER', 'LEADER')")
    @GetMapping("/projects/{projectId}/github/commits/user/{username}")
    public ResponseEntity<CommitStats> getUserCommitStats(
            @PathVariable String projectId,
            @PathVariable String username,
            @RequestParam(required = false) String repoName
    ) {
        String resolvedRepoName = (repoName == null || repoName.isBlank()) ? getProjectRepoName(projectId) : repoName;
        CommitStats stats = githubService.getUserCommitStats(resolvedRepoName, username);
        return ResponseEntity.ok(stats);
    }

    //All Team commits

    @PreAuthorize("hasAnyRole('ADMIN', 'LECTURER', 'LEADER')")
    @GetMapping("/projects/{projectId}/github/commits/all")
    public ResponseEntity<List<CommitSummary>> getAllTeamCommits(
            @PathVariable String projectId,
            @RequestParam(required = false) String repoName
    ) {
        String resolvedRepoName = (repoName == null || repoName.isBlank()) ? getProjectRepoName(projectId) : repoName;
        List<CommitSummary> summaries = githubService.getTeamCommitSummary(resolvedRepoName);
        return ResponseEntity.ok(summaries);
    }

    //User commits detail

    @PreAuthorize("hasAnyRole('ADMIN', 'LECTURER', 'LEADER')")
    @GetMapping("/projects/{projectId}/github/commits/user/{username}/detail")
    public ResponseEntity<List<CommitDetail>> getUserCommitsDetail(
            @PathVariable String projectId,
            @PathVariable String username,
            @RequestParam(required = false) String repoName,
            @RequestParam(defaultValue = "30") int perPage,
            @RequestParam(defaultValue = "1") int page
    ) {
        String resolvedRepoName = (repoName == null || repoName.isBlank()) ? getProjectRepoName(projectId) : repoName;
        List<CommitDetail> commits = githubService.getUserCommits(resolvedRepoName, username, perPage, page);
        return ResponseEntity.ok(commits);
    }

    //My commits detail

    @GetMapping("/users/me/github/commits/detail")
    public ResponseEntity<List<CommitDetail>> getMyCommitsDetail(
            @RequestParam String repoName,
            @RequestParam String githubUsername,
            @RequestParam(defaultValue = "30") int perPage,
            @RequestParam(defaultValue = "1") int page
    ) {
        List<CommitDetail> commits = githubService.getUserCommits(repoName, githubUsername, perPage, page);
        return ResponseEntity.ok(commits);
    }
}

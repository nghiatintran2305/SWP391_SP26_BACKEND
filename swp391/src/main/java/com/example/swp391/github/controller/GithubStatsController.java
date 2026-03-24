package com.example.swp391.github.controller;

import com.example.swp391.github.dto.response.CommitDetail;
import com.example.swp391.github.dto.response.CommitStats;
import com.example.swp391.github.dto.response.CommitSummary;
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
            @RequestParam String repoName
    ) {
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

    @PreAuthorize("hasAnyRole('ADMIN', 'LECTURER', 'LEADER')")
    @GetMapping("/projects/{projectId}/github/commits/user/{username}")
    public ResponseEntity<CommitStats> getUserCommitStats(
            @PathVariable String projectId,
            @PathVariable String username,
            @RequestParam String repoName
    ) {
        CommitStats stats = githubService.getUserCommitStats(repoName, username);
        return ResponseEntity.ok(stats);
    }

    //All Team commits

    @PreAuthorize("hasAnyRole('ADMIN', 'LECTURER', 'LEADER')")
    @GetMapping("/projects/{projectId}/github/commits/all")
    public ResponseEntity<List<CommitSummary>> getAllTeamCommits(
            @RequestParam String repoName
    ) {
        List<CommitSummary> summaries = githubService.getTeamCommitSummary(repoName);
        return ResponseEntity.ok(summaries);
    }

    //User commits detail

    @PreAuthorize("hasAnyRole('ADMIN', 'LECTURER', 'LEADER')")
    @GetMapping("/projects/{projectId}/github/commits/user/{username}/detail")
    public ResponseEntity<List<CommitDetail>> getUserCommitsDetail(
            @PathVariable String projectId,
            @PathVariable String username,
            @RequestParam String repoName,
            @RequestParam(defaultValue = "30") int perPage,
            @RequestParam(defaultValue = "1") int page
    ) {
        List<CommitDetail> commits = githubService.getUserCommits(repoName, username, perPage, page);
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

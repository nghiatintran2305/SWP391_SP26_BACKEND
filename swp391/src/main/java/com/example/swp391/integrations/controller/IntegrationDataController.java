package com.example.swp391.integrations.controller;

import com.example.swp391.accounts.entity.Account;
import com.example.swp391.accounts.repository.AccountRepository;
import com.example.swp391.config.security.SecurityUtil;
import com.example.swp391.integrations.dto.response.GitHubCommitResponse;
import com.example.swp391.integrations.dto.response.JiraIssueResponse;
import com.example.swp391.integrations.dto.response.JiraStatusStatsResponse;
import com.example.swp391.integrations.dto.response.MyGitHubCommitsResponse;
import com.example.swp391.integrations.dto.response.MyJiraIssuesResponse;
import com.example.swp391.integrations.entity.GitHubCommit;
import com.example.swp391.integrations.entity.IdentityMapping;
import com.example.swp391.integrations.entity.JiraIssue;
import com.example.swp391.integrations.repository.GitHubCommitRepository;
import com.example.swp391.integrations.repository.IdentityMappingRepository;
import com.example.swp391.integrations.repository.JiraIssueRepository;
import com.example.swp391.projects.service.GroupAccessService;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Read-only endpoints to expose cached integration data (Jira/GitHub) to FE.
 *
 * IMPORTANT: These endpoints do NOT write back to Jira/GitHub (out-of-scope).
 * They only read from local DB cache that was created by the Sync process.
 */
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class IntegrationDataController {

    private final JiraIssueRepository jiraIssueRepository;
    private final GitHubCommitRepository gitHubCommitRepository;
    private final IdentityMappingRepository identityMappingRepository;
    private final AccountRepository accountRepository;
    private final GroupAccessService groupAccessService;

    // -----------------------------
    // Jira issues
    // -----------------------------

    /**
     * List cached Jira issues for a group.
     *
     * Query params (optional):
     * - status
     * - sprintId
     * - issueType
     */
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/jira/issues/group/{groupId}")
    public ResponseEntity<List<JiraIssueResponse>> getJiraIssuesByGroup(
            @PathVariable String groupId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String sprintId,
            @RequestParam(required = false) String issueType
    ) {
        Account current = new SecurityUtil(accountRepository).getCurrentAccount();
        groupAccessService.assertCanReadGroup(groupId, current);

        List<JiraIssue> issues = jiraIssueRepository.findByGroupId(groupId);
        List<JiraIssueResponse> res = issues.stream()
                .filter(i -> status == null || (i.getStatus() != null && i.getStatus().equalsIgnoreCase(status)))
                .filter(i -> sprintId == null || (i.getSprintId() != null && i.getSprintId().equalsIgnoreCase(sprintId)))
                .filter(i -> issueType == null || (i.getIssueType() != null && i.getIssueType().equalsIgnoreCase(issueType)))
                .map(this::mapToJiraIssueResponse)
                .sorted(Comparator.comparing(JiraIssueResponse::getUpdatedAt, Comparator.nullsLast(Comparator.naturalOrder())).reversed())
                .collect(Collectors.toList());

        return ResponseEntity.ok(res);
    }

    /**
     * List cached Jira issues assigned to the current user.
     *
     * How it works:
     * - We look up IdentityMapping for current account.
     * - If mapping.jiraAccountId is present, we query JiraIssue.assigneeAccountId.
     */
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/jira/issues/me")
    public ResponseEntity<MyJiraIssuesResponse> getMyJiraIssues(
            @RequestParam(required = false) String groupId
    ) {
        Account current = new SecurityUtil(accountRepository).getCurrentAccount();

        Optional<IdentityMapping> mappingOpt = identityMappingRepository.findByAccountId(current.getId());
        MyJiraIssuesResponse wrapper = new MyJiraIssuesResponse();

        if (mappingOpt.isEmpty() || mappingOpt.get().getJiraAccountId() == null || mappingOpt.get().getJiraAccountId().isBlank()) {
            wrapper.setMapped(false);
            wrapper.setJiraAccountId(null);
            wrapper.setIssues(List.of());
            return ResponseEntity.ok(wrapper);
        }

        String jiraAccountId = mappingOpt.get().getJiraAccountId();
        wrapper.setMapped(true);
        wrapper.setJiraAccountId(jiraAccountId);

        List<JiraIssue> issues;
        if (groupId != null && !groupId.isBlank()) {
            // Ensure user can read this group.
            groupAccessService.assertCanReadGroup(groupId, current);
            issues = jiraIssueRepository.findByGroupIdAndAssigneeAccountId(groupId, jiraAccountId);
        } else {
            issues = jiraIssueRepository.findByAssigneeAccountId(jiraAccountId);
        }

        wrapper.setIssues(
                issues.stream()
                        .map(this::mapToJiraIssueResponse)
                        .sorted(Comparator.comparing(JiraIssueResponse::getUpdatedAt, Comparator.nullsLast(Comparator.naturalOrder())).reversed())
                        .collect(Collectors.toList())
        );

        return ResponseEntity.ok(wrapper);
    }

    /**
     * Simple status breakdown for a group (progress dashboard).
     */
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/jira/issues/group/{groupId}/stats/status")
    public ResponseEntity<JiraStatusStatsResponse> getJiraStatusStats(@PathVariable String groupId) {
        Account current = new SecurityUtil(accountRepository).getCurrentAccount();
        groupAccessService.assertCanReadGroup(groupId, current);

        List<JiraIssue> issues = jiraIssueRepository.findByGroupId(groupId);
        Map<String, Long> countByStatus = new HashMap<>();
        for (JiraIssue issue : issues) {
            String status = issue.getStatus() != null ? issue.getStatus() : "UNKNOWN";
            countByStatus.put(status, countByStatus.getOrDefault(status, 0L) + 1);
        }

        JiraStatusStatsResponse res = new JiraStatusStatsResponse();
        res.setGroupId(groupId);
        res.setCountByStatus(countByStatus);
        return ResponseEntity.ok(res);
    }

    // -----------------------------
    // GitHub commits
    // -----------------------------

    /**
     * List cached GitHub commits for a group.
     */
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/github/commits/group/{groupId}")
    public ResponseEntity<List<GitHubCommitResponse>> getCommitsByGroup(
            @PathVariable String groupId,
            @RequestParam(required = false) String authorEmail
    ) {
        Account current = new SecurityUtil(accountRepository).getCurrentAccount();
        groupAccessService.assertCanReadGroup(groupId, current);

        List<GitHubCommit> commits = (authorEmail == null || authorEmail.isBlank())
                ? gitHubCommitRepository.findByRepository_Group_Id(groupId)
                : gitHubCommitRepository.findByRepository_Group_IdAndAuthorEmailIgnoreCase(groupId, authorEmail);

        List<GitHubCommitResponse> res = commits.stream()
                .map(this::mapToCommitResponse)
                .sorted(Comparator.comparing(GitHubCommitResponse::getCommittedAt, Comparator.nullsLast(Comparator.naturalOrder())).reversed())
                .collect(Collectors.toList());

        return ResponseEntity.ok(res);
    }

    /**
     * List cached GitHub commits for the current user.
     *
     * How it works:
     * - We look up IdentityMapping.githubEmail for current account.
     * - If githubEmail exists, we query commits.authorEmail.
     */
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/github/commits/me")
    public ResponseEntity<MyGitHubCommitsResponse> getMyCommits(
            @RequestParam(required = false) String groupId
    ) {
        Account current = new SecurityUtil(accountRepository).getCurrentAccount();

        Optional<IdentityMapping> mappingOpt = identityMappingRepository.findByAccountId(current.getId());
        MyGitHubCommitsResponse wrapper = new MyGitHubCommitsResponse();

        if (mappingOpt.isEmpty() || mappingOpt.get().getGithubEmail() == null || mappingOpt.get().getGithubEmail().isBlank()) {
            wrapper.setMapped(false);
            wrapper.setGithubEmail(null);
            wrapper.setCommits(List.of());
            return ResponseEntity.ok(wrapper);
        }

        String githubEmail = mappingOpt.get().getGithubEmail();
        wrapper.setMapped(true);
        wrapper.setGithubEmail(githubEmail);

        List<GitHubCommit> commits;
        if (groupId != null && !groupId.isBlank()) {
            groupAccessService.assertCanReadGroup(groupId, current);
            commits = gitHubCommitRepository.findByRepository_Group_IdAndAuthorEmailIgnoreCase(groupId, githubEmail);
        } else {
            commits = gitHubCommitRepository.findByAuthorEmailIgnoreCase(githubEmail);
        }

        wrapper.setCommits(
                commits.stream()
                        .map(this::mapToCommitResponse)
                        .sorted(Comparator.comparing(GitHubCommitResponse::getCommittedAt, Comparator.nullsLast(Comparator.naturalOrder())).reversed())
                        .collect(Collectors.toList())
        );

        return ResponseEntity.ok(wrapper);
    }

    // -----------------------------
    // Mapping helpers
    // -----------------------------

    private JiraIssueResponse mapToJiraIssueResponse(JiraIssue issue) {
        JiraIssueResponse res = new JiraIssueResponse();
        res.setId(issue.getId());
        res.setGroupId(issue.getGroup().getId());
        res.setJiraIssueKey(issue.getJiraIssueKey());
        res.setIssueType(issue.getIssueType());
        res.setSummary(issue.getSummary());
        res.setDescription(issue.getDescription());
        res.setStatus(issue.getStatus());
        res.setSprintId(issue.getSprintId());
        res.setSprintName(issue.getSprintName());
        res.setAssigneeJiraAccountId(issue.getAssigneeAccountId());
        res.setCreatedAt(issue.getCreatedAt());
        res.setUpdatedAt(issue.getUpdatedAt());

        // Try mapping assignee Jira account -> local account.
        if (issue.getAssigneeAccountId() != null && !issue.getAssigneeAccountId().isBlank()) {
            identityMappingRepository.findByJiraAccountId(issue.getAssigneeAccountId())
                    .ifPresent(m -> {
                        res.setMappedAccountId(m.getAccount().getId());
                        res.setMappedUsername(m.getAccount().getUsername());
                    });
        }
        return res;
    }

    private GitHubCommitResponse mapToCommitResponse(GitHubCommit commit) {
        GitHubCommitResponse res = new GitHubCommitResponse();
        res.setId(commit.getId());
        res.setGroupId(commit.getRepository().getGroup().getId());
        res.setRepositoryId(commit.getRepository().getId());
        res.setSha(commit.getSha());
        res.setAuthorName(commit.getAuthorName());
        res.setAuthorEmail(commit.getAuthorEmail());
        res.setCommittedAt(commit.getCommittedAt());
        res.setMessage(commit.getMessage());

        // Try mapping commit author email -> local account.
        if (commit.getAuthorEmail() != null && !commit.getAuthorEmail().isBlank()) {
            identityMappingRepository.findByGithubEmailIgnoreCase(commit.getAuthorEmail())
                    .ifPresent(m -> {
                        res.setMappedAccountId(m.getAccount().getId());
                        res.setMappedUsername(m.getAccount().getUsername());
                    });
        }
        return res;
    }
}

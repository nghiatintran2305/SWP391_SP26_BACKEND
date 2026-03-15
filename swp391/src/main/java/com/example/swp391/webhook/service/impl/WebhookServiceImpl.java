package com.example.swp391.webhook.service.impl;

import com.example.swp391.jira.service.IJiraService;
import com.example.swp391.projects.entity.Project;
import com.example.swp391.projects.repository.ProjectRepository;
import com.example.swp391.tasks.entity.Task;
import com.example.swp391.tasks.repository.TaskRepository;
import com.example.swp391.webhook.dto.GithubPushEvent;
import com.example.swp391.webhook.service.IWebhookService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
public class WebhookServiceImpl implements IWebhookService {

    private final ProjectRepository projectRepository;
    private final TaskRepository taskRepository;
    private final IJiraService jiraService;

    // Pattern to match Jira issue keys (e.g., PROJ-123, ABC-1)
    private static final Pattern JIRA_ISSUE_PATTERN = Pattern.compile("([A-Z]+-\\d+)");

    // Pattern to match time spent (e.g., #time 2h, #time 30m)
    private static final Pattern TIME_PATTERN = Pattern.compile("#time\\s+(\\d+)([hm])", Pattern.CASE_INSENSITIVE);

    @Override
    public void handleGithubPush(GithubPushEvent event) {
        if (event == null || event.getCommits() == null || event.getCommits().isEmpty()) {
            log.info("No commits in push event");
            return;
        }

        String repoName = event.getRepository() != null ? event.getRepository().getName() : null;
        log.info("Processing {} commits from repository: {}", event.getCommits().size(), repoName);

        // Find project by GitHub repo name
        Optional<Project> projectOpt = projectRepository.findByGithubRepoName(repoName);
        if (projectOpt.isEmpty()) {
            log.warn("No project found for repository: {}", repoName);
            return;
        }

        Project project = projectOpt.get();
        log.info("Found project: {} for repository: {}", project.getProjectName(), repoName);

        // Track which issues have been processed to avoid duplicates
        Set<String> processedIssues = new HashSet<>();

        for (GithubPushEvent.Commit commit : event.getCommits()) {
            processCommit(commit, project, processedIssues);
        }
    }

    private void processCommit(GithubPushEvent.Commit commit, Project project, Set<String> processedIssues) {
        String commitMessage = commit.getMessage();
        String authorName = commit.getAuthor() != null ? commit.getAuthor().getName() : "Unknown";
        String commitUrl = commit.getUrl();

        log.info("Processing commit: {} by {}", commit.getId().substring(0, 7), authorName);

        // Find all Jira issue keys in commit message
        Matcher matcher = JIRA_ISSUE_PATTERN.matcher(commitMessage);
        
        while (matcher.find()) {
            String issueKey = matcher.group(1);

            // Skip if already processed this push
            if (processedIssues.contains(issueKey)) {
                continue;
            }

            // Check if this issue belongs to the project
            if (!issueKey.startsWith(project.getJiraProjectKey())) {
                log.debug("Issue {} doesn't belong to project {}", issueKey, project.getJiraProjectKey());
                continue;
            }

            processedIssues.add(issueKey);

            // Find task in our DB
            Optional<Task> taskOpt = taskRepository.findByJiraIssueKey(issueKey);
            if (taskOpt.isEmpty()) {
                log.debug("No task found for Jira issue: {}", issueKey);
                // Still try to add comment to Jira directly
                addCommentToJira(issueKey, commitMessage, authorName, commitUrl);
                continue;
            }

            Task task = taskOpt.get();

            // Build comment text
            String comment = buildCommitComment(commit, task);

            try {
                // Add comment to Jira issue
                jiraService.addComment(issueKey, comment);
                log.info("Added commit comment to Jira issue: {}", issueKey);

                // Check for time tracking
                processTimeTracking(commitMessage, issueKey);

            } catch (Exception e) {
                log.error("Failed to add comment to Jira issue: {}", issueKey, e);
            }
        }
    }

    private void addCommentToJira(String issueKey, String commitMessage, String authorName, String commitUrl) {
        try {
            String comment = String.format(
                    "Commited by %s:\n%s\n\n[View Commit](%s)",
                    authorName,
                    commitMessage,
                    commitUrl
            );
            jiraService.addComment(issueKey, comment);
            log.info("Added commit comment directly to Jira issue: {}", issueKey);
        } catch (Exception e) {
            log.error("Failed to add comment to Jira issue: {}", issueKey, e);
        }
    }

    private String buildCommitComment(GithubPushEvent.Commit commit, Task task) {
        String authorName = commit.getAuthor() != null ? commit.getAuthor().getName() : "Unknown";
        String authorUsername = commit.getAuthor() != null ? commit.getAuthor().getUsername() : "";
        String commitUrl = commit.getUrl();
        String shortId = commit.getId().substring(0, 7);

        StringBuilder comment = new StringBuilder();
        comment.append("h3. Git Commit\n");
        comment.append("*Author:* ").append(authorName);
        if (!authorUsername.isEmpty()) {
            comment.append(" (@").append(authorUsername).append(")");
        }
        comment.append("\n");
        comment.append("*Commit:* [").append(shortId).append("|").append(commitUrl).append("]\n");
        comment.append("\n");
        comment.append("{panel:title=Commit Message|borderStyle=dashed|borderColor=#ccc|titleBGColor=#F7D6C1|bgColor=#FFFFCE}\n");
        comment.append(commit.getMessage());
        comment.append("\n{panel}\n");

        // Add file changes summary
        if (commit.getAdded() != null && !commit.getAdded().isEmpty()) {
            comment.append("\n*Files added:* ").append(commit.getAdded().size());
        }
        if (commit.getModified() != null && !commit.getModified().isEmpty()) {
            comment.append("\n*Files modified:* ").append(commit.getModified().size());
        }
        if (commit.getRemoved() != null && !commit.getRemoved().isEmpty()) {
            comment.append("\n*Files removed:* ").append(commit.getRemoved().size());
        }

        return comment.toString();
    }

    private void processTimeTracking(String commitMessage, String issueKey) {
        Matcher timeMatcher = TIME_PATTERN.matcher(commitMessage);
        
        while (timeMatcher.find()) {
            int amount = Integer.parseInt(timeMatcher.group(1));
            String unit = timeMatcher.group(2).toLowerCase();

            try {
                // Note: Jira API for logging work is more complex
                // This would require additional Jira API implementation
                log.info("Time tracking found: {} {} for issue {} (implementation pending)",
                        amount, unit, issueKey);
            } catch (Exception e) {
                log.error("Failed to log time for issue: {}", issueKey, e);
            }
        }
    }
}

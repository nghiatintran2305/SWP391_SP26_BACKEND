package com.example.swp391.integrations.service;

import com.example.swp391.exception.BadRequestException;
import com.example.swp391.integrations.entity.GitHubCommit;
import com.example.swp391.integrations.entity.GitHubRepository;
import com.example.swp391.integrations.entity.IntegrationConfig;
import com.example.swp391.integrations.entity.JiraIssue;
import com.example.swp391.integrations.entity.JiraProject;
import com.example.swp391.integrations.entity.JiraSprint;
import com.example.swp391.integrations.repository.GitHubCommitRepository;
import com.example.swp391.integrations.repository.GitHubRepositoryRepository;
import com.example.swp391.integrations.repository.JiraIssueRepository;
import com.example.swp391.integrations.repository.JiraProjectRepository;
import com.example.swp391.integrations.repository.JiraSprintRepository;
import com.example.swp391.projects.entity.ProjectGroup;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.util.Iterator;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
@RequiredArgsConstructor
public class IntegrationSyncService {

    private final JiraProjectRepository jiraProjectRepository;
    private final JiraSprintRepository jiraSprintRepository;
    private final JiraIssueRepository jiraIssueRepository;
    private final GitHubRepositoryRepository gitHubRepositoryRepository;
    private final GitHubCommitRepository gitHubCommitRepository;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public SyncSummary syncJira(IntegrationConfig config) {
        ProjectGroup group = config.getGroup();
        JiraProject project = jiraProjectRepository
                .findByGroupIdAndProjectKey(group.getId(), config.getJiraProjectKey())
                .orElseGet(() -> JiraProject.builder()
                        .group(group)
                        .projectKey(config.getJiraProjectKey())
                        .build());

        project.setProjectName(fetchJiraProjectName(config));
        jiraProjectRepository.save(project);

        int sprintCount = syncJiraSprints(config, group);
        int issueCount = syncJiraIssues(config, group);

        return new SyncSummary(issueCount, sprintCount, 0);
    }

    public SyncSummary syncGithub(IntegrationConfig config) {
        ProjectGroup group = config.getGroup();
        GitHubRepository repository = gitHubRepositoryRepository
                .findByGroupIdAndOwnerAndName(group.getId(), config.getGithubOwner(), config.getGithubRepo())
                .orElseGet(() -> GitHubRepository.builder()
                        .group(group)
                        .owner(config.getGithubOwner())
                        .name(config.getGithubRepo())
                        .build());

        GitHubRepository savedRepo = gitHubRepositoryRepository.save(repository);

        String url = String.format("https://api.github.com/repos/%s/%s/commits", config.getGithubOwner(), config.getGithubRepo());
        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(createGithubHeaders(config)), String.class);

        int commitCount = 0;
        try {
            JsonNode root = objectMapper.readTree(response.getBody());
            for (JsonNode commitNode : root) {
                String sha = commitNode.path("sha").asText(null);
                if (sha == null) {
                    continue;
                }
                GitHubCommit commit = gitHubCommitRepository
                        .findByRepositoryIdAndSha(savedRepo.getId(), sha)
                        .orElseGet(() -> GitHubCommit.builder()
                                .repository(savedRepo)
                                .sha(sha)
                                .build());

                JsonNode commitDetail = commitNode.path("commit");
                commit.setAuthorName(commitDetail.path("author").path("name").asText(null));
                commit.setAuthorEmail(commitDetail.path("author").path("email").asText(null));
                commit.setCommittedAt(parseDate(commitDetail.path("author").path("date").asText(null)));
                commit.setMessage(commitDetail.path("message").asText(null));

                gitHubCommitRepository.save(commit);
                commitCount++;
            }
        } catch (IOException e) {
            throw new BadRequestException("Failed to parse GitHub response");
        }

        return new SyncSummary(0, 0, commitCount);
    }

    private int syncJiraSprints(IntegrationConfig config, ProjectGroup group) {
        if (config.getJiraBoardId() == null || config.getJiraBoardId().isBlank()) {
            return 0;
        }

        String url = String.format("%s/rest/agile/1.0/board/%s/sprint", config.getJiraBaseUrl(), config.getJiraBoardId());
        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(createJiraHeaders(config)), String.class);

        int count = 0;
        try {
            JsonNode root = objectMapper.readTree(response.getBody());
            Iterator<JsonNode> values = root.path("values").elements();
            while (values.hasNext()) {
                JsonNode sprintNode = values.next();
                String sprintId = sprintNode.path("id").asText(null);
                if (sprintId == null) {
                    continue;
                }
                JiraSprint sprint = jiraSprintRepository
                        .findByGroupIdAndJiraSprintId(group.getId(), sprintId)
                        .orElseGet(() -> JiraSprint.builder()
                                .group(group)
                                .jiraSprintId(sprintId)
                                .build());

                sprint.setName(sprintNode.path("name").asText(null));
                sprint.setState(sprintNode.path("state").asText(null));
                sprint.setStartDate(parseDate(sprintNode.path("startDate").asText(null)));
                sprint.setEndDate(parseDate(sprintNode.path("endDate").asText(null)));

                jiraSprintRepository.save(sprint);
                count++;
            }
        } catch (IOException e) {
            throw new BadRequestException("Failed to parse Jira sprint response");
        }

        return count;
    }

    private int syncJiraIssues(IntegrationConfig config, ProjectGroup group) {
        String url = String.format("%s/rest/api/3/search?jql=project=%s&maxResults=100", config.getJiraBaseUrl(), config.getJiraProjectKey());
        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(createJiraHeaders(config)), String.class);

        int count = 0;
        try {
            JsonNode root = objectMapper.readTree(response.getBody());
            for (JsonNode issueNode : root.path("issues")) {
                String issueKey = issueNode.path("key").asText(null);
                if (issueKey == null) {
                    continue;
                }
                JiraIssue issue = jiraIssueRepository
                        .findByGroupIdAndJiraIssueKey(group.getId(), issueKey)
                        .orElseGet(() -> JiraIssue.builder()
                                .group(group)
                                .jiraIssueKey(issueKey)
                                .build());

                JsonNode fields = issueNode.path("fields");
                issue.setSummary(fields.path("summary").asText(null));
                issue.setDescription(extractJiraDescription(fields.path("description")));
                issue.setIssueType(fields.path("issuetype").path("name").asText(null));
                issue.setStatus(fields.path("status").path("name").asText(null));
                issue.setAssigneeAccountId(fields.path("assignee").path("accountId").asText(null));

                JsonNode sprintNode = fields.path("sprint");
                issue.setSprintId(sprintNode.path("id").asText(null));
                issue.setSprintName(sprintNode.path("name").asText(null));

                jiraIssueRepository.save(issue);
                count++;
            }
        } catch (IOException e) {
            throw new BadRequestException("Failed to parse Jira issue response");
        }

        return count;
    }

    private String fetchJiraProjectName(IntegrationConfig config) {
        String url = String.format("%s/rest/api/3/project/%s", config.getJiraBaseUrl(), config.getJiraProjectKey());
        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(createJiraHeaders(config)), String.class);
        try {
            JsonNode root = objectMapper.readTree(response.getBody());
            return root.path("name").asText(null);
        } catch (IOException e) {
            return null;
        }
    }

    private HttpHeaders createJiraHeaders(IntegrationConfig config) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + config.getJiraAccessToken());
        headers.set("Accept", "application/json");
        return headers;
    }

    private HttpHeaders createGithubHeaders(IntegrationConfig config) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "token " + config.getGithubToken());
        headers.set("Accept", "application/vnd.github+json");
        return headers;
    }

    private LocalDateTime parseDate(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return LocalDateTime.ofInstant(Instant.parse(value), ZoneOffset.UTC);
        } catch (DateTimeParseException ex) {
            return null;
        }
    }

    private String extractJiraDescription(JsonNode descriptionNode) {
        if (descriptionNode == null || descriptionNode.isNull()) {
            return null;
        }
        if (descriptionNode.isTextual()) {
            return descriptionNode.asText();
        }
        return descriptionNode.toString();
    }

    public record SyncSummary(int jiraIssueCount, int jiraSprintCount, int githubCommitCount) {
    }
}

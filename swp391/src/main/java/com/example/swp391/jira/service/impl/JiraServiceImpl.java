package com.example.swp391.jira.service.impl;

import com.example.swp391.jira.dto.response.JiraIssueResponse;
import com.example.swp391.jira.dto.response.JiraProjectResponse;
import com.example.swp391.jira.service.IJiraService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class JiraServiceImpl implements IJiraService {

    @Value("${jira.base-url}")
    private String baseUrl;

    @Value("${jira.admin-email}")
    private String adminEmail;

    @Value("${jira.api-token}")
    private String apiToken;

    private final RestTemplate restTemplate;

    public JiraProjectResponse createProject(
            String key,
            String name,
            String leadAccountId
    ) {

        String url = baseUrl + "/rest/api/3/project";

        Map<String, Object> body = new HashMap<>();
        body.put("key", key);
        body.put("name", name);
        body.put("projectTypeKey", "software");
        body.put("projectTemplateKey", "com.pyxis.greenhopper.jira:gh-simplified-scrum-classic");
        body.put("leadAccountId", leadAccountId);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBasicAuth(adminEmail, apiToken);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

        ResponseEntity<JiraProjectResponse> response =
                restTemplate.postForEntity(url, request, JiraProjectResponse.class);

        return response.getBody();
    }

    @Override
    public void deleteProjectQuietly(String projectKey) {

        try {

            String url = baseUrl + "/rest/api/3/project/" + projectKey;

            HttpEntity<Void> entity = new HttpEntity<>(createHeaders());

            restTemplate.exchange(
                    url,
                    HttpMethod.DELETE,
                    entity,
                    Void.class
            );

            log.info("Rollback Jira project success: {}", projectKey);

        } catch (Exception e) {

            log.warn("Rollback Jira project failed: {}", projectKey);

        }
    }

    @Override
    public void addUserToProjectRole(String projectKey, String roleId, String accountId) {

        String url = baseUrl +
                "/rest/api/3/project/" + projectKey +
                "/role/" + roleId;

        Map<String, Object> body = Map.of(
                "user", List.of(accountId)
        );

        HttpEntity<Map<String, Object>> entity =
                new HttpEntity<>(body, createHeaders());

        restTemplate.exchange(
                url,
                HttpMethod.POST,
                entity,
                Void.class
        );
    }

    @Override
    public void removeUserFromProjectRole(String projectKey, String roleId, String accountId) {

        String url = baseUrl +
                "/rest/api/3/project/" + projectKey +
                "/role/" + roleId +
                "?user=" + accountId;

        restTemplate.exchange(
                url,
                HttpMethod.DELETE,
                new HttpEntity<>(createHeaders()),
                Void.class
        );
    }

    @Override
    public List<JiraIssueResponse> getProjectIssues(String projectKey) {
        String url = baseUrl + "/rest/api/3/search?jql=project=" + projectKey;

        try {
            HttpEntity<Void> entity = new HttpEntity<>(createHeaders());
            ResponseEntity<Map> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    Map.class
            );

            List<JiraIssueResponse> issues = new ArrayList<>();
            Map<String, Object> body = response.getBody();

            if (body != null && body.get("issues") != null) {
                List<Map<String, Object>> issuesList = (List<Map<String, Object>>) body.get("issues");

                for (Map<String, Object> issueMap : issuesList) {
                    Map<String, Object> fields = (Map<String, Object>) issueMap.get("fields");

                    JiraIssueResponse issue = JiraIssueResponse.builder()
                            .id((String) issueMap.get("id"))
                            .key((String) issueMap.get("key"))
                            .summary((String) fields.get("summary"))
                            .description(fields.get("description") != null ? fields.get("description").toString() : null)
                            .status((String) ((Map<String, Object>) fields.get("status")).get("name"))
                            .priority(fields.get("priority") != null ?
                                    (String) ((Map<String, Object>) fields.get("priority")).get("name") : null)
                            .issueType(fields.get("issuetype") != null ?
                                    (String) ((Map<String, Object>) fields.get("issuetype")).get("name") : null)
                            .build();

                    if (fields.get("assignee") != null) {
                        Map<String, Object> assignee = (Map<String, Object>) fields.get("assignee");
                        issue.setAssigneeAccountId((String) assignee.get("accountId"));
                        issue.setAssigneeDisplayName((String) assignee.get("displayName"));
                    }

                    if (fields.get("reporter") != null) {
                        Map<String, Object> reporter = (Map<String, Object>) fields.get("reporter");
                        issue.setReporterAccountId((String) reporter.get("accountId"));
                        issue.setReporterDisplayName((String) reporter.get("displayName"));
                    }

                    issues.add(issue);
                }
            }

            return issues;

        } catch (Exception e) {
            log.error("Failed to get project issues", e);
            throw new RuntimeException("Failed to get project issues", e);
        }
    }

    @Override
    public JiraIssueResponse getIssueByKey(String projectKey, String issueKey) {
        String url = baseUrl + "/rest/api/3/issue/" + issueKey;

        try {
            HttpEntity<Void> entity = new HttpEntity<>(createHeaders());
            ResponseEntity<Map> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    Map.class
            );

            Map<String, Object> body = response.getBody();
            Map<String, Object> fields = (Map<String, Object>) body.get("fields");

            return JiraIssueResponse.builder()
                    .id((String) body.get("id"))
                    .key((String) body.get("key"))
                    .summary((String) fields.get("summary"))
                    .description(fields.get("description") != null ? fields.get("description").toString() : null)
                    .status((String) ((Map<String, Object>) fields.get("status")).get("name"))
                    .priority(fields.get("priority") != null ?
                            (String) ((Map<String, Object>) fields.get("priority")).get("name") : null)
                    .issueType(fields.get("issuetype") != null ?
                            (String) ((Map<String, Object>) fields.get("issuetype")).get("name") : null)
                    .build();

        } catch (Exception e) {
            log.error("Failed to get issue by key", e);
            throw new RuntimeException("Failed to get issue by key", e);
        }
    }

    @Override
    public JiraIssueResponse createIssue(
            String projectKey,
            String summary,
            String description,
            String issueType,
            String priority,
            String assigneeAccountId
    ) {

        String url = baseUrl + "/rest/api/3/issue";

        Map<String, Object> fields = new HashMap<>();

        // Project
        fields.put("project", Map.of("key", projectKey));

        // Summary
        fields.put("summary", summary);

        // Issue type
        fields.put("issuetype", Map.of(
                "name", issueType != null ? issueType : "Task"
        ));

        // Description (ADF format)
        if (description != null && !description.isBlank()) {

            Map<String, Object> descriptionADF = Map.of(
                    "type", "doc",
                    "version", 1,
                    "content", List.of(
                            Map.of(
                                    "type", "paragraph",
                                    "content", List.of(
                                            Map.of(
                                                    "type", "text",
                                                    "text", description
                                            )
                                    )
                            )
                    )
            );

            fields.put("description", descriptionADF);
        }

        // Priority
        if (priority != null) {
            fields.put("priority", Map.of("name", priority));
        }

        // Assignee
        if (assigneeAccountId != null) {
            fields.put("assignee", Map.of("accountId", assigneeAccountId));
        }

        Map<String, Object> body = Map.of("fields", fields);

        try {

            HttpEntity<Map<String, Object>> entity =
                    new HttpEntity<>(body, createHeaders());

            ResponseEntity<Map> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    Map.class
            );

            Map<String, Object> res = response.getBody();

            return JiraIssueResponse.builder()
                    .id((String) res.get("id"))
                    .key((String) res.get("key"))
                    .summary(summary)
                    .status("To Do")
                    .issueType(issueType)
                    .priority(priority)
                    .build();

        } catch (Exception e) {

            log.error("Create Jira issue failed", e);
            throw new RuntimeException("Create Jira issue failed", e);

        }
    }

    @Override
    public JiraIssueResponse updateIssueStatus(String issueKey, String status) {
        // This would require workflow transitions which is complex
        // For now, return the issue as-is
        return getIssueByKey(null, issueKey);
    }

    @Override
    public void deleteIssue(String issueKey) {
        String url = baseUrl + "/rest/api/3/issue/" + issueKey;

        try {
            HttpEntity<Void> entity = new HttpEntity<>(createHeaders());
            restTemplate.exchange(
                    url,
                    HttpMethod.DELETE,
                    entity,
                    Void.class
            );
        } catch (Exception e) {
            log.error("Failed to delete issue", e);
            throw new RuntimeException("Failed to delete issue", e);
        }
    }

    private HttpHeaders createHeaders() {

        String auth = adminEmail + ":" + apiToken;
        String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes());

        HttpHeaders headers = new HttpHeaders();

        headers.set("Authorization", "Basic " + encodedAuth);
        headers.setContentType(MediaType.APPLICATION_JSON);

        return headers;
    }
}

package com.example.swp391.jira.service.impl;

import com.example.swp391.jira.service.IJiraService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;
import java.util.Base64;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;

@Slf4j
@Service
@RequiredArgsConstructor
public class JiraServiceImpl implements IJiraService {

    @Value("${jira.base-url}")
    private String baseUrl;

    @Value("${jira.admin-email}")
    private String adminEmail;

    @Value("${jira.api-token}")
    private String apiToken;

    private final RestTemplate restTemplate;

    @Override
    public String createGroup(String groupName) {

        String url = baseUrl + "/rest/api/3/group";

        Map<String, String> body = Map.of("name", groupName);

        HttpEntity<Map<String, String>> entity =
                new HttpEntity<>(body, createHeaders());

        try {

            ResponseEntity<String> res =
                    restTemplate.postForEntity(url, entity, String.class);

            log.info("Create Jira group success: {}", groupName);
            return groupName;

        } catch (HttpStatusCodeException e) {

            log.error("Jira status: {}", e.getStatusCode());
            log.error("Jira response: {}", e.getResponseBodyAsString());

            throw new RuntimeException(
                    "Jira error: " + e.getResponseBodyAsString(), e);

        } catch (Exception e) {

            log.error("Unexpected Jira error", e);
            throw e;
        }
    }

    @Override
    public void deleteGroupQuietly(String groupName) {
        try {
            String url = baseUrl + "/rest/api/3/group?groupname=" + groupName;

            HttpEntity<Void> entity = new HttpEntity<>(createHeaders());

            restTemplate.exchange(url, HttpMethod.DELETE, entity, Void.class);
        } catch (Exception ignored) {
        }
    }

    @Override
    public void addUserToGroup(String jiraAccountId, String groupName) {

        String url = baseUrl + "/rest/api/3/group/user"
                + "?groupname=" + groupName;

        Map<String, String> body = Map.of(
                "accountId", jiraAccountId
        );

        HttpEntity<Map<String, String>> request =
                new HttpEntity<>(body, createHeaders());

        restTemplate.exchange(
                url,
                HttpMethod.POST,
                request,
                Void.class
        );
    }

    @Override
    public void removeUserFromGroup(String jiraAccountId, String groupName) {

        String url = baseUrl + "/rest/api/3/group/user"
                + "?groupname=" + groupName
                + "&accountId=" + jiraAccountId;

        restTemplate.exchange(
                url,
                HttpMethod.DELETE,
                new HttpEntity<>(createHeaders()),
                Void.class
        );
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

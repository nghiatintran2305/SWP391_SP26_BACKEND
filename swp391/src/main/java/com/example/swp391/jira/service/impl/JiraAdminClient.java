package com.example.swp391.jira.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpHeaders;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;

@Service
@RequiredArgsConstructor
public class JiraAdminClient {

    @Value("${jira.base-url}")
    private String baseUrl;

    @Value("${jira.admin-email}")
    private String adminEmail;

    @Value("${jira.api-token}")
    private String apiToken;

    @Value("${jira.org-id}")
    private String orgId;

    private final RestTemplate restTemplate;

    public Optional<String> validateAccountId(String accountId) {

        String url = baseUrl + "/rest/api/3/user?accountId=" + accountId;

        HttpHeaders headers = new HttpHeaders();
        headers.setBasicAuth(adminEmail, apiToken);

        ResponseEntity<Map<String, Object>> res =
                restTemplate.exchange(
                        url,
                        HttpMethod.GET,
                        new HttpEntity<>(headers),
                        new ParameterizedTypeReference<>() {}
                );

        return Optional.ofNullable((String) res.getBody().get("accountId"));
    }

    public void inviteUser(String email) {

        String url = "https://api.atlassian.com/admin/v1/orgs/"
                + orgId + "/users/invite";

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(apiToken);
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> body = Map.of(
                "email", email,
                "products", List.of("jira-software")
        );

        HttpEntity<Map<String, Object>> request =
                new HttpEntity<>(body, headers);

        restTemplate.postForEntity(url, request, String.class);
    }

    public boolean isUserInOrg(String email) {

        try {
            String url = String.format(
                    "%s/admin/v1/orgs/%s/users?query=%s",
                    baseUrl,
                    orgId,
                    email
            );

            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(apiToken);

            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<Map> res = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    Map.class
            );

            List<?> data = (List<?>) res.getBody().get("data");

            return data != null && !data.isEmpty();

        } catch (HttpClientErrorException.NotFound e) {
            return false;

        } catch (Exception e) {
            throw e;
        }
    }
}


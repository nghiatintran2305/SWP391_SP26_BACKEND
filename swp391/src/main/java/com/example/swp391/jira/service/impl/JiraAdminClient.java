package com.example.swp391.jira.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpHeaders;

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

}


package com.example.swp391.jira.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class JiraOAuthService {

    @Value("${jira.oauth.client-id}")
    private String clientId;

    @Value("${jira.oauth.client-secret}")
    private String clientSecret;

    @Value("${jira.oauth.redirect-uri}")
    private String redirectUri;

    private final RestTemplate restTemplate;

    public String buildAuthorizeUrl() {
        return "https://auth.atlassian.com/authorize"
                + "?audience=api.atlassian.com"
                + "&client_id=" + clientId
                + "&scope=read:me%20read:jira-user%20read:jira-work"
                + "&redirect_uri=" + redirectUri
                + "&response_type=code"
                + "&prompt=consent";
    }


    public String exchangeToken(String code) {

        Map<String, String> body = Map.of(
                "grant_type", "authorization_code",
                "client_id", clientId,
                "client_secret", clientSecret,
                "code", code,
                "redirect_uri", redirectUri
        );

        ResponseEntity<Map> response =
                restTemplate.postForEntity(
                        "https://auth.atlassian.com/oauth/token",
                        body,
                        Map.class
                );

        return (String) response.getBody().get("access_token");
    }

    public String getAccountId(String accessToken) {

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);

        HttpEntity<Void> entity = new HttpEntity<>(headers);

        ResponseEntity<Map> res =
                restTemplate.exchange(
                        "https://api.atlassian.com/me",
                        HttpMethod.GET,
                        entity,
                        Map.class
                );

        return (String) res.getBody().get("account_id");
    }
}


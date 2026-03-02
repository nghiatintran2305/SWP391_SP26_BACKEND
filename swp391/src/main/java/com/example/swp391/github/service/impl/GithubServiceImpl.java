package com.example.swp391.github.service.impl;

import com.example.swp391.exception.BadRequestException;
import com.example.swp391.github.service.IGithubService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class GithubServiceImpl implements IGithubService {

    @Value("${github.base-url}")
    private String baseUrl;

    @Value("${github.org}")
    private String org;

    @Value("${github.admin-token}")
    private String adminToken;

    private final RestTemplate restTemplate;

    // create team
    @Override
    public String createTeam(String teamName, String description) {

        String url = baseUrl + "/orgs/" + org + "/teams";

        Map<String, Object> body = Map.of(
                "name", teamName,
                "description", description,
                "privacy", "closed"
        );

        HttpEntity<Map<String, Object>> entity =
                new HttpEntity<>(body, createHeaders());

        try {
            ResponseEntity<Map> res =
                    restTemplate.postForEntity(url, entity, Map.class);

            // GitHub API returns team slug
            return (String) res.getBody().get("slug");

        } catch (HttpClientErrorException e) {

            if (e.getStatusCode() == HttpStatus.UNPROCESSABLE_ENTITY) {
                throw new BadRequestException("GitHub team already exists");
            }

            throw new RuntimeException("Create GitHub team failed", e);
        }
    }

    @Override
    public void deleteTeamQuietly(String teamSlug) {
        try {
            String url = baseUrl + "/orgs/" + org + "/teams/" + teamSlug;

            restTemplate.exchange(
                    url,
                    HttpMethod.DELETE,
                    new HttpEntity<>(createHeaders()),
                    Void.class
            );

        } catch (Exception ignored) {
        }
    }

    @Override
    public void addMemberToTeam(String username, String teamSlug) {

        String url = baseUrl + "/orgs/" + org
                + "/teams/" + teamSlug
                + "/memberships/" + username;

        HttpEntity<Void> request = new HttpEntity<>(createHeaders());

        ResponseEntity<Void> response = restTemplate.exchange(
                url,
                HttpMethod.PUT,
                request,
                Void.class
        );

        if (!response.getStatusCode().is2xxSuccessful()) {
            throw new RuntimeException("Failed to add member to GitHub team");
        }
    }

    @Override
    public void removeMemberFromTeam(String username, String teamSlug) {

        String url = baseUrl + "/orgs/" + org
                + "/teams/" + teamSlug
                + "/memberships/" + username;

        restTemplate.exchange(
                url,
                HttpMethod.DELETE,
                new HttpEntity<>(createHeaders()),
                Void.class
        );
    }

    private HttpHeaders createHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(adminToken);
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        return headers;
    }
}

package com.example.swp391.github.service.impl;

import com.example.swp391.exceptions.BadRequestException;
import com.example.swp391.github.dto.response.CommitStats;
import com.example.swp391.github.dto.response.CommitSummary;
import com.example.swp391.github.dto.response.GithubRepoResponse;
import com.example.swp391.github.service.IGithubService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
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

    // CREATE REPO
    @Override
    public GithubRepoResponse createRepo(String repoName) {

        String url = baseUrl + "/orgs/" + org + "/repos";

        Map<String, Object> body = Map.of(
                "name", repoName,
                "private", true
        );

        try {

            ResponseEntity<GithubRepoResponse> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    new HttpEntity<>(body, createHeaders()),
                    GithubRepoResponse.class
            );

            System.out.println(response.getBody());

            return response.getBody();

        } catch (HttpClientErrorException e) {

            if (e.getStatusCode() == HttpStatus.UNPROCESSABLE_ENTITY) {
                throw new BadRequestException("GitHub repo already exists");
            }

            throw new RuntimeException("Create GitHub repo failed: " + e.getResponseBodyAsString());
        }
    }

    // DELETE REPO
    @Override
    public void deleteRepoQuietly(String repoName) {
        try {

            String url = baseUrl + "/repos/" + org + "/" + repoName;

            restTemplate.exchange(
                    url,
                    HttpMethod.DELETE,
                    new HttpEntity<>(createHeaders()),
                    Void.class
            );

        } catch (Exception ignored) {
        }
    }

    // ADD COLLABORATOR
    @Override
    public void addCollaboratorToRepo(String repoName, String username) {

        String url = baseUrl +
                "/repos/" + org + "/" + repoName +
                "/collaborators/" + username;

        Map<String, String> body = Map.of(
                "permission", "push"
        );

        restTemplate.exchange(
                url,
                HttpMethod.PUT,
                new HttpEntity<>(body, createHeaders()),
                Void.class
        );
    }

    // REMOVE COLLABORATOR
    @Override
    public void removeCollaboratorFromRepo(String repoName, String username) {

        String url = baseUrl +
                "/repos/" + org + "/" + repoName +
                "/collaborators/" + username;

        restTemplate.exchange(
                url,
                HttpMethod.DELETE,
                new HttpEntity<>(createHeaders()),
                Void.class
        );
    }

    // GET REPO COMMIT STATS
    @Override
    public CommitStats getRepoCommitStats(String repoName) {
        String url = baseUrl + "/repos/" + org + "/" + repoName + "/stats/contributors";

        try {
            ResponseEntity<List> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    new HttpEntity<>(createHeaders()),
                    List.class
            );

            List<Map<String, Object>> contributors = response.getBody();
            int totalCommits = 0;
            int totalAdditions = 0;
            int totalDeletions = 0;

            if (contributors != null) {
                for (Map<String, Object> contributor : contributors) {
                    totalCommits += (Integer) contributor.getOrDefault("total", 0);
                    Map<String, Integer> weeks = (Map<String, Integer>) contributor.get("weeks");
                    if (weeks != null) {
                        totalAdditions += weeks.getOrDefault("a", 0);
                        totalDeletions += weeks.getOrDefault("d", 0);
                    }
                }
            }

            return CommitStats.builder()
                    .repoName(repoName)
                    .totalCommits(totalCommits)
                    .additions(totalAdditions)
                    .deletions(totalDeletions)
                    .build();

        } catch (Exception e) {
            throw new RuntimeException("Failed to get repo commit stats", e);
        }
    }

    // GET USER COMMIT STATS
    @Override
    public CommitStats getUserCommitStats(String repoName, String username) {
        String url = baseUrl + "/repos/" + org + "/" + repoName + "/stats/contributors";

        try {
            ResponseEntity<List> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    new HttpEntity<>(createHeaders()),
                    List.class
            );

            List<Map<String, Object>> contributors = response.getBody();

            if (contributors != null) {
                for (Map<String, Object> contributor : contributors) {
                    Map<String, Object> author = (Map<String, Object>) contributor.get("author");
                    if (author != null && username.equals(author.get("login"))) {
                        int totalCommits = (Integer) contributor.getOrDefault("total", 0);
                        int additions = 0;
                        int deletions = 0;

                        // Calculate from weekly data
                        List<Map<String, Object>> weeks = (List<Map<String, Object>>) contributor.get("weeks");
                        if (weeks != null) {
                            for (Map<String, Object> week : weeks) {
                                additions += ((Number) week.getOrDefault("a", 0)).intValue();
                                deletions += ((Number) week.getOrDefault("d", 0)).intValue();
                            }
                        }

                        return CommitStats.builder()
                                .repoName(repoName)
                                .username(username)
                                .totalCommits(totalCommits)
                                .additions(additions)
                                .deletions(deletions)
                                .build();
                    }
                }
            }

            return CommitStats.builder()
                    .repoName(repoName)
                    .username(username)
                    .totalCommits(0)
                    .additions(0)
                    .deletions(0)
                    .build();

        } catch (Exception e) {
            throw new RuntimeException("Failed to get user commit stats", e);
        }
    }

    // GET TEAM COMMIT SUMMARY
    @Override
    public List<CommitSummary> getTeamCommitSummary(String repoName) {
        String url = baseUrl + "/repos/" + org + "/" + repoName + "/stats/contributors";

        try {
            ResponseEntity<List> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    new HttpEntity<>(createHeaders()),
                    List.class
            );

            List<Map<String, Object>> contributors = response.getBody();
            List<CommitSummary> summaries = new ArrayList<>();

            if (contributors != null) {
                for (Map<String, Object> contributor : contributors) {
                    Map<String, Object> author = (Map<String, Object>) contributor.get("author");
                    if (author != null) {
                        String username = (String) author.get("login");
                        int commits = (Integer) contributor.getOrDefault("total", 0);
                        int additions = 0;
                        int deletions = 0;

                        List<Map<String, Object>> weeks = (List<Map<String, Object>>) contributor.get("weeks");
                        if (weeks != null) {
                            for (Map<String, Object> week : weeks) {
                                additions += ((Number) week.getOrDefault("a", 0)).intValue();
                                deletions += ((Number) week.getOrDefault("d", 0)).intValue();
                            }
                        }

                        summaries.add(CommitSummary.builder()
                                .username(username)
                                .commits(commits)
                                .additions(additions)
                                .deletions(deletions)
                                .build());
                    }
                }
            }

            return summaries;

        } catch (Exception e) {
            throw new RuntimeException("Failed to get team commit summary", e);
        }
    }

    private HttpHeaders createHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(adminToken);
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        return headers;
    }
}
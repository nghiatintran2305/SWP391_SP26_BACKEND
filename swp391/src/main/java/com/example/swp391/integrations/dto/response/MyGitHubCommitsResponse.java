package com.example.swp391.integrations.dto.response;

import java.util.List;
import lombok.Getter;
import lombok.Setter;

/**
 * Wrapper response for "my commits" endpoint.
 *
 * If the current user has no GitHub identity mapping (email), mapped = false and commits = empty.
 */
@Getter
@Setter
public class MyGitHubCommitsResponse {
    private boolean mapped;
    private String githubEmail;
    private List<GitHubCommitResponse> commits;
}

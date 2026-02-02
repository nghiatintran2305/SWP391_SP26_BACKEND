package com.example.swp391.integrations.dto.response;

import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

/**
 * DTO returned to FE for cached GitHub commits.
 *
 * NOTE: This project currently caches only basic commit fields.
 */
@Getter
@Setter
public class GitHubCommitResponse {
    private String id;
    private String groupId;
    private String repositoryId;
    private String sha;
    private String authorName;
    private String authorEmail;
    private LocalDateTime committedAt;
    private String message;

    // Local user mapping by githubEmail (nullable if unmapped)
    private String mappedAccountId;
    private String mappedUsername;
}

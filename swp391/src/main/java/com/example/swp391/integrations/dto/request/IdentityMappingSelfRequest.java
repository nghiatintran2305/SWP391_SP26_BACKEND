package com.example.swp391.integrations.dto.request;

import lombok.Getter;
import lombok.Setter;

/**
 * Self-service mapping update request.
 *
 * We intentionally do NOT allow FE to send accountId to avoid privilege escalation.
 */
@Getter
@Setter
public class IdentityMappingSelfRequest {
    private String jiraAccountId;
    private String jiraEmail;
    private String githubUsername;
    private String githubEmail;
}

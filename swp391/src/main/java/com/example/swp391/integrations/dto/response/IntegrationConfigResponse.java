package com.example.swp391.integrations.dto.response;

import java.time.LocalDateTime;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class IntegrationConfigResponse {

    private String id;
    private String groupId;
    private String jiraBaseUrl;
    private String jiraProjectKey;
    private String jiraBoardId;
    private String jiraAccessTokenMasked;
    private String githubOwner;
    private String githubRepo;
    private String githubTokenMasked;
    private LocalDateTime lastSyncAt;
}

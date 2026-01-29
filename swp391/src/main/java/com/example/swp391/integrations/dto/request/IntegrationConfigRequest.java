package com.example.swp391.integrations.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class IntegrationConfigRequest {

    @NotBlank
    private String groupId;

    @NotBlank
    private String jiraBaseUrl;

    @NotBlank
    private String jiraProjectKey;

    private String jiraBoardId;

    @NotBlank
    private String jiraAccessToken;

    @NotBlank
    private String githubOwner;

    @NotBlank
    private String githubRepo;

    @NotBlank
    private String githubToken;
}

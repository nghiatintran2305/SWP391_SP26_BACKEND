package com.example.swp391.integrations.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class IdentityMappingRequest {

    @NotBlank
    private String accountId;

    private String jiraAccountId;

    private String jiraEmail;

    private String githubUsername;

    private String githubEmail;
}

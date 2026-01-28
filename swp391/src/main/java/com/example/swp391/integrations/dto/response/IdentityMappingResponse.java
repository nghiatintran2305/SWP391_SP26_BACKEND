package com.example.swp391.integrations.dto.response;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class IdentityMappingResponse {

    private String id;
    private String accountId;
    private String jiraAccountId;
    private String jiraEmail;
    private String githubUsername;
    private String githubEmail;
}

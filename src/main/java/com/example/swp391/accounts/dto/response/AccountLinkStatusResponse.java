package com.example.swp391.accounts.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccountLinkStatusResponse {
    
    private boolean githubLinked;
    private String githubUsername;
    private boolean jiraLinked;
    private String jiraAccountId;
}

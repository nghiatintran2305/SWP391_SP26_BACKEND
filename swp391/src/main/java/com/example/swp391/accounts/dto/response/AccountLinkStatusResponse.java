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
    
    /**
     * Whether the user's GitHub account is linked
     */
    private boolean githubLinked;
    
    /**
     * GitHub username if linked, null otherwise
     */
    private String githubUsername;
    
    /**
     * Whether the user's Jira account is linked
     */
    private boolean jiraLinked;
    
    /**
     * Jira account ID if linked, null otherwise
     */
    private String jiraAccountId;
    
    /**
     * Jira account email if linked, null otherwise
     */
    private String jiraAccountEmail;
}

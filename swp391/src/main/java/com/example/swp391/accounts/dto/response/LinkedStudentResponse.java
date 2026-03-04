package com.example.swp391.accounts.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class LinkedStudentResponse {
    private String id;
    private String username;
    private String email;
    private String fullName;
    private boolean githubLinked;
    private boolean jiraLinked;
}
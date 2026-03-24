package com.example.swp391.github.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommitDetail {

    private String sha;

    private String message;

    private String author;

    private String authorEmail;

    private LocalDateTime commitDate;

    private String htmlUrl;

    private int additions;

    private int deletions;
}

package com.example.swp391.github.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommitStats {

    private String repoName;

    private String username;

    private int totalCommits;

    private int additions;

    private int deletions;
}

package com.example.swp391.github.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommitSummary {

    private String username;

    private int commits;

    private int additions;

    private int deletions;
}

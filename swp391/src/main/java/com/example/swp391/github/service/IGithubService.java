package com.example.swp391.github.service;

import com.example.swp391.github.dto.response.CommitStats;
import com.example.swp391.github.dto.response.CommitSummary;
import com.example.swp391.github.dto.response.GithubRepoResponse;

import java.util.List;

public interface IGithubService {

    GithubRepoResponse createRepo(String repoName);

    void deleteRepoQuietly(String repoName);

    void addCollaboratorToRepo(String repoName, String username);

    void removeCollaboratorFromRepo(String repoName, String username);

    // Commit Statistics
    CommitStats getRepoCommitStats(String repoName);

    CommitStats getUserCommitStats(String repoName, String username);

    List<CommitSummary> getTeamCommitSummary(String repoName);
}

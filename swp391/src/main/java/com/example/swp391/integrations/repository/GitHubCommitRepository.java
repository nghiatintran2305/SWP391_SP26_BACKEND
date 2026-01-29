package com.example.swp391.integrations.repository;

import com.example.swp391.integrations.entity.GitHubCommit;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GitHubCommitRepository extends JpaRepository<GitHubCommit, String> {
    Optional<GitHubCommit> findByRepositoryIdAndSha(String repositoryId, String sha);
}

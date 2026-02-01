package com.example.swp391.integrations.repository;

import com.example.swp391.integrations.entity.GitHubCommit;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GitHubCommitRepository extends JpaRepository<GitHubCommit, String> {
    Optional<GitHubCommit> findByRepositoryIdAndSha(String repositoryId, String sha);

    // Query commits by group via repository -> group relationship.
    List<GitHubCommit> findByRepository_Group_Id(String groupId);

    // Query commits by group and author email.
    List<GitHubCommit> findByRepository_Group_IdAndAuthorEmailIgnoreCase(String groupId, String authorEmail);

    // Global query by author email.
    List<GitHubCommit> findByAuthorEmailIgnoreCase(String authorEmail);

    // Optional: date range helpers (used by reports/stats if you extend later).
    List<GitHubCommit> findByRepository_Group_IdAndCommittedAtBetween(String groupId, LocalDateTime from, LocalDateTime to);
}

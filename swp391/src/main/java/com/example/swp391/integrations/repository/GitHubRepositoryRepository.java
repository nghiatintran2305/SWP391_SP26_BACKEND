package com.example.swp391.integrations.repository;

import com.example.swp391.integrations.entity.GitHubRepository;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GitHubRepositoryRepository extends JpaRepository<GitHubRepository, String> {
    Optional<GitHubRepository> findByGroupIdAndOwnerAndName(String groupId, String owner, String name);
}

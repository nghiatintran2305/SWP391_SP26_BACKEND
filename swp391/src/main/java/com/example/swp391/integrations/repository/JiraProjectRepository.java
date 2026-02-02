package com.example.swp391.integrations.repository;

import com.example.swp391.integrations.entity.JiraProject;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JiraProjectRepository extends JpaRepository<JiraProject, String> {
    Optional<JiraProject> findByGroupIdAndProjectKey(String groupId, String projectKey);
}

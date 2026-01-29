package com.example.swp391.integrations.repository;

import com.example.swp391.integrations.entity.JiraIssue;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JiraIssueRepository extends JpaRepository<JiraIssue, String> {
    Optional<JiraIssue> findByGroupIdAndJiraIssueKey(String groupId, String jiraIssueKey);
}

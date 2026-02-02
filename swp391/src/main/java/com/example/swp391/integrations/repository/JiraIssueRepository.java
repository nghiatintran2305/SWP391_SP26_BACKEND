package com.example.swp391.integrations.repository;

import com.example.swp391.integrations.entity.JiraIssue;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JiraIssueRepository extends JpaRepository<JiraIssue, String> {
    Optional<JiraIssue> findByGroupIdAndJiraIssueKey(String groupId, String jiraIssueKey);

    List<JiraIssue> findByGroupId(String groupId);

    List<JiraIssue> findByGroupIdAndAssigneeAccountId(String groupId, String assigneeAccountId);

    List<JiraIssue> findByAssigneeAccountId(String assigneeAccountId);
}

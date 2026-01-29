package com.example.swp391.integrations.repository;

import com.example.swp391.integrations.entity.JiraSprint;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JiraSprintRepository extends JpaRepository<JiraSprint, String> {
    Optional<JiraSprint> findByGroupIdAndJiraSprintId(String groupId, String jiraSprintId);
}

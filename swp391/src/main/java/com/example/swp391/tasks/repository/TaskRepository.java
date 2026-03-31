package com.example.swp391.tasks.repository;

import com.example.swp391.accounts.entity.Account;
import com.example.swp391.projects.entity.Project;
import com.example.swp391.tasks.entity.Task;
import com.example.swp391.tasks.enums.TaskStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TaskRepository extends JpaRepository<Task, String> {

    List<Task> findByProjectId(String projectId);

    List<Task> findByProjectIdAndStatus(String projectId, TaskStatus status);

    List<Task> findByAssignedTo(Account assignedTo);

    List<Task> findByAssignedToId(String accountId);

    List<Task> findByProjectIdAndAssignedTo(String projectId, Account assignedTo);

    Optional<Task> findByJiraIssueId(String jiraIssueId);

    Optional<Task> findByJiraIssueKey(String jiraIssueKey);

    boolean existsByProjectIdAndAssignedToId(String projectId, String accountId);

    long countByProjectId(String projectId);

    long countByProjectIdAndStatus(String projectId, TaskStatus status);

    List<Task> findByProjectIdAndIsRequirement(String projectId, boolean isRequirement);
}

package com.example.swp391.tasks.entity;

import com.example.swp391.accounts.entity.Account;
import com.example.swp391.projects.entity.Project;
import com.example.swp391.tasks.enums.TaskPriority;
import com.example.swp391.tasks.enums.TaskStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UuidGenerator;

import java.time.LocalDateTime;

@Entity
@Table(name = "tasks")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Task {

    @Id
    @GeneratedValue(generator = "UUID")
    @UuidGenerator
    @Column(length = 36, updatable = false, nullable = false)
    private String id;

    @ManyToOne
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @Column(nullable = false)
    private String taskName;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "jira_issue_id")
    private String jiraIssueId;

    @Column(name = "jira_issue_key")
    private String jiraIssueKey;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TaskStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TaskPriority priority;

    @ManyToOne
    @JoinColumn(name = "assigned_to")
    private Account assignedTo;

    @ManyToOne
    @JoinColumn(name = "created_by", nullable = false)
    private Account createdBy;

    @CreationTimestamp
    private LocalDateTime createdAt;

    private LocalDateTime dueDate;

    @Column(name = "is_requirement")
    private boolean isRequirement;
}

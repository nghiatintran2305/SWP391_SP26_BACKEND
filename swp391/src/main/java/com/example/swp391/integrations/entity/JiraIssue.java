package com.example.swp391.integrations.entity;

import com.example.swp391.projects.entity.ProjectGroup;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.UuidGenerator;

import java.time.LocalDateTime;

@Entity
@Table(name = "jira_issues")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JiraIssue {

    @Id
    @GeneratedValue(generator = "UUID")
    @UuidGenerator
    @Column(length = 36, updatable = false, nullable = false)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id", nullable = false)
    private ProjectGroup group;

    @Column(name = "jira_issue_key", nullable = false)
    private String jiraIssueKey;

    @Column(name = "issue_type")
    private String issueType;

    @Column(columnDefinition = "TEXT")
    private String summary;

    @Column(columnDefinition = "TEXT")
    private String description;

    private String status;

    @Column(name = "assignee_account_id")
    private String assigneeAccountId;

    @Column(name = "sprint_id")
    private String sprintId;

    @Column(name = "sprint_name")
    private String sprintName;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}

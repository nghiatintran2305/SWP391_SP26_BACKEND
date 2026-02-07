package com.example.swp391.jira.entity;

import com.example.swp391.projects.entity.ProjectGroup;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UuidGenerator;

import java.time.LocalDateTime;

@Entity
@Table(name = "jira_project_mappings")
@Getter
@Setter
public class JiraProjectMapping {

    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(length = 36, updatable = false, nullable = false)
    private String id;

    @OneToOne
    @JoinColumn(name = "group_id", nullable = false, unique = true)
    private ProjectGroup group;

    @Column(nullable = false)
    private String jiraProjectId;

    @Column(nullable = false)
    private String jiraProjectKey;

    @Column(nullable = false)
    private String jiraProjectName;

    @CreationTimestamp
    private LocalDateTime createdAt;
}


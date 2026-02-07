package com.example.swp391.github.entity;

import com.example.swp391.github.enums.GithubLinkStatus;
import com.example.swp391.projects.entity.ProjectGroup;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UuidGenerator;

import java.time.LocalDateTime;

@Entity
@Table(name = "github_repo_mappings")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class GithubRepoMapping {

    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(length = 36, nullable = false, updatable = false)
    private String id;

    @OneToOne
    @JoinColumn(name = "group_id", nullable = false, unique = true)
    private ProjectGroup group;

    @Column(nullable = false)
    private String repoOwner;

    @Column(nullable = false)
    private String repoName;

    @Column(nullable = false)
    private Long githubRepoId;

    @Enumerated(EnumType.STRING)
    private GithubLinkStatus status;

    @CreationTimestamp
    private LocalDateTime createdAt;
}


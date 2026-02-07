package com.example.swp391.github.entity;

import com.example.swp391.accounts.entity.Account;
import com.example.swp391.projects.entity.ProjectGroup;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "github_commits")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GithubCommit {

    @Id
    @Column(length = 40)
    private String sha;

    @ManyToOne
    @JoinColumn(name = "group_id", nullable = false)
    private ProjectGroup group;

    @ManyToOne
    @JoinColumn(name = "account_id")
    private Account author;

    private String authorEmail;
    private String message;
    private LocalDateTime commitTime;

    private String jiraIssueKey;
}



package com.example.swp391.github.entity;

import com.example.swp391.accounts.entity.Account;
import com.example.swp391.github.enums.GithubLinkStatus;
import com.example.swp391.jira.enums.JiraLinkStatus;
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
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.UuidGenerator;

import java.time.LocalDateTime;

@Entity
@Table(name = "github_user_mappings")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class GithubUserMapping {

    @Id
    @GeneratedValue
    @UuidGenerator
    private String id;

    @OneToOne
    @JoinColumn(name = "account_id", unique = true, nullable = false)
    private Account account;

    @Column(nullable = false)
    private Long githubUserId;

    @Column(nullable = false)
    private String githubUsername;

    @Enumerated(EnumType.STRING)
    private GithubLinkStatus status;
}





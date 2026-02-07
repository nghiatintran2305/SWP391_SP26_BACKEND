package com.example.swp391.jira.repository;

import java.util.Optional;

import com.example.swp391.accounts.entity.Account;
import com.example.swp391.jira.entity.JiraUserMapping;
import com.example.swp391.jira.enums.JiraLinkStatus;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JiraUserMappingRepository
        extends JpaRepository<JiraUserMapping, String> {

    Optional<JiraUserMapping> findByAccount(Account account);

    void deleteByAccount(Account account);

    boolean existsByAccountAndStatus(Account account, JiraLinkStatus status);
}

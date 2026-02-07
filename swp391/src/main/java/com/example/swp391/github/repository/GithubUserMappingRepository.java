package com.example.swp391.github.repository;


import com.example.swp391.accounts.entity.Account;
import com.example.swp391.github.entity.GithubUserMapping;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface GithubUserMappingRepository
        extends JpaRepository<GithubUserMapping, String> {

    Optional<GithubUserMapping> findByAccount(Account account);

    boolean existsByGithubUserId(Long githubUserId);

    void deleteByAccount(Account account);
}



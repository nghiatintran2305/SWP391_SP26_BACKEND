package com.example.swp391.github.service.impl;

import com.example.swp391.accounts.entity.Account;
import com.example.swp391.configs.security.SecurityUtil;
import com.example.swp391.github.entity.GithubUserMapping;
import com.example.swp391.github.repository.GithubUserMappingRepository;
import com.example.swp391.github.service.IGitUserLinkService;
import com.example.swp391.projects.enums.ProjectStatus;
import com.example.swp391.projects.repository.ProjectMemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class GithubUserLinkServiceImpl
        implements IGitUserLinkService {

    private final GithubUserMappingRepository repo;
    private final GithubOAuthService oauthService;
    private final SecurityUtil securityUtil;
    private final ProjectMemberRepository groupMemberRepo;

    @Override
    public String getAuthorizeUrl() {
        return oauthService.buildAuthorizeUrl();
    }

    @Transactional
    @Override
    public void handleCallback(String code) {
        oauthService.handleCallback(code);
    }

    @Override
    public GithubUserMapping getCurrentMapping() {
        Account account = securityUtil.getCurrentAccount();
        return repo.findByAccount(account)
                .orElseThrow(() -> new RuntimeException("Chưa liên kết GitHub"));
    }

    @Transactional
    public void unlink() {
        Account account = securityUtil.getCurrentAccount();

        boolean inActiveGroup = groupMemberRepo
                .existsByAccountAndProject_StatusIn(
                        account,
                        List.of(
                                ProjectStatus.CONFIGURED,
                                ProjectStatus.ACTIVE,
                                ProjectStatus.LOCKED
                        )
                );

        if (inActiveGroup) {
            throw new IllegalStateException(
                    "Không thể unlink khi đang thuộc một project đang hoạt động"
            );
        }

        repo.deleteByAccount(account);
    }
}


package com.example.swp391.github.service.impl;

import com.example.swp391.accounts.entity.Account;
import com.example.swp391.config.security.SecurityUtil;
import com.example.swp391.github.entity.GithubUserMapping;
import com.example.swp391.github.repository.GithubUserMappingRepository;
import com.example.swp391.github.service.IGitUserLinkService;
import com.example.swp391.projects.enums.GroupStatus;
import com.example.swp391.projects.repository.GroupMemberRepository;
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
    private final GroupMemberRepository groupMemberRepo;

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
                .existsByAccountAndGroup_StatusIn(
                        account,
                        List.of(
                                GroupStatus.CREATED,
                                GroupStatus.CONFIGURED,
                                GroupStatus.ACTIVE,
                                GroupStatus.LOCKED
                        )
                );

        if (inActiveGroup) {
            throw new IllegalStateException(
                    "Không thể unlink khi đang thuộc một group đang hoạt động"
            );
        }

        repo.deleteByAccount(account);
    }
}


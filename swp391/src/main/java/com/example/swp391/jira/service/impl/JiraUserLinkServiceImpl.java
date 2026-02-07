package com.example.swp391.jira.service.impl;

import com.example.swp391.accounts.entity.Account;
import com.example.swp391.config.security.SecurityUtil;
import com.example.swp391.jira.entity.JiraUserMapping;
import com.example.swp391.jira.enums.JiraLinkStatus;
import com.example.swp391.jira.repository.JiraUserMappingRepository;
import com.example.swp391.jira.service.IJiraUserLinkService;
import com.example.swp391.projects.enums.GroupStatus;
import com.example.swp391.projects.repository.GroupMemberRepository;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class JiraUserLinkServiceImpl implements IJiraUserLinkService {

    private final JiraUserMappingRepository repo;
    private final JiraAdminClient jiraAdminClient;
    private final SecurityUtil securityUtil;
    private final GroupMemberRepository groupMemberRepo;
    private final JiraOAuthService oauthService;


    @Override
    public String getAuthorizeUrl() {
        return oauthService.buildAuthorizeUrl();
    }

    @Transactional
    @Override
    public void handleCallback(String code) {

        Account account = securityUtil.getCurrentAccount();

        String accessToken = oauthService.exchangeToken(code);
        String accountId = oauthService.getAccountId(accessToken);

        JiraUserMapping mapping = repo.findByAccount(account)
                .orElseGet(() -> JiraUserMapping.builder()
                        .account(account)
                        .build());

        mapping.setJiraAccountId(accountId);
        mapping.setStatus(JiraLinkStatus.LINKED);

        repo.save(mapping);
    }

    @Transactional(readOnly = true)
    public JiraUserMapping getCurrentMapping() {
        Account account = securityUtil.getCurrentAccount();
        return repo.findByAccount(account)
                .orElseThrow(() -> new RuntimeException("Chưa liên kết Jira"));
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



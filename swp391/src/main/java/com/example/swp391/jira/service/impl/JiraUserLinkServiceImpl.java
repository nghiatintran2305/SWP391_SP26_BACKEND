package com.example.swp391.jira.service.impl;

import com.example.swp391.accounts.entity.Account;
import com.example.swp391.configs.security.SecurityUtil;
import com.example.swp391.jira.entity.JiraUserMapping;
import com.example.swp391.jira.enums.JiraLinkStatus;
import com.example.swp391.jira.repository.JiraUserMappingRepository;
import com.example.swp391.jira.service.IJiraUserLinkService;
import com.example.swp391.projects.enums.ProjectStatus;
import com.example.swp391.projects.repository.ProjectMemberRepository;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class JiraUserLinkServiceImpl implements IJiraUserLinkService {

    private final JiraUserMappingRepository repo;
    private final JiraAdminClient jiraAdminClient;
    private final SecurityUtil securityUtil;
    private final ProjectMemberRepository groupMemberRepo;
    private final JiraOAuthService oauthService;
    private final RestTemplate restTemplate;


    @Override
    public String getAuthorizeUrl() {
        return oauthService.buildAuthorizeUrl();
    }


    @Transactional
    @Override
    public void handleCallback(String code) {

        Account account = securityUtil.getCurrentAccount();

        // Exchange token
        String accessToken = oauthService.exchangeToken(code);

        // Map profile Jira
        Map<String, Object> me = oauthService.getMe(accessToken);

        String accountId = (String) me.get("account_id");
        String email = (String) me.get("email");

        // Kiểm tra xem Jira account đã được link với tài khoản local nào khác chưa
        if (repo.existsByAccountId(accountId)) {
            JiraUserMapping existingByJira = repo.findByAccountId(accountId).orElse(null);
            
            // Nếu đã link với tài khoản khác -> chặn
            if (existingByJira != null
                    && existingByJira.getAccount() != null
                    && !existingByJira.getAccount().getId().equals(account.getId())) {
                throw new IllegalStateException("This Jira account is already linked to another local account");
            }
            
            // Nếu đã link với chính account hiện tại -> update lại rồi return
            if (existingByJira != null
                    && existingByJira.getAccount() != null
                    && existingByJira.getAccount().getId().equals(account.getId())) {
                existingByJira.setJiraAccountId(accountId);
                existingByJira.setStatus(JiraLinkStatus.LINKED);
                repo.save(existingByJira);
                return;
            }
        }

        // Save mapping
        JiraUserMapping existingByAccount = repo.findByAccount(account).orElse(null);
        JiraUserMapping mapping = existingByAccount != null
                ? existingByAccount
                : JiraUserMapping.builder()
                        .account(account)
                        .build();

        mapping.setJiraAccountId(accountId);
        mapping.setStatus(JiraLinkStatus.LINKED);

        repo.save(mapping);

        // CHECK & INVITE

        try {
            boolean exists = jiraAdminClient.isUserInOrg(email);

            if (!exists) {
                jiraAdminClient.inviteUser(email);
            }

        } catch (Exception e) {
            System.err.println("Error checking/inviting user to Jira org: " + e.getMessage());
        }
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



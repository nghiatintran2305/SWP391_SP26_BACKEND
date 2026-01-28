package com.example.swp391.integrations.service.impl;

import com.example.swp391.accounts.entity.Account;
import com.example.swp391.accounts.repository.AccountRepository;
import com.example.swp391.audit.enums.AuditAction;
import com.example.swp391.audit.service.AuditLogService;
import com.example.swp391.config.security.SecurityUtil;
import com.example.swp391.exception.BadRequestException;
import com.example.swp391.exception.NotFoundException;
import com.example.swp391.integrations.dto.request.IntegrationConfigRequest;
import com.example.swp391.integrations.dto.request.SyncRequest;
import com.example.swp391.integrations.dto.response.IntegrationConfigResponse;
import com.example.swp391.integrations.dto.response.SyncJobResponse;
import com.example.swp391.integrations.entity.IntegrationConfig;
import com.example.swp391.integrations.entity.SyncJob;
import com.example.swp391.integrations.enums.SyncJobStatus;
import com.example.swp391.integrations.repository.IntegrationConfigRepository;
import com.example.swp391.integrations.repository.SyncJobRepository;
import com.example.swp391.integrations.service.IIntegrationConfigService;
import com.example.swp391.projects.entity.ProjectGroup;
import com.example.swp391.projects.repository.ProjectGroupRepository;
import jakarta.transaction.Transactional;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Transactional
public class IntegrationConfigServiceImpl implements IIntegrationConfigService {

    private final IntegrationConfigRepository configRepository;
    private final ProjectGroupRepository projectGroupRepository;
    private final SyncJobRepository syncJobRepository;
    private final AccountRepository accountRepository;
    private final AuditLogService auditLogService;

    @Override
    public IntegrationConfigResponse createConfig(IntegrationConfigRequest request) {
        ProjectGroup group = projectGroupRepository.findById(request.getGroupId())
                .orElseThrow(() -> new NotFoundException("Group not found"));

        if (configRepository.existsByGroupId(group.getId())) {
            throw new BadRequestException("Integration config already exists for this group");
        }

        IntegrationConfig config = IntegrationConfig.builder()
                .group(group)
                .jiraBaseUrl(request.getJiraBaseUrl())
                .jiraProjectKey(request.getJiraProjectKey())
                .jiraBoardId(request.getJiraBoardId())
                .jiraAccessToken(request.getJiraAccessToken())
                .githubOwner(request.getGithubOwner())
                .githubRepo(request.getGithubRepo())
                .githubToken(request.getGithubToken())
                .build();

        IntegrationConfig saved = configRepository.save(config);

        Account actor = new SecurityUtil(accountRepository).getCurrentAccount();
        auditLogService.record(
                AuditAction.CREATE,
                "IntegrationConfig",
                saved.getId(),
                "Create integration config for group " + group.getId(),
                actor
        );

        return mapToResponse(saved);
    }

    @Override
    public IntegrationConfigResponse updateConfig(String groupId, IntegrationConfigRequest request) {
        if (!groupId.equals(request.getGroupId())) {
            throw new BadRequestException("Group mismatch");
        }

        IntegrationConfig config = configRepository.findByGroupId(groupId)
                .orElseThrow(() -> new NotFoundException("Integration config not found"));

        config.setJiraBaseUrl(request.getJiraBaseUrl());
        config.setJiraProjectKey(request.getJiraProjectKey());
        config.setJiraBoardId(request.getJiraBoardId());
        config.setJiraAccessToken(request.getJiraAccessToken());
        config.setGithubOwner(request.getGithubOwner());
        config.setGithubRepo(request.getGithubRepo());
        config.setGithubToken(request.getGithubToken());

        IntegrationConfig saved = configRepository.save(config);

        Account actor = new SecurityUtil(accountRepository).getCurrentAccount();
        auditLogService.record(
                AuditAction.UPDATE,
                "IntegrationConfig",
                saved.getId(),
                "Update integration config for group " + groupId,
                actor
        );

        return mapToResponse(saved);
    }

    @Override
    public IntegrationConfigResponse getConfig(String groupId) {
        IntegrationConfig config = configRepository.findByGroupId(groupId)
                .orElseThrow(() -> new NotFoundException("Integration config not found"));
        return mapToResponse(config);
    }

    @Override
    public SyncJobResponse sync(String groupId, SyncRequest request) {
        IntegrationConfig config = configRepository.findByGroupId(groupId)
                .orElseThrow(() -> new NotFoundException("Integration config not found"));

        validateConfig(config);

        Account actor = new SecurityUtil(accountRepository).getCurrentAccount();

        SyncJob job = SyncJob.builder()
                .group(config.getGroup())
                .type(request.getType())
                .status(SyncJobStatus.PENDING)
                .startedAt(LocalDateTime.now())
                .createdBy(actor)
                .build();

        SyncJob saved = syncJobRepository.save(job);

        saved.setStatus(SyncJobStatus.SUCCESS);
        saved.setFinishedAt(LocalDateTime.now());
        saved.setMessage("Sync completed (placeholder)");

        config.setLastSyncAt(saved.getFinishedAt());
        configRepository.save(config);

        auditLogService.record(
                AuditAction.SYNC,
                "SyncJob",
                saved.getId(),
                "Sync " + request.getType() + " for group " + groupId,
                actor
        );

        return mapToResponse(saved);
    }

    private void validateConfig(IntegrationConfig config) {
        if (isBlank(config.getJiraAccessToken())
                || isBlank(config.getJiraBaseUrl())
                || isBlank(config.getJiraProjectKey())
                || isBlank(config.getGithubToken())
                || isBlank(config.getGithubOwner())
                || isBlank(config.getGithubRepo())) {
            throw new BadRequestException("Integration config is not valid for sync");
        }
    }

    private IntegrationConfigResponse mapToResponse(IntegrationConfig config) {
        IntegrationConfigResponse response = new IntegrationConfigResponse();
        response.setId(config.getId());
        response.setGroupId(config.getGroup().getId());
        response.setJiraBaseUrl(config.getJiraBaseUrl());
        response.setJiraProjectKey(config.getJiraProjectKey());
        response.setJiraBoardId(config.getJiraBoardId());
        response.setJiraAccessTokenMasked(maskToken(config.getJiraAccessToken()));
        response.setGithubOwner(config.getGithubOwner());
        response.setGithubRepo(config.getGithubRepo());
        response.setGithubTokenMasked(maskToken(config.getGithubToken()));
        response.setLastSyncAt(config.getLastSyncAt());
        return response;
    }

    private SyncJobResponse mapToResponse(SyncJob job) {
        SyncJobResponse response = new SyncJobResponse();
        response.setId(job.getId());
        response.setGroupId(job.getGroup().getId());
        response.setType(job.getType());
        response.setStatus(job.getStatus());
        response.setStartedAt(job.getStartedAt());
        response.setFinishedAt(job.getFinishedAt());
        response.setMessage(job.getMessage());
        return response;
    }

    private String maskToken(String token) {
        if (isBlank(token)) {
            return null;
        }
        int visibleCount = Math.min(4, token.length());
        return "****" + token.substring(token.length() - visibleCount);
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}

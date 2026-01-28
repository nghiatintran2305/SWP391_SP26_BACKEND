package com.example.swp391.integrations.service.impl;

import com.example.swp391.accounts.entity.Account;
import com.example.swp391.accounts.repository.AccountRepository;
import com.example.swp391.audit.enums.AuditAction;
import com.example.swp391.audit.service.AuditLogService;
import com.example.swp391.config.security.SecurityUtil;
import com.example.swp391.exception.NotFoundException;
import com.example.swp391.integrations.dto.request.IdentityMappingRequest;
import com.example.swp391.integrations.dto.response.IdentityMappingResponse;
import com.example.swp391.integrations.entity.IdentityMapping;
import com.example.swp391.integrations.repository.IdentityMappingRepository;
import com.example.swp391.integrations.service.IIdentityMappingService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Transactional
public class IdentityMappingServiceImpl implements IIdentityMappingService {

    private final IdentityMappingRepository identityMappingRepository;
    private final AccountRepository accountRepository;
    private final AuditLogService auditLogService;

    @Override
    public IdentityMappingResponse upsertMapping(IdentityMappingRequest request) {
        Account account = accountRepository.findById(request.getAccountId())
                .orElseThrow(() -> new NotFoundException("Account not found"));

        IdentityMapping mapping = identityMappingRepository.findByAccountId(account.getId())
                .orElseGet(() -> IdentityMapping.builder().account(account).build());
        boolean isNew = mapping.getId() == null;

        mapping.setJiraAccountId(request.getJiraAccountId());
        mapping.setJiraEmail(request.getJiraEmail());
        mapping.setGithubUsername(request.getGithubUsername());
        mapping.setGithubEmail(request.getGithubEmail());

        IdentityMapping saved = identityMappingRepository.save(mapping);

        Account actor = new SecurityUtil(accountRepository).getCurrentAccount();
        AuditAction action = isNew ? AuditAction.CREATE : AuditAction.UPDATE;
        auditLogService.record(
                action,
                "IdentityMapping",
                saved.getId(),
                "Upsert identity mapping for account " + account.getId(),
                actor
        );

        return mapToResponse(saved);
    }

    @Override
    public IdentityMappingResponse getMapping(String accountId) {
        IdentityMapping mapping = identityMappingRepository.findByAccountId(accountId)
                .orElseThrow(() -> new NotFoundException("Identity mapping not found"));
        return mapToResponse(mapping);
    }

    private IdentityMappingResponse mapToResponse(IdentityMapping mapping) {
        IdentityMappingResponse response = new IdentityMappingResponse();
        response.setId(mapping.getId());
        response.setAccountId(mapping.getAccount().getId());
        response.setJiraAccountId(mapping.getJiraAccountId());
        response.setJiraEmail(mapping.getJiraEmail());
        response.setGithubUsername(mapping.getGithubUsername());
        response.setGithubEmail(mapping.getGithubEmail());
        return response;
    }
}

package com.example.swp391.integrations.service.impl;

import com.example.swp391.accounts.entity.Account;
import com.example.swp391.accounts.repository.AccountRepository;
import com.example.swp391.config.security.SecurityUtil;
import com.example.swp391.exception.NotFoundException;
import com.example.swp391.integrations.dto.request.IdentityMappingRequest;
import com.example.swp391.integrations.dto.request.IdentityMappingSelfRequest;
import com.example.swp391.integrations.dto.response.IdentityMappingResponse;
import com.example.swp391.integrations.entity.IdentityMapping;
import com.example.swp391.integrations.repository.IdentityMappingRepository;
import com.example.swp391.integrations.service.IIdentityMappingService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Identity mapping service.
 *
 * Supports:
 * - Admin/Lecturer upsert mapping for any account.
 * - Self-service (Student/any authenticated user) upsert mapping for current account.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class IdentityMappingServiceImpl implements IIdentityMappingService {

    private final IdentityMappingRepository identityMappingRepository;
    private final AccountRepository accountRepository;
    private final SecurityUtil securityUtil;

    @Override
    public IdentityMappingResponse upsertMapping(IdentityMappingRequest request) {
        Account account = accountRepository.findById(request.getAccountId())
                .orElseThrow(() -> new NotFoundException("Account not found"));

        IdentityMapping mapping = identityMappingRepository.findByAccountId(account.getId())
                .orElseGet(() -> IdentityMapping.builder().account(account).build());

        mapping.setJiraAccountId(request.getJiraAccountId());
        mapping.setJiraEmail(request.getJiraEmail());
        mapping.setGithubUsername(request.getGithubUsername());
        mapping.setGithubEmail(request.getGithubEmail());

        IdentityMapping saved = identityMappingRepository.save(mapping);
        return mapToResponse(saved);
    }

    @Override
    public IdentityMappingResponse getMapping(String accountId) {
        IdentityMapping mapping = identityMappingRepository.findByAccountId(accountId)
                .orElseThrow(() -> new NotFoundException("Identity mapping not found"));
        return mapToResponse(mapping);
    }

    @Override
    public IdentityMappingResponse getMyMapping() {
        Account current = securityUtil.getCurrentAccount();
        // If no mapping exists yet, return an empty DTO so FE can render "Unmapped" state.
        return identityMappingRepository.findByAccountId(current.getId())
                .map(this::mapToResponse)
                .orElseGet(() -> {
                    IdentityMappingResponse res = new IdentityMappingResponse();
                    res.setAccountId(current.getId());
                    return res;
                });
    }

    @Override
    public IdentityMappingResponse upsertMyMapping(IdentityMappingSelfRequest request) {
        Account current = securityUtil.getCurrentAccount();

        // Never trust FE to provide accountId.
        IdentityMappingRequest safeReq = new IdentityMappingRequest();
        safeReq.setAccountId(current.getId());
        safeReq.setJiraAccountId(request.getJiraAccountId());
        safeReq.setJiraEmail(request.getJiraEmail());
        safeReq.setGithubUsername(request.getGithubUsername());
        safeReq.setGithubEmail(request.getGithubEmail());

        return upsertMapping(safeReq);
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

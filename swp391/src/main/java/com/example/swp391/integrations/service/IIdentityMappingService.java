package com.example.swp391.integrations.service;

import com.example.swp391.integrations.dto.request.IdentityMappingRequest;
import com.example.swp391.integrations.dto.request.IdentityMappingSelfRequest;
import com.example.swp391.integrations.dto.response.IdentityMappingResponse;

public interface IIdentityMappingService {
    IdentityMappingResponse upsertMapping(IdentityMappingRequest request);
    IdentityMappingResponse getMapping(String accountId);

    /**
     * Self-service: get mapping for current logged-in account.
     */
    IdentityMappingResponse getMyMapping();

    /**
     * Self-service: upsert mapping for current logged-in account.
     * NOTE: request.accountId is ignored and overridden.
     */
    IdentityMappingResponse upsertMyMapping(IdentityMappingSelfRequest request);
}

package com.example.swp391.integrations.service;

import com.example.swp391.integrations.dto.request.IdentityMappingRequest;
import com.example.swp391.integrations.dto.response.IdentityMappingResponse;

public interface IIdentityMappingService {
    IdentityMappingResponse upsertMapping(IdentityMappingRequest request);
    IdentityMappingResponse getMapping(String accountId);
}

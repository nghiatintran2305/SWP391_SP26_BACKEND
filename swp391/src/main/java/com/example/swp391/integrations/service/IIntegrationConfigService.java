package com.example.swp391.integrations.service;

import com.example.swp391.integrations.dto.request.IntegrationConfigRequest;
import com.example.swp391.integrations.dto.request.SyncRequest;
import com.example.swp391.integrations.dto.response.IntegrationConfigResponse;
import com.example.swp391.integrations.dto.response.SyncJobResponse;

public interface IIntegrationConfigService {
    IntegrationConfigResponse createConfig(IntegrationConfigRequest request);
    IntegrationConfigResponse updateConfig(String groupId, IntegrationConfigRequest request);
    IntegrationConfigResponse getConfig(String groupId);
    SyncJobResponse sync(String groupId, SyncRequest request);
}

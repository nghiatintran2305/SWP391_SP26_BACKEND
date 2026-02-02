package com.example.swp391.integrations.controller;

import com.example.swp391.integrations.dto.request.IntegrationConfigRequest;
import com.example.swp391.integrations.dto.request.SyncRequest;
import com.example.swp391.integrations.dto.response.IntegrationConfigResponse;
import com.example.swp391.integrations.dto.response.SyncJobResponse;
import com.example.swp391.integrations.service.IIntegrationConfigService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/integrations")
@RequiredArgsConstructor
public class IntegrationConfigController {

    private final IIntegrationConfigService integrationConfigService;

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/configs")
    public ResponseEntity<IntegrationConfigResponse> createConfig(
            @Valid @RequestBody IntegrationConfigRequest request
    ) {
        IntegrationConfigResponse response = integrationConfigService.createConfig(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/configs/{groupId}")
    public ResponseEntity<IntegrationConfigResponse> updateConfig(
            @PathVariable String groupId,
            @Valid @RequestBody IntegrationConfigRequest request
    ) {
        IntegrationConfigResponse response = integrationConfigService.updateConfig(groupId, request);
        return ResponseEntity.ok(response);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'LECTURER', 'STUDENT')")
    @GetMapping("/configs/{groupId}")
    public ResponseEntity<IntegrationConfigResponse> getConfig(
            @PathVariable String groupId
    ) {
        return ResponseEntity.ok(integrationConfigService.getConfig(groupId));
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'LECTURER', 'STUDENT')")
    @PostMapping("/configs/{groupId}/sync")
    public ResponseEntity<SyncJobResponse> sync(
            @PathVariable String groupId,
            @Valid @RequestBody SyncRequest request
    ) {
        return ResponseEntity.ok(integrationConfigService.sync(groupId, request));
    }
}

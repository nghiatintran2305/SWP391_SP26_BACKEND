package com.example.swp391.integrations.controller;

import com.example.swp391.integrations.dto.request.IdentityMappingRequest;
import com.example.swp391.integrations.dto.response.IdentityMappingResponse;
import com.example.swp391.integrations.service.IIdentityMappingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/identity-mappings")
@RequiredArgsConstructor
public class IdentityMappingController {

    private final IIdentityMappingService identityMappingService;

    @PreAuthorize("hasAnyRole('ADMIN', 'LECTURER')")
    @PostMapping
    public ResponseEntity<IdentityMappingResponse> upsertMapping(
            @Valid @RequestBody IdentityMappingRequest request
    ) {
        return ResponseEntity.ok(identityMappingService.upsertMapping(request));
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'LECTURER')")
    @GetMapping("/{accountId}")
    public ResponseEntity<IdentityMappingResponse> getMapping(
            @PathVariable String accountId
    ) {
        return ResponseEntity.ok(identityMappingService.getMapping(accountId));
    }
}

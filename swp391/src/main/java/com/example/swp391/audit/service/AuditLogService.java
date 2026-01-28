package com.example.swp391.audit.service;

import com.example.swp391.accounts.entity.Account;
import com.example.swp391.audit.entity.AuditLog;
import com.example.swp391.audit.enums.AuditAction;
import com.example.swp391.audit.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;

    public AuditLog record(
            AuditAction action,
            String entityType,
            String entityId,
            String description,
            Account createdBy
    ) {
        AuditLog log = AuditLog.builder()
                .action(action)
                .entityType(entityType)
                .entityId(entityId)
                .description(description)
                .createdBy(createdBy)
                .build();
        return auditLogRepository.save(log);
    }
}

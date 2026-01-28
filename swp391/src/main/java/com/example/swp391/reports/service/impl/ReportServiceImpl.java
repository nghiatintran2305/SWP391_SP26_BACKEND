package com.example.swp391.reports.service.impl;

import com.example.swp391.accounts.entity.Account;
import com.example.swp391.accounts.repository.AccountRepository;
import com.example.swp391.audit.enums.AuditAction;
import com.example.swp391.audit.service.AuditLogService;
import com.example.swp391.config.security.SecurityUtil;
import com.example.swp391.exception.NotFoundException;
import com.example.swp391.projects.entity.ProjectGroup;
import com.example.swp391.projects.repository.ProjectGroupRepository;
import com.example.swp391.reports.dto.request.ReportRequest;
import com.example.swp391.reports.dto.response.ReportResponse;
import com.example.swp391.reports.entity.Report;
import com.example.swp391.reports.repository.ReportRepository;
import com.example.swp391.reports.service.IReportService;
import jakarta.transaction.Transactional;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Transactional
public class ReportServiceImpl implements IReportService {

    private final ReportRepository reportRepository;
    private final ProjectGroupRepository projectGroupRepository;
    private final AccountRepository accountRepository;
    private final AuditLogService auditLogService;

    @Override
    public ReportResponse createReport(ReportRequest request) {
        ProjectGroup group = projectGroupRepository.findById(request.getGroupId())
                .orElseThrow(() -> new NotFoundException("Group not found"));

        Account actor = new SecurityUtil(accountRepository).getCurrentAccount();

        Report report = Report.builder()
                .group(group)
                .type(request.getType())
                .periodStart(request.getPeriodStart())
                .periodEnd(request.getPeriodEnd())
                .generatedBy(actor)
                .fileUrl(request.getFileUrl())
                .payload(request.getPayload())
                .build();

        Report saved = reportRepository.save(report);

        auditLogService.record(
                AuditAction.CREATE,
                "Report",
                saved.getId(),
                "Generate report " + request.getType() + " for group " + group.getId(),
                actor
        );

        return mapToResponse(saved);
    }

    @Override
    public List<ReportResponse> getReportsByGroup(String groupId) {
        return reportRepository.findByGroupId(groupId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    private ReportResponse mapToResponse(Report report) {
        ReportResponse response = new ReportResponse();
        response.setId(report.getId());
        response.setGroupId(report.getGroup().getId());
        response.setType(report.getType());
        response.setPeriodStart(report.getPeriodStart());
        response.setPeriodEnd(report.getPeriodEnd());
        response.setGeneratedById(report.getGeneratedBy().getId());
        response.setFileUrl(report.getFileUrl());
        response.setPayload(report.getPayload());
        response.setCreatedAt(report.getCreatedAt());
        return response;
    }
}

package com.example.swp391.reports.service;

import com.example.swp391.reports.dto.request.ReportRequest;
import com.example.swp391.reports.dto.response.ReportResponse;
import java.util.List;

public interface IReportService {
    ReportResponse createReport(ReportRequest request);
    List<ReportResponse> getReportsByGroup(String groupId);
}

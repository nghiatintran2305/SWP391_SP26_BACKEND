package com.example.swp391.projects.service;

public interface ProjectReportExportService {

    ExportedProjectReport exportMembersReport(String projectId, String format);

    record ExportedProjectReport(
            String filename,
            String contentType,
            byte[] content
    ) {}
}

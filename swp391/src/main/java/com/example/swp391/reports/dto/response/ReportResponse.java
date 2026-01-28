package com.example.swp391.reports.dto.response;

import com.example.swp391.reports.enums.ReportType;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ReportResponse {

    private String id;
    private String groupId;
    private ReportType type;
    private LocalDate periodStart;
    private LocalDate periodEnd;
    private String generatedById;
    private String fileUrl;
    private String payload;
    private LocalDateTime createdAt;
}

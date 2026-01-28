package com.example.swp391.reports.dto.request;

import com.example.swp391.reports.enums.ReportType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ReportRequest {

    @NotBlank
    private String groupId;

    @NotNull
    private ReportType type;

    private LocalDate periodStart;

    private LocalDate periodEnd;

    private String fileUrl;

    private String payload;
}

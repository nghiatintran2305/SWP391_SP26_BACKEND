package com.example.swp391.srs.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProjectSrsResponse {
    private boolean exists;
    private boolean editable;
    private String id;
    private String projectId;
    private String projectName;
    private String title;
    private String content;
    private String createdById;
    private String createdByUsername;
    private String createdByFullName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

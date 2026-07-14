package com.example.swp391.projects.dto.response;

import com.example.swp391.projects.enums.MemberFeedbackRating;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MemberFeedbackResponse {
    private String id;
    private String projectId;
    private String projectName;
    private String studentId;
    private String studentUsername;
    private String studentFullName;
    private String leaderId;
    private String leaderUsername;
    private String leaderFullName;
    private String leaderFeedback;
    private MemberFeedbackRating lecturerRating;
    private String lecturerComment;
    private String reviewedById;
    private String reviewedByUsername;
    private String reviewedByFullName;
    private LocalDateTime reviewedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

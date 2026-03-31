package com.example.swp391.projects.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubmitMemberFeedbackRequest {

    @NotBlank
    private String studentId;

    @NotBlank
    private String feedback;
}

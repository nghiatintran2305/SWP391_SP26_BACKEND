package com.example.swp391.projects.dto.request;

import com.example.swp391.projects.enums.GroupStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateProjectGroupRequest {

    @NotBlank(message = "Group name is required")
    @Size(min = 3, max = 100, message = "Group name must be between 3 and 100 characters")
    private String groupName;

    @NotBlank(message = "Semester is required")
    @Pattern(regexp = "^(Spring|Summer|Fall)\\s\\d{4}$",
            message = "Semester must be in format 'Spring 2024'")
    private String semester;

    @NotBlank(message = "Lecturer id is required")
    @Pattern(
            regexp = "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$",
            message = "Lecturer ID must be a valid UUID (36 characters)"
    )
    private String lecturerId;
}

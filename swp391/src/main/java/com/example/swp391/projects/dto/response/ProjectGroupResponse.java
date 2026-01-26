package com.example.swp391.projects.dto.response;

import com.example.swp391.projects.enums.GroupStatus;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class ProjectGroupResponse {

    private String id;
    private String groupName;
    private String semester;

    private String lecturerId;
    private String lecturerUsername;
    private String lecturerEmail;

    private GroupStatus status;

    private LocalDateTime createdAt;
}

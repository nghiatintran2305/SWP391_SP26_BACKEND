package com.example.swp391.projects.dto.response;

import com.example.swp391.projects.enums.GroupRole;
import com.example.swp391.projects.enums.GroupStatus;
import lombok.Getter;
import lombok.Setter;

/**
 * Group card data for the current user.
 *
 * FE usage:
 * - show list "My Groups"; render lecturer info and my role (leader/member).
 */
@Getter
@Setter
public class MyProjectGroupResponse {
    private String id;
    private String groupName;
    private String semester;

    private String lecturerId;
    private String lecturerUsername;
    private String lecturerEmail;

    private GroupStatus status;

    // Only meaningful for students. Admin/Lecturer can ignore this.
    private GroupRole myRoleInGroup;
}

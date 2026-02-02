package com.example.swp391.projects.dto.response;

import com.example.swp391.projects.enums.GroupRole;
import com.example.swp391.projects.enums.GroupStatus;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

/**
 * Group detail screen data.
 */
@Getter
@Setter
public class GroupDetailResponse {
    private String id;
    private String groupName;
    private String semester;

    private String lecturerId;
    private String lecturerUsername;
    private String lecturerEmail;

    private GroupStatus status;

    // Current user's role in this group (nullable for admin/lecturer)
    private GroupRole myRoleInGroup;

    // Member list is included for convenience.
    private List<GroupMemberResponse> members;
}

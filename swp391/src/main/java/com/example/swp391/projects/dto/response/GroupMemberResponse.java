package com.example.swp391.projects.dto.response;

import com.example.swp391.projects.enums.GroupRole;
import lombok.Getter;
import lombok.Setter;

/**
 * Member row in group member list.
 */
@Getter
@Setter
public class GroupMemberResponse {
    private String id;
    private String groupId;
    private String accountId;
    private String username;
    private String email;
    private GroupRole roleInGroup;
}

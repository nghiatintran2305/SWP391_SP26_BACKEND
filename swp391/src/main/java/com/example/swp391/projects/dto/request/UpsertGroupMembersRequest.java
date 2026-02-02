package com.example.swp391.projects.dto.request;

import com.example.swp391.projects.enums.GroupRole;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

/**
 * Request body for adding/upserting members in a group.
 *
 * Example:
 * {
 *   "members": [
 *     { "accountId": "...", "roleInGroup": "LEADER" },
 *     { "accountId": "...", "roleInGroup": "MEMBER" }
 *   ]
 * }
 */
@Getter
@Setter
public class UpsertGroupMembersRequest {

    @NotEmpty
    @Valid
    private List<MemberItem> members;

    @Getter
    @Setter
    public static class MemberItem {
        @NotNull
        private String accountId;

        @NotNull
        private GroupRole roleInGroup;
    }
}

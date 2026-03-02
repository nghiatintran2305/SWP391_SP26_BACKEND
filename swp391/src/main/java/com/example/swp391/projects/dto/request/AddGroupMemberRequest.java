package com.example.swp391.projects.dto.request;

import com.example.swp391.projects.enums.GroupRole;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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
public class AddGroupMemberRequest {

    @NotBlank
    private String accountId;

    @NotNull
    private GroupRole role;
}

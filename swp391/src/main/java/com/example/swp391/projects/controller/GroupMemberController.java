package com.example.swp391.projects.controller;

import com.example.swp391.projects.dto.request.AddGroupMemberRequest;
import com.example.swp391.projects.service.IGroupMemberService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/project-groups/{groupId}/members")
@RequiredArgsConstructor
public class GroupMemberController {

    private final IGroupMemberService groupMemberService;

    @PreAuthorize("hasAnyRole('ADMIN','LECTURER')")
    @PostMapping
    public ResponseEntity<Void> addMember(
            @PathVariable String groupId,
            @Valid @RequestBody AddGroupMemberRequest request
    ) {
        groupMemberService.addMemberToGroup(
                groupId,
                request.getAccountId(),
                request.getRole()
        );
        return ResponseEntity.ok().build();
    }


    @PreAuthorize("hasAnyRole('ADMIN','LECTURER')")
    @DeleteMapping("/{accountId}")
    public ResponseEntity<Void> removeMember(
            @PathVariable String groupId,
            @PathVariable String accountId
    ) {
        groupMemberService.removeMemberFromGroup(groupId, accountId);
        return ResponseEntity.noContent().build();
    }
}

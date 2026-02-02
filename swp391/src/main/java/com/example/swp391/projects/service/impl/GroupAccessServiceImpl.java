package com.example.swp391.projects.service.impl;

import com.example.swp391.accounts.entity.Account;
import com.example.swp391.exception.NotFoundException;
import com.example.swp391.exception.UnauthorizedException;
import com.example.swp391.projects.entity.GroupMember;
import com.example.swp391.projects.entity.ProjectGroup;
import com.example.swp391.projects.enums.GroupRole;
import com.example.swp391.projects.repository.GroupMemberRepository;
import com.example.swp391.projects.repository.ProjectGroupRepository;
import com.example.swp391.projects.service.GroupAccessService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Default implementation of group authorization.
 */
@Service
@RequiredArgsConstructor
public class GroupAccessServiceImpl implements GroupAccessService {

    private final ProjectGroupRepository projectGroupRepository;
    private final GroupMemberRepository groupMemberRepository;

    @Override
    public void assertCanReadGroup(String groupId, Account account) {
        // Validate group exists first (avoid leaking access detail).
        ProjectGroup group = projectGroupRepository.findById(groupId)
                .orElseThrow(() -> new NotFoundException("Group not found"));

        String role = account.getRole() != null ? account.getRole().getName() : null;
        if ("ADMIN".equals(role)) {
            return;
        }
        if ("LECTURER".equals(role)) {
            if (group.getLecturer() != null && group.getLecturer().getId().equals(account.getId())) {
                return;
            }
            throw new UnauthorizedException("You are not assigned to this group");
        }

        // STUDENT (or any other role) must be a group member.
        boolean isMember = groupMemberRepository.existsByGroupIdAndAccountId(groupId, account.getId());
        if (!isMember) {
            throw new UnauthorizedException("You are not a member of this group");
        }
    }

    @Override
    public void assertIsLeader(String groupId, Account account) {
        assertCanReadGroup(groupId, account);

        // ADMIN or group lecturer can be treated as leader for management operations.
        String role = account.getRole() != null ? account.getRole().getName() : null;
        if ("ADMIN".equals(role)) {
            return;
        }
        if ("LECTURER".equals(role)) {
            return;
        }

        GroupMember membership = groupMemberRepository
                .findByGroupIdAndAccountId(groupId, account.getId())
                .orElseThrow(() -> new UnauthorizedException("You are not a member of this group"));

        if (membership.getRoleInGroup() != GroupRole.LEADER) {
            throw new UnauthorizedException("Leader role required");
        }
    }
}

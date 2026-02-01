package com.example.swp391.projects.service.impl;

import com.example.swp391.accounts.entity.Account;
import com.example.swp391.accounts.repository.AccountRepository;
import com.example.swp391.config.security.SecurityUtil;
import com.example.swp391.exception.BadRequestException;
import com.example.swp391.exception.NotFoundException;
import com.example.swp391.projects.dto.request.CreateProjectGroupRequest;
import com.example.swp391.projects.dto.request.UpsertGroupMembersRequest;
import com.example.swp391.projects.dto.response.GroupDetailResponse;
import com.example.swp391.projects.dto.response.GroupMemberResponse;
import com.example.swp391.projects.dto.response.MyProjectGroupResponse;
import com.example.swp391.projects.dto.response.ProjectGroupResponse;
import com.example.swp391.projects.entity.GroupMember;
import com.example.swp391.projects.entity.ProjectGroup;
import com.example.swp391.projects.enums.GroupRole;
import com.example.swp391.projects.enums.GroupStatus;
import com.example.swp391.projects.repository.GroupMemberRepository;
import com.example.swp391.projects.repository.ProjectGroupRepository;
import com.example.swp391.projects.service.IProjectGroupService;
import com.example.swp391.projects.service.GroupAccessService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class ProjectGroupServiceImpl implements IProjectGroupService {

    private final ProjectGroupRepository projectGroupRepository;
    private final AccountRepository accountRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final GroupAccessService groupAccessService;

    @Override
    public ProjectGroupResponse createProjectGroup(CreateProjectGroupRequest request) {

        Account lecturer = accountRepository.findById(request.getLecturerId())
                .orElseThrow(() -> new NotFoundException("Lecturer not found"));

        if (!"LECTURER".equals(lecturer.getRole().getName())) {
            throw new BadRequestException("Assigned account is not a lecturer");
        }

        boolean exists = projectGroupRepository
                .existsByGroupNameAndSemester(
                        request.getGroupName(),
                        request.getSemester()
                );

        if (exists) {
            throw new BadRequestException("Group name already exists in this semester");
        }

        Account admin = new SecurityUtil(accountRepository).getCurrentAccount();

        ProjectGroup group = ProjectGroup.builder()
                .groupName(request.getGroupName())
                .semester(request.getSemester())
                .lecturer(lecturer)
                .createdBy(admin)
                .status(GroupStatus.OPEN)
                .build();

        ProjectGroup savedGroup = projectGroupRepository.save(group);

        return mapToResponse(savedGroup);
    }

    @Override
    public List<MyProjectGroupResponse> getMyGroups() {
        Account current = new SecurityUtil(accountRepository).getCurrentAccount();
        String role = current.getRole() != null ? current.getRole().getName() : null;

        // ADMIN: show all groups.
        if ("ADMIN".equals(role)) {
            return projectGroupRepository.findAll().stream()
                    .map(g -> mapToMyGroupResponse(g, null))
                    .sorted(Comparator.comparing(MyProjectGroupResponse::getSemester).reversed())
                    .collect(Collectors.toList());
        }

        // LECTURER: show groups assigned to lecturer.
        if ("LECTURER".equals(role)) {
            return projectGroupRepository.findByLecturerId(current.getId()).stream()
                    .map(g -> mapToMyGroupResponse(g, null))
                    .sorted(Comparator.comparing(MyProjectGroupResponse::getSemester).reversed())
                    .collect(Collectors.toList());
        }

        // STUDENT: show groups where user is a member.
        List<GroupMember> memberships = groupMemberRepository.findByAccountId(current.getId());
        List<MyProjectGroupResponse> res = new ArrayList<>();
        for (GroupMember m : memberships) {
            ProjectGroup g = m.getGroup();
            res.add(mapToMyGroupResponse(g, m.getRoleInGroup()));
        }

        res.sort(Comparator.comparing(MyProjectGroupResponse::getSemester).reversed());
        return res;
    }

    @Override
    public GroupDetailResponse getGroupDetail(String groupId) {
        Account current = new SecurityUtil(accountRepository).getCurrentAccount();
        groupAccessService.assertCanReadGroup(groupId, current);

        ProjectGroup group = projectGroupRepository.findById(groupId)
                .orElseThrow(() -> new NotFoundException("Group not found"));

        GroupRole myRole = groupMemberRepository.findByGroupIdAndAccountId(groupId, current.getId())
                .map(GroupMember::getRoleInGroup)
                .orElse(null);

        GroupDetailResponse res = new GroupDetailResponse();
        res.setId(group.getId());
        res.setGroupName(group.getGroupName());
        res.setSemester(group.getSemester());
        res.setLecturerId(group.getLecturer().getId());
        res.setLecturerUsername(group.getLecturer().getUsername());
        res.setLecturerEmail(group.getLecturer().getEmail());
        res.setStatus(group.getStatus());
        res.setMyRoleInGroup(myRole);
        res.setMembers(getGroupMembers(groupId));
        return res;
    }

    @Override
    public List<GroupMemberResponse> getGroupMembers(String groupId) {
        Account current = new SecurityUtil(accountRepository).getCurrentAccount();
        groupAccessService.assertCanReadGroup(groupId, current);

        return groupMemberRepository.findByGroupId(groupId)
                .stream()
                .map(this::mapToMemberResponse)
                .sorted(Comparator.comparing(GroupMemberResponse::getUsername, String.CASE_INSENSITIVE_ORDER))
                .collect(Collectors.toList());
    }

    @Override
    public List<GroupMemberResponse> upsertMembers(String groupId, UpsertGroupMembersRequest request) {
        // Admin-only operations are enforced on controller via @PreAuthorize.
        ProjectGroup group = projectGroupRepository.findById(groupId)
                .orElseThrow(() -> new NotFoundException("Group not found"));

        // NOTE: We do not delete old members here ("upsert" = add if absent, update role if exists).
        for (UpsertGroupMembersRequest.MemberItem item : request.getMembers()) {
            Account account = accountRepository.findById(item.getAccountId())
                    .orElseThrow(() -> new NotFoundException("Account not found: " + item.getAccountId()));

            GroupMember membership = groupMemberRepository.findByGroupIdAndAccountId(groupId, account.getId())
                    .orElseGet(() -> GroupMember.builder()
                            .group(group)
                            .account(account)
                            .build());

            membership.setRoleInGroup(item.getRoleInGroup());
            groupMemberRepository.save(membership);
        }

        return getGroupMembers(groupId);
    }

    private ProjectGroupResponse mapToResponse(ProjectGroup group) {
        ProjectGroupResponse res = new ProjectGroupResponse();
        res.setId(group.getId());
        res.setGroupName(group.getGroupName());
        res.setSemester(group.getSemester());

        res.setLecturerId(group.getLecturer().getId());
        res.setLecturerUsername(group.getLecturer().getUsername());
        res.setLecturerEmail(group.getLecturer().getEmail());

        res.setStatus(group.getStatus());
        return res;
    }

    private MyProjectGroupResponse mapToMyGroupResponse(ProjectGroup group, GroupRole myRole) {
        MyProjectGroupResponse res = new MyProjectGroupResponse();
        res.setId(group.getId());
        res.setGroupName(group.getGroupName());
        res.setSemester(group.getSemester());
        res.setLecturerId(group.getLecturer().getId());
        res.setLecturerUsername(group.getLecturer().getUsername());
        res.setLecturerEmail(group.getLecturer().getEmail());
        res.setStatus(group.getStatus());
        res.setMyRoleInGroup(myRole);
        return res;
    }

    private GroupMemberResponse mapToMemberResponse(GroupMember m) {
        GroupMemberResponse res = new GroupMemberResponse();
        res.setId(m.getId());
        res.setGroupId(m.getGroup().getId());
        res.setAccountId(m.getAccount().getId());
        res.setUsername(m.getAccount().getUsername());
        res.setEmail(m.getAccount().getEmail());
        res.setRoleInGroup(m.getRoleInGroup());
        return res;
    }

}

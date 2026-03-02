package com.example.swp391.projects.service.impl;

import com.example.swp391.accounts.entity.Account;
import com.example.swp391.accounts.repository.AccountRepository;
import com.example.swp391.github.enums.GithubLinkStatus;
import com.example.swp391.github.repository.GithubUserMappingRepository;
import com.example.swp391.github.service.IGithubService;
import com.example.swp391.jira.enums.JiraLinkStatus;
import com.example.swp391.jira.repository.JiraUserMappingRepository;
import com.example.swp391.jira.service.IJiraService;
import com.example.swp391.projects.entity.Group;
import com.example.swp391.projects.entity.GroupMember;
import com.example.swp391.projects.enums.GroupRole;
import com.example.swp391.projects.repository.GroupMemberRepository;
import com.example.swp391.projects.repository.GroupRepository;
import com.example.swp391.projects.service.IGroupMemberService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class GroupMemberServiceImpl implements IGroupMemberService {

    private final GroupRepository groupRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final AccountRepository accountRepository;
    private final IJiraService jiraService;
    private final IGithubService githubService;
    private final JiraUserMappingRepository jiraUserMappingRepository;
    private final GithubUserMappingRepository githubUserMappingRepository;

    @Transactional
    @Override
    public void addMemberToGroup(String groupId, String accountId, GroupRole role) {

        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new RuntimeException("Group not found"));

        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new RuntimeException("Account not found"));

        // check duplicate
        boolean exists = groupMemberRepository.existsByGroupIdAndAccountId(groupId, accountId);
        if (exists) {
            throw new RuntimeException("User already in group");
        }

        // save DB first
        GroupMember member = GroupMember.builder()
                .group(group)
                .account(account)
                .roleInGroup(role)
                .build();

        groupMemberRepository.save(member);

        //  SYNC JIRA
        jiraUserMappingRepository.findByAccountId(accountId)
                .filter(m -> m.getStatus() == JiraLinkStatus.LINKED)
                .ifPresent(mapping ->
                        jiraService.addUserToGroup(
                                mapping.getJiraAccountId(),
                                group.getJiraGroupName()
                        )
                );

        // SYNC GITHUB
        githubUserMappingRepository.findByAccountId(accountId)
                .filter(m -> m.getStatus() == GithubLinkStatus.LINKED)
                .ifPresent(mapping ->
                        githubService.addMemberToTeam(
                                mapping.getGithubUsername(),
                                group.getGithubTeamSlug()
                        )
                );
    }

    @Transactional
    @Override
    public void removeMemberFromGroup(String groupId, String accountId) {

        GroupMember member = groupMemberRepository
                .findByGroupIdAndAccountId(groupId, accountId)
                .orElseThrow(() -> new RuntimeException("Member not found"));

        Group group = member.getGroup();

        groupMemberRepository.delete(member);

        // SYNC JIRA
        jiraUserMappingRepository.findByAccountId(accountId)
                .filter(m -> m.getStatus() == JiraLinkStatus.LINKED)
                .ifPresent(mapping ->
                        jiraService.removeUserFromGroup(
                                mapping.getJiraAccountId(),
                                group.getJiraGroupName()
                        )
                );

        // SYNC GITHUB
        githubUserMappingRepository.findByAccountId(accountId)
                .filter(m -> m.getStatus() == GithubLinkStatus.LINKED)
                .ifPresent(mapping ->
                        githubService.removeMemberFromTeam(
                                mapping.getGithubUsername(),
                                group.getGithubTeamSlug()
                        )
                );
    }
}

package com.example.swp391.projects.service.impl;

import com.example.swp391.accounts.entity.Account;
import com.example.swp391.accounts.repository.AccountRepository;
import com.example.swp391.config.security.SecurityUtil;
import com.example.swp391.exception.BadRequestException;
import com.example.swp391.github.service.IGithubService;
import com.example.swp391.jira.service.IJiraService;
import com.example.swp391.projects.dto.request.CreateProjectGroupRequest;
import com.example.swp391.projects.dto.response.ProjectGroupResponse;
import com.example.swp391.projects.entity.Group;
import com.example.swp391.projects.enums.GroupStatus;
import com.example.swp391.projects.repository.GroupRepository;
import com.example.swp391.projects.service.IProjectGroupService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Transactional
public class ProjectGroupServiceImpl implements IProjectGroupService {

    private final GroupRepository groupRepository;
    private final AccountRepository accountRepository;
    private final IJiraService jiraService;
    private final IGithubService githubService;

    @Override
    public ProjectGroupResponse createProjectGroup(CreateProjectGroupRequest req) {

        // check DB
        if (groupRepository.existsByJiraGroupName(req.getJiraGroupName())) {
            throw new BadRequestException("Jira group name already used in system");
        }

        if (groupRepository.existsByGithubTeamSlug(req.getGithubTeamName())) {
            throw new BadRequestException("GitHub team name already used in system");
        }

        Account admin = new SecurityUtil(accountRepository).getCurrentAccount();

        String jiraGroupName = null;
        String githubTeamSlug = null;

        try {

            // create Jira group
            jiraGroupName = jiraService.createGroup(req.getJiraGroupName());

            // create GitHub team
            githubTeamSlug = githubService.createTeam(
                    req.getGithubTeamName(),
                    "Team for " + req.getGroupName()
            );

            // save DB
            Group entity = Group.builder()
                    .groupName(req.getGroupName())
                    .createdBy(admin)
                    .jiraGroupName(jiraGroupName)
                    .githubTeamSlug(githubTeamSlug)
                    .status(GroupStatus.ACTIVE)
                    .build();

            groupRepository.save(entity);

            return mapToResponse(entity);

        } catch (Exception ex) {

            // rollback external
            if (jiraGroupName != null) {
                jiraService.deleteGroupQuietly(jiraGroupName);
            }

            if (githubTeamSlug != null) {
                githubService.deleteTeamQuietly(githubTeamSlug); //dùng slug
            }

            throw ex;
        }
    }

    private ProjectGroupResponse mapToResponse(Group group) {
        ProjectGroupResponse res = new ProjectGroupResponse();
        res.setId(group.getId());
        res.setGroupName(group.getGroupName());
        res.setJiraGroupName(group.getJiraGroupName());
        res.setGithubTeamName(group.getGithubTeamName());
        res.setGithubTeamSlug(group.getGithubTeamSlug());
        res.setStatus(group.getStatus());
        return res;
    }
}

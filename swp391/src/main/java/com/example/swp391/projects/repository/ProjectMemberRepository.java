package com.example.swp391.projects.repository;

import com.example.swp391.accounts.entity.Account;
import com.example.swp391.projects.entity.ProjectMember;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import com.example.swp391.projects.enums.ProjectRole;
import com.example.swp391.projects.enums.ProjectStatus;
import org.springframework.data.jpa.repository.JpaRepository;


public interface ProjectMemberRepository extends JpaRepository<ProjectMember, String> {

    Optional<ProjectMember> findByProjectIdAndAccountId(String groupId, String accountId);

    boolean existsByProjectIdAndAccountId(String groupId, String accountId);

    boolean existsByAccountAndProject_StatusIn(
            Account account,
            Collection<ProjectStatus> statuses
    );

    boolean existsByProjectIdAndRoleInGroup(String projectId, ProjectRole role);

    boolean existsByAccountId(String accountId);
}

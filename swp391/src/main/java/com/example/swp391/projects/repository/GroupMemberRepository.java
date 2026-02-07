package com.example.swp391.projects.repository;

import com.example.swp391.accounts.entity.Account;
import com.example.swp391.projects.entity.GroupMember;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import com.example.swp391.projects.enums.GroupStatus;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repository for group membership queries.
 *
 * Business rules:
 * - BR-02: user can join multiple groups.
 * - Uniqueness is enforced per (group_id, account_id).
 */
public interface GroupMemberRepository extends JpaRepository<GroupMember, String> {

    List<GroupMember> findByAccountId(String accountId);

    List<GroupMember> findByGroupId(String groupId);

    Optional<GroupMember> findByGroupIdAndAccountId(String groupId, String accountId);

    boolean existsByGroupIdAndAccountId(String groupId, String accountId);

    boolean existsByAccountAndGroup_StatusIn(
            Account account,
            Collection<GroupStatus> statuses
    );
}

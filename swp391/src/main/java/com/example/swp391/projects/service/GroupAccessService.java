package com.example.swp391.projects.service;

import com.example.swp391.accounts.entity.Account;

/**
 * Centralized authorization checks for Group-related resources.
 *
 * We keep this logic in one place so controllers/services can be simple.
 */
public interface GroupAccessService {

    /**
     * Ensure current account can access group data (read).
     *
     * Rules:
     * - ADMIN: can access all groups.
     * - LECTURER: can access groups assigned to them.
     * - STUDENT: can access groups where they are a member.
     */
    void assertCanReadGroup(String groupId, Account account);

    /**
     * Ensure current account is a leader in the group.
     *
     * Used for leader-only operations (if you extend later).
     */
    void assertIsLeader(String groupId, Account account);
}

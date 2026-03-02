package com.example.swp391.projects.service;

import com.example.swp391.projects.enums.GroupRole;

public interface IGroupMemberService {
    void addMemberToGroup(String groupId, String accountId, GroupRole role);

    void removeMemberFromGroup(String groupId, String accountId) ;
}

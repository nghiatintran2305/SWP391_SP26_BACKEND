package com.example.swp391.jira.service;

public interface IJiraService {

    String createGroup(String groupName);

    void deleteGroupQuietly(String groupName);

    void addUserToGroup(String jiraAccountId, String groupName);

    void removeUserFromGroup(String jiraAccountId, String groupName);
}

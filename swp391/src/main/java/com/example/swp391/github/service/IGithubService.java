package com.example.swp391.github.service;

public interface IGithubService {

    String createTeam(String teamName, String description);

    void deleteTeamQuietly(String slug);

    void addMemberToTeam(String username, String teamSlug);

    void removeMemberFromTeam(String username, String teamSlug);
}

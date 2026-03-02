package com.example.swp391.projects.repository;

import com.example.swp391.projects.entity.Group;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface GroupRepository extends JpaRepository<Group, String> {
    boolean existsByGroupName(String groupName);
    boolean existsByJiraGroupName(String jiraGroupName);
    boolean existsByGithubTeamSlug(String slug);
    boolean existsByGithubTeamName(String gitlabGroupPath);

}

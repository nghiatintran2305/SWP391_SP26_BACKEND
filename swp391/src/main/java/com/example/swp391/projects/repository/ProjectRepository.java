package com.example.swp391.projects.repository;

import com.example.swp391.projects.entity.Project;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProjectRepository extends JpaRepository<Project, String> {
    boolean existsByJiraProjectKey(String jiraProjectKey);

}

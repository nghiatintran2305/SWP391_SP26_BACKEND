package com.example.swp391.projects.repository;

import com.example.swp391.projects.entity.ProjectGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProjectGroupRepository extends JpaRepository<ProjectGroup, String> {
    boolean existsByGroupNameAndSemester(String groupName, String semester);
}

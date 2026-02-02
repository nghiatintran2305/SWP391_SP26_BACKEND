package com.example.swp391.projects.repository;

import com.example.swp391.projects.entity.ProjectGroup;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProjectGroupRepository extends JpaRepository<ProjectGroup, String> {
    boolean existsByGroupNameAndSemester(String groupName, String semester);

    // Lecturer can view groups assigned to them.
    List<ProjectGroup> findByLecturerId(String lecturerId);
}

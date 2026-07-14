package com.example.swp391.srs.repository;

import com.example.swp391.srs.entity.ProjectSrs;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ProjectSrsRepository extends JpaRepository<ProjectSrs, String> {
    Optional<ProjectSrs> findByProjectId(String projectId);
}

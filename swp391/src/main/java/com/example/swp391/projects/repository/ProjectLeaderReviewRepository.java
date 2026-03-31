package com.example.swp391.projects.repository;

import com.example.swp391.projects.entity.ProjectLeaderReview;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ProjectLeaderReviewRepository extends JpaRepository<ProjectLeaderReview, String> {
    Optional<ProjectLeaderReview> findByProjectIdAndLeaderId(String projectId, String leaderId);
}

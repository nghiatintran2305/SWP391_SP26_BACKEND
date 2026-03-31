package com.example.swp391.projects.repository;

import com.example.swp391.projects.entity.MemberFeedback;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MemberFeedbackRepository extends JpaRepository<MemberFeedback, String> {
    List<MemberFeedback> findByProjectIdOrderByCreatedAtDesc(String projectId);

    Optional<MemberFeedback> findByIdAndProjectId(String feedbackId, String projectId);
}

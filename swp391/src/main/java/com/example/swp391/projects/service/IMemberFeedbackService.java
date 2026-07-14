package com.example.swp391.projects.service;

import com.example.swp391.projects.dto.request.ReviewMemberFeedbackRequest;
import com.example.swp391.projects.dto.request.SubmitMemberFeedbackRequest;
import com.example.swp391.projects.dto.response.LeaderReviewResponse;
import com.example.swp391.projects.dto.response.MemberFeedbackOverviewResponse;
import com.example.swp391.projects.dto.response.MemberFeedbackResponse;

import java.util.List;

public interface IMemberFeedbackService {
    MemberFeedbackResponse submitFeedback(String projectId, String currentUserId, SubmitMemberFeedbackRequest request);

    List<MemberFeedbackResponse> getFeedbacks(String projectId, String currentUserId);

    List<MemberFeedbackOverviewResponse> getFeedbackOverview(String projectId, String currentUserId);

    MemberFeedbackResponse reviewFeedback(
            String projectId,
            String feedbackId,
            String currentUserId,
            ReviewMemberFeedbackRequest request
    );

    LeaderReviewResponse getLeaderReview(String projectId, String currentUserId);

    LeaderReviewResponse reviewLeader(
            String projectId,
            String currentUserId,
            ReviewMemberFeedbackRequest request
    );
}

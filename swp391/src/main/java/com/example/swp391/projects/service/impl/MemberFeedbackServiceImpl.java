package com.example.swp391.projects.service.impl;

import com.example.swp391.accounts.entity.Account;
import com.example.swp391.accounts.repository.AccountRepository;
import com.example.swp391.exceptions.BadRequestException;
import com.example.swp391.exceptions.ForbiddenException;
import com.example.swp391.exceptions.NotFoundException;
import com.example.swp391.projects.dto.request.ReviewMemberFeedbackRequest;
import com.example.swp391.projects.dto.request.SubmitMemberFeedbackRequest;
import com.example.swp391.projects.dto.response.LeaderReviewResponse;
import com.example.swp391.projects.dto.response.MemberFeedbackOverviewResponse;
import com.example.swp391.projects.dto.response.MemberFeedbackResponse;
import com.example.swp391.projects.entity.MemberFeedback;
import com.example.swp391.projects.entity.Project;
import com.example.swp391.projects.entity.ProjectLeaderReview;
import com.example.swp391.projects.entity.ProjectMember;
import com.example.swp391.projects.enums.MemberFeedbackRating;
import com.example.swp391.projects.enums.ProjectRole;
import com.example.swp391.projects.repository.MemberFeedbackRepository;
import com.example.swp391.projects.repository.ProjectLeaderReviewRepository;
import com.example.swp391.projects.repository.ProjectMemberRepository;
import com.example.swp391.projects.repository.ProjectRepository;
import com.example.swp391.projects.service.IMemberFeedbackService;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class MemberFeedbackServiceImpl implements IMemberFeedbackService {

    private final MemberFeedbackRepository memberFeedbackRepository;
    private final ProjectLeaderReviewRepository projectLeaderReviewRepository;
    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final AccountRepository accountRepository;
    private final JavaMailSender mailSender;

    @Value("${app.mail.from}")
    private String fromEmail;

    @Override
    public MemberFeedbackResponse submitFeedback(String projectId, String currentUserId, SubmitMemberFeedbackRequest request) {
        Project project = getProject(projectId);
        Account leader = getAccount(currentUserId);
        ProjectMember leaderMember = getProjectMember(projectId, currentUserId);

        if (leaderMember.getRoleInGroup() != ProjectRole.LEADER) {
            throw new ForbiddenException("Only the project leader can submit member feedback.");
        }

        if (currentUserId.equals(request.getStudentId())) {
            throw new BadRequestException("The leader cannot submit feedback for themselves.");
        }

        ProjectMember targetMember = getProjectMember(projectId, request.getStudentId());
        Account student = targetMember.getAccount();
        if (!"STUDENT".equals(student.getRole().getName())) {
            throw new BadRequestException("Feedback can only be submitted for student members.");
        }

        MemberFeedback feedback = MemberFeedback.builder()
                .project(project)
                .student(student)
                .leader(leader)
                .leaderFeedback(request.getFeedback().trim())
                .lecturerRating(MemberFeedbackRating.PENDING)
                .build();

        return mapToResponse(memberFeedbackRepository.save(feedback));
    }

    @Override
    public List<MemberFeedbackResponse> getFeedbacks(String projectId, String currentUserId) {
        Project project = getProject(projectId);
        Account currentAccount = getAccount(currentUserId);
        String currentRole = currentAccount.getRole() != null ? currentAccount.getRole().getName() : null;

        List<MemberFeedback> feedbacks = safeFindFeedbacks(projectId);

        if ("ADMIN".equals(currentRole)) {
            return feedbacks.stream().map(this::mapToResponse).toList();
        }

        if ("LECTURER".equals(currentRole)) {
            ensureLecturerOwnsProject(project, currentUserId);
            return feedbacks.stream().map(this::mapToResponse).toList();
        }

        if (!"STUDENT".equals(currentRole)) {
            throw new ForbiddenException("You do not have permission to view these feedback reports.");
        }

        ProjectMember currentMember = getProjectMember(projectId, currentUserId);
        ProjectRole groupRole = currentMember.getRoleInGroup();

        return feedbacks.stream()
                .filter(feedback -> {
                    if (groupRole == ProjectRole.LEADER) {
                        return currentUserId.equals(feedback.getLeader().getId()) || currentUserId.equals(feedback.getStudent().getId());
                    }
                    return currentUserId.equals(feedback.getStudent().getId());
                })
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    public List<MemberFeedbackOverviewResponse> getFeedbackOverview(String projectId, String currentUserId) {
        Project project = getProject(projectId);
        Account currentAccount = getAccount(currentUserId);
        String currentRole = currentAccount.getRole() != null ? currentAccount.getRole().getName() : null;

        List<ProjectMember> members = projectMemberRepository.findByProjectId(projectId).stream()
                .filter(member -> member.getAccount() != null)
                .filter(member -> member.getAccount().getRole() != null)
                .filter(member -> "STUDENT".equals(member.getAccount().getRole().getName()))
                .filter(member -> member.getRoleInGroup() != ProjectRole.LEADER)
                .toList();

        Predicate<ProjectMember> visibility = member -> true;

        if ("LECTURER".equals(currentRole)) {
            ensureLecturerOwnsProject(project, currentUserId);
        } else if ("ADMIN".equals(currentRole)) {
            visibility = member -> true;
        } else if ("STUDENT".equals(currentRole)) {
            ProjectMember currentMember = getProjectMember(projectId, currentUserId);
            ProjectRole groupRole = currentMember.getRoleInGroup();
            if (groupRole == ProjectRole.LEADER) {
                visibility = member -> !currentUserId.equals(member.getAccount().getId());
            } else {
                return List.of();
            }
        } else {
            throw new ForbiddenException("You do not have permission to view these feedback reports.");
        }

        Map<String, MemberFeedback> latestFeedbackByStudentId = safeFindFeedbacks(projectId).stream()
                .sorted(Comparator.comparing(MemberFeedback::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .collect(Collectors.toMap(
                        feedback -> feedback.getStudent().getId(),
                        feedback -> feedback,
                        (first, second) -> first
                ));

        return members.stream()
                .filter(visibility)
                .map(member -> mapOverview(project, member, latestFeedbackByStudentId.get(member.getAccount().getId())))
                .toList();
    }

    @Override
    public MemberFeedbackResponse reviewFeedback(
            String projectId,
            String feedbackId,
            String currentUserId,
            ReviewMemberFeedbackRequest request
    ) {
        Project project = getProject(projectId);
        Account reviewer = getAccount(currentUserId);
        String reviewerRole = reviewer.getRole() != null ? reviewer.getRole().getName() : null;

        if (!"ADMIN".equals(reviewerRole)) {
            if (!"LECTURER".equals(reviewerRole)) {
                throw new ForbiddenException("Only lecturers can review member feedback.");
            }
            ensureLecturerOwnsProject(project, currentUserId);
        }

        MemberFeedback feedback = memberFeedbackRepository.findByIdAndProjectId(feedbackId, projectId)
                .orElseThrow(() -> new NotFoundException("Feedback report not found."));

        feedback.setLecturerRating(request.getRating());
        feedback.setLecturerComment(trimToNull(request.getLecturerComment()));
        feedback.setReviewedBy(reviewer);
        feedback.setReviewedAt(LocalDateTime.now());
        MemberFeedback saved = memberFeedbackRepository.save(feedback);
        sendLecturerEvaluationEmail(saved);
        return mapToResponse(saved);
    }

    @Override
    public LeaderReviewResponse getLeaderReview(String projectId, String currentUserId) {
        Project project = getProject(projectId);
        Account currentAccount = getAccount(currentUserId);
        String currentRole = currentAccount.getRole() != null ? currentAccount.getRole().getName() : null;
        ProjectMember leaderMember = getLeaderMember(projectId);

        if ("ADMIN".equals(currentRole)) {
            return mapToLeaderReview(project, leaderMember, findLeaderReview(projectId, leaderMember.getAccount().getId()).orElse(null));
        }

        if ("LECTURER".equals(currentRole)) {
            ensureLecturerOwnsProject(project, currentUserId);
            return mapToLeaderReview(project, leaderMember, findLeaderReview(projectId, leaderMember.getAccount().getId()).orElse(null));
        }

        if (!"STUDENT".equals(currentRole)) {
            throw new ForbiddenException("You do not have permission to view the leader review.");
        }

        ProjectMember currentMember = getProjectMember(projectId, currentUserId);
        if (currentMember.getRoleInGroup() != ProjectRole.LEADER) {
            throw new ForbiddenException("Only the project leader can view this review.");
        }

        return mapToLeaderReview(project, leaderMember, findLeaderReview(projectId, currentUserId).orElse(null));
    }

    @Override
    public LeaderReviewResponse reviewLeader(String projectId, String currentUserId, ReviewMemberFeedbackRequest request) {
        Project project = getProject(projectId);
        Account reviewer = getAccount(currentUserId);
        String reviewerRole = reviewer.getRole() != null ? reviewer.getRole().getName() : null;

        if (!"ADMIN".equals(reviewerRole)) {
            if (!"LECTURER".equals(reviewerRole)) {
                throw new ForbiddenException("Only lecturers can review the project leader.");
            }
            ensureLecturerOwnsProject(project, currentUserId);
        }

        ProjectMember leaderMember = getLeaderMember(projectId);
        Account leader = leaderMember.getAccount();
        ProjectLeaderReview review = findLeaderReview(projectId, leader.getId())
                .orElse(ProjectLeaderReview.builder()
                        .project(project)
                        .leader(leader)
                        .build());

        review.setLecturerRating(request.getRating());
        review.setLecturerComment(trimToNull(request.getLecturerComment()));
        review.setReviewedBy(reviewer);
        review.setReviewedAt(LocalDateTime.now());
        ProjectLeaderReview saved = projectLeaderReviewRepository.save(review);
        sendLeaderReviewEmail(saved);
        return mapToLeaderReview(project, leaderMember, saved);
    }

    private Project getProject(String projectId) {
        return projectRepository.findById(projectId)
                .orElseThrow(() -> new NotFoundException("Project not found with id: " + projectId));
    }

    private Account getAccount(String accountId) {
        return accountRepository.findById(accountId)
                .orElseThrow(() -> new NotFoundException("Account not found with id: " + accountId));
    }

    private ProjectMember getProjectMember(String projectId, String accountId) {
        return projectMemberRepository.findByProjectIdAndAccountId(projectId, accountId)
                .orElseThrow(() -> new ForbiddenException("This account is not a member of the selected project."));
    }

    private ProjectMember getLeaderMember(String projectId) {
        return projectMemberRepository.findByProjectIdAndRoleInGroup(projectId, ProjectRole.LEADER)
                .orElseThrow(() -> new NotFoundException("No leader has been assigned for this project."));
    }

    private void ensureLecturerOwnsProject(Project project, String lecturerId) {
        if (!lecturerId.equals(project.getLecturerId())) {
            throw new ForbiddenException("You do not have permission to access feedback of this project.");
        }
    }

    private MemberFeedbackResponse mapToResponse(MemberFeedback feedback) {
        return MemberFeedbackResponse.builder()
                .id(feedback.getId())
                .projectId(feedback.getProject().getId())
                .projectName(feedback.getProject().getProjectName())
                .studentId(feedback.getStudent().getId())
                .studentUsername(feedback.getStudent().getUsername())
                .studentFullName(getFullName(feedback.getStudent()))
                .leaderId(feedback.getLeader().getId())
                .leaderUsername(feedback.getLeader().getUsername())
                .leaderFullName(getFullName(feedback.getLeader()))
                .leaderFeedback(feedback.getLeaderFeedback())
                .lecturerRating(feedback.getLecturerRating())
                .lecturerComment(feedback.getLecturerComment())
                .reviewedById(feedback.getReviewedBy() != null ? feedback.getReviewedBy().getId() : null)
                .reviewedByUsername(feedback.getReviewedBy() != null ? feedback.getReviewedBy().getUsername() : null)
                .reviewedByFullName(feedback.getReviewedBy() != null ? getFullName(feedback.getReviewedBy()) : null)
                .reviewedAt(feedback.getReviewedAt())
                .createdAt(feedback.getCreatedAt())
                .updatedAt(feedback.getUpdatedAt())
                .build();
    }

    private LeaderReviewResponse mapToLeaderReview(Project project, ProjectMember leaderMember, ProjectLeaderReview review) {
        Account leader = leaderMember.getAccount();
        return LeaderReviewResponse.builder()
                .id(review != null ? review.getId() : null)
                .projectId(project.getId())
                .projectName(project.getProjectName())
                .leaderId(leader.getId())
                .leaderUsername(leader.getUsername())
                .leaderFullName(getFullName(leader))
                .lecturerRating(review != null ? review.getLecturerRating() : MemberFeedbackRating.PENDING)
                .lecturerComment(review != null ? review.getLecturerComment() : null)
                .reviewedById(review != null && review.getReviewedBy() != null ? review.getReviewedBy().getId() : null)
                .reviewedByUsername(review != null && review.getReviewedBy() != null ? review.getReviewedBy().getUsername() : null)
                .reviewedByFullName(review != null && review.getReviewedBy() != null ? getFullName(review.getReviewedBy()) : null)
                .reviewedAt(review != null ? review.getReviewedAt() : null)
                .createdAt(review != null ? review.getCreatedAt() : null)
                .updatedAt(review != null ? review.getUpdatedAt() : null)
                .build();
    }

    private MemberFeedbackOverviewResponse mapOverview(Project project, ProjectMember member, MemberFeedback feedback) {
        Account student = member.getAccount();
        return MemberFeedbackOverviewResponse.builder()
                .projectId(project.getId())
                .projectName(project.getProjectName())
                .studentId(student.getId())
                .studentUsername(student.getUsername())
                .studentFullName(getFullName(student))
                .groupRole(member.getRoleInGroup() != null ? member.getRoleInGroup().name() : null)
                .feedbackId(feedback != null ? feedback.getId() : null)
                .leaderId(feedback != null && feedback.getLeader() != null ? feedback.getLeader().getId() : null)
                .leaderUsername(feedback != null && feedback.getLeader() != null ? feedback.getLeader().getUsername() : null)
                .leaderFullName(feedback != null && feedback.getLeader() != null ? getFullName(feedback.getLeader()) : null)
                .leaderFeedback(feedback != null ? feedback.getLeaderFeedback() : null)
                .lecturerRating(feedback != null ? feedback.getLecturerRating() : MemberFeedbackRating.PENDING)
                .lecturerComment(feedback != null ? feedback.getLecturerComment() : null)
                .reviewedAt(feedback != null ? feedback.getReviewedAt() : null)
                .createdAt(feedback != null ? feedback.getCreatedAt() : null)
                .updatedAt(feedback != null ? feedback.getUpdatedAt() : null)
                .build();
    }

    private String getFullName(Account account) {
        return account.getDetails() != null ? account.getDetails().getFullName() : null;
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private java.util.Optional<ProjectLeaderReview> findLeaderReview(String projectId, String leaderId) {
        try {
            return projectLeaderReviewRepository.findByProjectIdAndLeaderId(projectId, leaderId);
        } catch (Exception ex) {
            log.error("Unable to load leader review for project {} and leader {}", projectId, leaderId, ex);
            return java.util.Optional.empty();
        }
    }

    private List<MemberFeedback> safeFindFeedbacks(String projectId) {
        try {
            return memberFeedbackRepository.findByProjectIdOrderByCreatedAtDesc(projectId);
        } catch (Exception ex) {
            log.error("Unable to load member feedbacks for project {}", projectId, ex);
            return List.of();
        }
    }

    private void sendLecturerEvaluationEmail(MemberFeedback feedback) {
        Account student = feedback.getStudent();
        if (student == null || student.getEmail() == null || student.getEmail().isBlank()) {
            return;
        }

        try {
            sendHtmlEmail(
                    student.getEmail(),
                    "SWP391 Lecturer Evaluation Notification",
                    buildLecturerEvaluationEmailHtml(feedback)
            );
        } catch (MailException | MessagingException ex) {
            log.error("Failed to send lecturer evaluation email to {}", student.getEmail(), ex);
        }
    }

    private String buildLecturerEvaluationEmailHtml(MemberFeedback feedback) {
        String studentName = getFullName(feedback.getStudent()) != null
                ? getFullName(feedback.getStudent())
                : feedback.getStudent().getUsername();

        String projectName = feedback.getProject() != null ? feedback.getProject().getProjectName() : "your project";
        String rating = formatRatingLabel(feedback.getLecturerRating());
        String comment = feedback.getLecturerComment() != null && !feedback.getLecturerComment().isBlank()
                ? feedback.getLecturerComment()
                : "No additional lecturer comment.";
        String intro = "Your lecturer has reviewed the latest report submitted by your group leader.";
        String leaderFeedback = feedback.getLeaderFeedback() != null && !feedback.getLeaderFeedback().isBlank()
                ? feedback.getLeaderFeedback()
                : "No leader feedback was attached to this evaluation.";
        String leaderName = feedback.getLeader() != null
                ? (getFullName(feedback.getLeader()) != null ? getFullName(feedback.getLeader()) : feedback.getLeader().getUsername())
                : "Group leader";

        String extraSection = """
                <div style="margin-top:18px;border-radius:20px;background:#ffffff;border:1px solid rgba(15,23,42,0.08);padding:20px 22px;">
                  <div style="font-size:12px;letter-spacing:0.16em;text-transform:uppercase;color:#64748b;">Leader Feedback</div>
                  <div style="margin-top:8px;font-size:14px;font-weight:700;color:#0f172a;">Submitted by %s</div>
                  <div style="margin-top:10px;font-size:15px;line-height:1.7;color:#334155;">%s</div>
                </div>
                """.formatted(escapeHtml(leaderName), nl2br(escapeHtml(leaderFeedback)));

        return buildEvaluationEmailHtml(studentName, projectName, rating, comment, intro, feedback.getLecturerRating(), extraSection);
    }

    private void sendLeaderReviewEmail(ProjectLeaderReview review) {
        Account leader = review.getLeader();
        if (leader == null || leader.getEmail() == null || leader.getEmail().isBlank()) {
            return;
        }

        try {
            sendHtmlEmail(
                    leader.getEmail(),
                    "SWP391 Leader Evaluation Notification",
                    buildLeaderReviewEmailHtml(review)
            );
        } catch (MailException | MessagingException ex) {
            log.error("Failed to send leader evaluation email to {}", leader.getEmail(), ex);
        }
    }

    private String buildLeaderReviewEmailHtml(ProjectLeaderReview review) {
        String leaderName = getFullName(review.getLeader()) != null
                ? getFullName(review.getLeader())
                : review.getLeader().getUsername();

        String projectName = review.getProject() != null ? review.getProject().getProjectName() : "your project";
        String rating = formatRatingLabel(review.getLecturerRating());
        String comment = review.getLecturerComment() != null && !review.getLecturerComment().isBlank()
                ? review.getLecturerComment()
                : "No additional lecturer comment.";
        String intro = "Your lecturer has reviewed your latest group leader performance.";
        return buildEvaluationEmailHtml(leaderName, projectName, rating, comment, intro, review.getLecturerRating(), "");
    }

    private void sendHtmlEmail(String to, String subject, String htmlContent) throws MessagingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
        helper.setFrom(fromEmail);
        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(htmlContent, true);
        mailSender.send(message);
    }

    private String buildEvaluationEmailHtml(
            String recipientName,
            String projectName,
            String ratingLabel,
            String comment,
            String intro,
            MemberFeedbackRating rating,
            String extraSectionHtml
    ) {
        boolean positive = rating == MemberFeedbackRating.GOOD;
        boolean pending = rating == MemberFeedbackRating.PENDING;
        boolean negative = !positive && !pending;

        String accent = positive ? "#16a34a" : pending ? "#d97706" : "#dc2626";
        String surface = positive ? "#ecfdf3" : pending ? "#fff7ed" : "#fef2f2";
        String title = positive ? "Positive evaluation received" : pending ? "Evaluation is pending" : "Immediate improvement required";
        String highlightedRating = """
                <span style="display:inline-block;padding:6px 12px;border-radius:999px;background:%s;color:%s;font-weight:800;letter-spacing:0.02em;">
                  %s
                </span>
                """.formatted(surface, accent, escapeHtml(ratingLabel));
        String guidance = positive
                ? "Your performance has been evaluated positively. Please come to FPT University Lab 02 to receive your reward and recognition."
                : pending
                ? "Your lecturer has not marked this evaluation as positive or negative yet. Review the feedback and follow up where needed."
                : "This is a formal warning. Please review the lecturer feedback carefully and correct the reported issues as soon as possible.";
        String outcomeSection = positive
                ? """
                   <div style="margin-top:16px;padding:18px 20px;border-radius:18px;background:#ecfdf3;border:1px solid #bbf7d0;color:#166534;">
                     <div style="font-weight:800;font-size:15px;">Reward Notice</div>
                     <div style="margin-top:8px;font-size:14px;line-height:1.7;">
                       You have received a positive evaluation. Please visit <strong>FPT University Lab 02</strong> to receive your reward and recognition.
                     </div>
                   </div>
                   """
                : negative
                ? """
                   <div style="margin-top:16px;padding:18px 20px;border-radius:18px;background:#fef2f2;border:1px solid #fecaca;color:#b91c1c;">
                     <div style="font-weight:800;font-size:15px;">Warning Notice</div>
                     <div style="margin-top:8px;font-size:14px;line-height:1.7;">
                       This evaluation is negative. Please improve immediately and follow the lecturer instructions carefully.
                     </div>
                   </div>
                   """
                : "";
        String escalation = negative
                ? """
                   <div style="margin-top:16px;padding:16px 18px;border-radius:16px;background:#fff1f2;border:1px solid #fecdd3;color:#9f1239;">
                     <div style="font-weight:700;font-size:14px;">Warning notice</div>
                     <div style="margin-top:8px;font-size:14px;line-height:1.6;">
                       If you receive two more bad evaluations, your parents may be contacted to report your current academic situation.
                     </div>
                   </div>
                   """
                : "";

        return """
                <!DOCTYPE html>
                <html lang="en">
                  <body style="margin:0;padding:0;background:#f8fafc;font-family:Segoe UI,Arial,sans-serif;color:#0f172a;">
                    <div style="max-width:720px;margin:0 auto;padding:32px 18px;">
                      <div style="border-radius:28px;overflow:hidden;border:1px solid #e2e8f0;background:#ffffff;box-shadow:0 24px 60px rgba(15,23,42,0.08);">
                        <div style="padding:28px 32px;background:%s;color:#ffffff;">
                          <div style="font-size:12px;letter-spacing:0.18em;text-transform:uppercase;opacity:0.9;">SWP391 Evaluation Notice</div>
                          <div style="margin-top:12px;font-size:28px;font-weight:700;line-height:1.2;">%s</div>
                        </div>
                        <div style="padding:28px 32px;">
                          <p style="margin:0 0 16px;font-size:15px;line-height:1.7;">Hello %s,</p>
                          <p style="margin:0 0 18px;font-size:15px;line-height:1.7;">%s</p>
                          <p style="margin:0 0 18px;font-size:15px;line-height:1.7;">
                            Current result: %s
                          </p>

                          <div style="border-radius:20px;background:%s;border:1px solid rgba(15,23,42,0.06);padding:20px 22px;">
                            <div style="font-size:12px;letter-spacing:0.16em;text-transform:uppercase;color:#64748b;">Project</div>
                            <div style="margin-top:8px;font-size:22px;font-weight:700;color:#0f172a;">%s</div>

                            <div style="margin-top:18px;font-size:12px;letter-spacing:0.16em;text-transform:uppercase;color:#64748b;">Evaluation</div>
                            <div style="margin-top:8px;display:inline-block;padding:10px 16px;border-radius:999px;background:#ffffff;border:1px solid rgba(15,23,42,0.08);font-size:14px;font-weight:700;color:%s;">
                              %s
                            </div>

                            <div style="margin-top:18px;font-size:12px;letter-spacing:0.16em;text-transform:uppercase;color:#64748b;">Lecturer Comment</div>
                            <div style="margin-top:8px;font-size:15px;line-height:1.7;color:#334155;">%s</div>
                          </div>

                          <div style="margin-top:18px;font-size:15px;line-height:1.7;color:#334155;">%s</div>
                          %s
                          %s
                          %s
                          <div style="margin-top:20px;padding-top:18px;border-top:1px solid #e2e8f0;font-size:13px;line-height:1.7;color:#64748b;">
                            This email was generated by the SWP391 platform. Please contact your lecturer or group leader if you need clarification.
                          </div>
                        </div>
                      </div>
                    </div>
                  </body>
                </html>
                """.formatted(
                accent,
                title,
                escapeHtml(recipientName),
                escapeHtml(intro),
                highlightedRating,
                surface,
                escapeHtml(projectName),
                accent,
                escapeHtml(ratingLabel),
                nl2br(escapeHtml(comment)),
                escapeHtml(guidance),
                outcomeSection,
                extraSectionHtml == null ? "" : extraSectionHtml,
                escalation
        );
    }

    private String formatRatingLabel(MemberFeedbackRating rating) {
        if (rating == null) {
            return "Pending";
        }
        return switch (rating) {
            case GOOD -> "Good";
            case PENDING -> "Pending";
            case NEEDS_IMPROVEMENT -> "Bad";
            case AT_RISK -> "Bad";
        };
    }

    private String escapeHtml(String value) {
        if (value == null) {
            return "";
        }
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    private String nl2br(String value) {
        return value.replace("\n", "<br/>");
    }
}

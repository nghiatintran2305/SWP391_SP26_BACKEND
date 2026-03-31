package com.example.swp391.projects.entity;

import com.example.swp391.accounts.entity.Account;
import com.example.swp391.projects.enums.MemberFeedbackRating;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.UuidGenerator;

import java.time.LocalDateTime;

@Entity
@Table(name = "project_member_reports")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MemberFeedback {

    @Id
    @GeneratedValue(generator = "UUID")
    @UuidGenerator
    @Column(length = 36, updatable = false, nullable = false)
    private String id;

    @ManyToOne
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @ManyToOne
    @JoinColumn(name = "student_id", nullable = false)
    private Account student;

    @ManyToOne
    @JoinColumn(name = "leader_id", nullable = false)
    private Account leader;

    @Column(name = "leader_feedback", nullable = false, columnDefinition = "TEXT")
    private String leaderFeedback;

    @Enumerated(EnumType.STRING)
    @Column(name = "lecturer_rating", nullable = false)
    private MemberFeedbackRating lecturerRating;

    @Column(name = "lecturer_comment", columnDefinition = "TEXT")
    private String lecturerComment;

    @ManyToOne
    @JoinColumn(name = "reviewed_by")
    private Account reviewedBy;

    private LocalDateTime reviewedAt;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}

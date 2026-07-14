package com.example.swp391.accounts.repository;

import com.example.swp391.accounts.entity.PasswordResetOtp;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface PasswordResetOtpRepository extends JpaRepository<PasswordResetOtp, String> {

    List<PasswordResetOtp> findByAccountIdAndUsedFalse(String accountId);

    Optional<PasswordResetOtp> findTopByAccountIdAndOtpCodeAndUsedFalseAndExpiresAtAfterOrderByCreatedAtDesc(
            String accountId,
            String otpCode,
            LocalDateTime now
    );
}

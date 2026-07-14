package com.example.swp391.accounts.repository;

import com.example.swp391.accounts.entity.RegistrationOtp;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface RegistrationOtpRepository extends JpaRepository<RegistrationOtp, String> {

    List<RegistrationOtp> findByAccountIdAndUsedFalse(String accountId);

    Optional<RegistrationOtp> findTopByAccountIdAndOtpCodeAndUsedFalseAndExpiresAtAfterOrderByCreatedAtDesc(
            String accountId,
            String otpCode,
            LocalDateTime now
    );
}

package com.example.swp391.accounts.service.impl;

import com.example.swp391.accounts.dto.request.ForgotPasswordRequest;
import com.example.swp391.accounts.dto.request.LoginRequest;
import com.example.swp391.accounts.dto.request.ResetPasswordRequest;
import com.example.swp391.accounts.dto.response.LoginResponse;
import com.example.swp391.accounts.dto.response.MessageResponse;
import com.example.swp391.accounts.entity.Account;
import com.example.swp391.accounts.entity.PasswordResetOtp;
import com.example.swp391.accounts.repository.AccountRepository;
import com.example.swp391.accounts.repository.PasswordResetOtpRepository;
import com.example.swp391.accounts.service.IAuthService;
import com.example.swp391.configs.security.JwtUtil;
import com.example.swp391.exceptions.BadRequestException;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements IAuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthServiceImpl.class);
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final AccountRepository accountRepository;
    private final PasswordResetOtpRepository passwordResetOtpRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final JavaMailSender mailSender;

    @Value("${app.otp.expiration-minutes}")
    private long otpExpirationMinutes;

    @Value("${app.mail.from}")
    private String fromEmail;

    @Override
    public LoginResponse login(LoginRequest request) {
        Account account = accountRepository.findByUsernameIgnoreCase(request.getUsername().trim())
                .orElseThrow(() -> new BadCredentialsException("Username or password is incorrect"));

        if (!account.isActive()) {
            if (!account.isEmailVerified()) {
                throw new BadCredentialsException("Account is not verified. Please verify your email OTP first");
            }
            throw new BadCredentialsException("Account has been deactivated");
        }

        boolean matches = passwordEncoder.matches(request.getPassword(), account.getPassword());
        if (!matches) {
            throw new BadCredentialsException("Username or password is incorrect");
        }

        String roleName = account.getRole() != null ? account.getRole().getName() : null;
        String token = jwtUtil.generateToken(
                account.getUsername(),
                roleName == null ? List.of() : List.of(roleName)
        );

        return LoginResponse.builder()
                .token(token)
                .roles(roleName == null ? "[]" : String.valueOf(List.of(roleName)))
                .build();
    }

    @Override
    @Transactional
    public MessageResponse forgotPassword(ForgotPasswordRequest request) {
        String normalizedEmail = request.getEmail().trim();

        Account account = accountRepository.findByEmailIgnoreCase(normalizedEmail)
                .orElseThrow(() -> new BadRequestException("No account found with this email"));

        if (!account.isEmailVerified()) {
            throw new BadRequestException("Account email is not verified yet");
        }

        if (!account.isActive()) {
            throw new BadRequestException("Account has been deactivated");
        }

        invalidateActiveOtps(account.getId());

        PasswordResetOtp otp = PasswordResetOtp.builder()
                .account(account)
                .otpCode(generateOtpCode())
                .expiresAt(LocalDateTime.now().plusMinutes(otpExpirationMinutes))
                .used(false)
                .build();

        passwordResetOtpRepository.save(otp);
        sendResetOtpEmail(account, otp);

        return MessageResponse.builder()
                .success(true)
                .message("OTP has been sent to your registered email")
                .build();
    }

    @Override
    @Transactional
    public MessageResponse resetPassword(ResetPasswordRequest request) {
        String normalizedEmail = request.getEmail().trim();

        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new BadRequestException("Confirm password does not match");
        }

        Account account = accountRepository.findByEmailIgnoreCase(normalizedEmail)
                .orElseThrow(() -> new BadRequestException("No account found with this email"));

        if (!account.isEmailVerified()) {
            throw new BadRequestException("Account email is not verified yet");
        }

        if (!account.isActive()) {
            throw new BadRequestException("Account has been deactivated");
        }

        PasswordResetOtp otp = passwordResetOtpRepository
                .findTopByAccountIdAndOtpCodeAndUsedFalseAndExpiresAtAfterOrderByCreatedAtDesc(
                        account.getId(),
                        request.getOtpCode().trim(),
                        LocalDateTime.now()
                )
                .orElseThrow(() -> new BadRequestException("OTP is invalid or has expired"));

        otp.setUsed(true);
        passwordResetOtpRepository.save(otp);

        account.setPassword(passwordEncoder.encode(request.getNewPassword()));
        accountRepository.save(account);

        invalidateActiveOtps(account.getId());

        return MessageResponse.builder()
                .success(true)
                .message("Password has been reset successfully")
                .build();
    }

    private void invalidateActiveOtps(String accountId) {
        List<PasswordResetOtp> activeOtps = passwordResetOtpRepository.findByAccountIdAndUsedFalse(accountId);
        for (PasswordResetOtp activeOtp : activeOtps) {
            activeOtp.setUsed(true);
        }
        passwordResetOtpRepository.saveAll(activeOtps);
    }

    private String generateOtpCode() {
        int number = 100000 + SECURE_RANDOM.nextInt(900000);
        return String.valueOf(number);
    }

    private void sendResetOtpEmail(Account account, PasswordResetOtp otp) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(account.getEmail());
            message.setSubject("SWP391 Password Reset OTP");
            message.setText(buildResetOtpEmailBody(account, otp));
            mailSender.send(message);
        } catch (MailException ex) {
            log.error("Failed to send reset OTP email to {}", account.getEmail(), ex);
            throw new RuntimeException("Unable to send OTP email at the moment");
        }
    }

    private String buildResetOtpEmailBody(Account account, PasswordResetOtp otp) {
        String name = account.getDetails() != null && account.getDetails().getFullName() != null
                ? account.getDetails().getFullName()
                : account.getUsername();

        return """
                Hello %s,

                We received a request to reset your SWP391 account password.

                Your OTP code is: %s

                This OTP will expire in %d minutes.

                If you did not request this, you can ignore this email.

                SWP391 System
                """.formatted(name, otp.getOtpCode(), otpExpirationMinutes);
    }
}

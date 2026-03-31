package com.example.swp391.accounts.service.impl;

import com.example.swp391.accounts.dto.request.AdminUpdateAccountRequest;
import com.example.swp391.accounts.dto.request.ChangePasswordRequest;
import com.example.swp391.accounts.dto.request.CreateLecturerRequest;
import com.example.swp391.accounts.dto.request.CreateStudentRequest;
import com.example.swp391.accounts.dto.request.ResendRegistrationOtpRequest;
import com.example.swp391.accounts.dto.request.UpdateAccountRequest;
import com.example.swp391.accounts.dto.request.VerifyRegistrationOtpRequest;
import com.example.swp391.accounts.dto.response.AccountLinkStatusResponse;
import com.example.swp391.accounts.dto.response.AccountResponse;
import com.example.swp391.accounts.dto.response.LecturerResponse;
import com.example.swp391.accounts.dto.response.LinkedStudentResponse;
import com.example.swp391.accounts.dto.response.MessageResponse;
import com.example.swp391.accounts.entity.Account;
import com.example.swp391.accounts.entity.AccountDetails;
import com.example.swp391.accounts.entity.RegistrationOtp;
import com.example.swp391.accounts.entity.Role;
import com.example.swp391.accounts.repository.AccountRepository;
import com.example.swp391.accounts.repository.RegistrationOtpRepository;
import com.example.swp391.accounts.repository.RoleRepository;
import com.example.swp391.accounts.service.IAccountService;
import com.example.swp391.exceptions.BadRequestException;
import com.example.swp391.exceptions.NotFoundException;
import com.example.swp391.github.entity.GithubUserMapping;
import com.example.swp391.github.repository.GithubUserMappingRepository;
import com.example.swp391.jira.entity.JiraUserMapping;
import com.example.swp391.jira.repository.JiraUserMappingRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class AccountServiceImpl implements IAccountService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final AccountRepository accountRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final GithubUserMappingRepository githubUserMappingRepository;
    private final JiraUserMappingRepository jiraUserMappingRepository;
    private final RegistrationOtpRepository registrationOtpRepository;
    private final JavaMailSender mailSender;

    @Value("${app.otp.expiration-minutes}")
    private long otpExpirationMinutes;

    @Value("${app.mail.from}")
    private String fromEmail;

    @Override
    public List<LecturerResponse> getLecturers(String keyword) {
        List<Account> lecturers;

        Role lecturerRole = roleRepository.findByName("LECTURER")
                .orElseThrow(() -> new NotFoundException("Role LECTURER khong ton tai"));

        if (keyword == null || keyword.trim().isEmpty()) {
            lecturers = accountRepository.findByRoleAndIsActiveTrue(lecturerRole);
        } else {
            lecturers = accountRepository
                    .findByRoleAndIsActiveTrueAndUsernameContainingIgnoreCase(
                            lecturerRole,
                            keyword.trim()
                    );
        }

        return lecturers.stream()
                .map(a -> new LecturerResponse(
                        a.getId(),
                        a.getUsername(),
                        a.getEmail(),
                        a.getDetails() != null ? a.getDetails().getFullName() : null
                ))
                .toList();
    }

    @Override
    public MessageResponse registerStudent(CreateStudentRequest request) {
        String normalizedUsername = normalizeRequired(request.getUsername());
        String normalizedEmail = normalizeRequired(request.getEmail());

        if (accountRepository.existsByUsernameIgnoreCase(normalizedUsername)) {
            throw new BadRequestException("Username already exists. Please choose another username");
        }

        if (accountRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            throw new BadRequestException("Email already exists");
        }

        Role studentRole = roleRepository.findByName("STUDENT")
                .orElseThrow(() -> new NotFoundException("Role STUDENT khong ton tai"));

        AccountDetails details = AccountDetails.builder()
                .fullName(normalizeOptional(request.getFullName()))
                .phone(normalizeOptional(request.getPhone()))
                .address(normalizeOptional(request.getAddress()))
                .dateOfBirth(request.getDateOfBirth())
                .build();

        Account account = Account.builder()
                .username(normalizedUsername)
                .password(passwordEncoder.encode(request.getPassword()))
                .email(normalizedEmail)
                .role(studentRole)
                .details(details)
                .emailVerified(false)
                .isActive(false)
                .build();

        Account savedAccount = accountRepository.saveAndFlush(account);
        createAndSendRegistrationOtp(savedAccount);

        return MessageResponse.builder()
                .success(true)
                .message("Registration successful. OTP has been sent to your email")
                .build();
    }

    @Override
    public MessageResponse verifyStudentRegistrationOtp(VerifyRegistrationOtpRequest request) {
        Account account = accountRepository.findByEmailIgnoreCase(normalizeRequired(request.getEmail()))
                .orElseThrow(() -> new BadRequestException("No account found with this email"));

        if (account.isEmailVerified()) {
            return MessageResponse.builder()
                    .success(true)
                    .message("Account already verified")
                    .build();
        }

        RegistrationOtp otp = registrationOtpRepository
                .findTopByAccountIdAndOtpCodeAndUsedFalseAndExpiresAtAfterOrderByCreatedAtDesc(
                        account.getId(),
                        request.getOtpCode().trim(),
                        LocalDateTime.now()
                )
                .orElseThrow(() -> new BadRequestException("OTP is invalid or has expired"));

        otp.setUsed(true);
        registrationOtpRepository.save(otp);

        account.setEmailVerified(true);
        account.setActive(true);
        accountRepository.save(account);

        invalidateActiveRegistrationOtps(account.getId());

        return MessageResponse.builder()
                .success(true)
                .message("Email verified successfully. You can now log in")
                .build();
    }

    @Override
    public MessageResponse resendStudentRegistrationOtp(ResendRegistrationOtpRequest request) {
        Account account = accountRepository.findByEmailIgnoreCase(normalizeRequired(request.getEmail()))
                .orElseThrow(() -> new BadRequestException("No account found with this email"));

        if (account.isEmailVerified()) {
            throw new BadRequestException("Account already verified");
        }

        createAndSendRegistrationOtp(account);

        return MessageResponse.builder()
                .success(true)
                .message("A new OTP has been sent to your email")
                .build();
    }

    @Override
    public AccountResponse updateAccount(String accountId, UpdateAccountRequest request) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new NotFoundException("Tai khoan khong ton tai"));

        if (request.getEmail() != null && !request.getEmail().equals(account.getEmail())) {
            String normalizedEmail = normalizeRequired(request.getEmail());
            if (accountRepository.existsByEmailAndIdNot(normalizedEmail, accountId)) {
                throw new BadRequestException("Email already exists");
            }
            account.setEmail(normalizedEmail);
        }

        AccountDetails details = account.getDetails();
        if (details == null) {
            details = new AccountDetails();
        }

        if (request.getFullName() != null) {
            details.setFullName(normalizeOptional(request.getFullName()));
        }
        if (request.getPhone() != null) {
            details.setPhone(normalizeOptional(request.getPhone()));
        }
        if (request.getAddress() != null) {
            details.setAddress(normalizeOptional(request.getAddress()));
        }
        if (request.getDateOfBirth() != null) {
            details.setDateOfBirth(request.getDateOfBirth());
        }

        account.setDetails(details);
        return mapToAccountResponse(accountRepository.save(account));
    }

    @Override
    public MessageResponse deleteAccount(String accountId) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new NotFoundException("Tai khoan khong ton tai"));

        account.setActive(false);
        accountRepository.save(account);

        return MessageResponse.builder()
                .message("Tai khoan da duoc xoa thanh cong")
                .success(true)
                .build();
    }

    @Override
    public MessageResponse changePassword(String accountId, ChangePasswordRequest request) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new NotFoundException("Tai khoan khong ton tai"));

        if (!passwordEncoder.matches(request.getOldPassword(), account.getPassword())) {
            throw new BadRequestException("Mat khau cu khong chinh xac");
        }

        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new BadRequestException("Mat khau xac nhan khong khop");
        }

        account.setPassword(passwordEncoder.encode(request.getNewPassword()));
        accountRepository.save(account);

        return MessageResponse.builder()
                .message("Doi mat khau thanh cong")
                .success(true)
                .build();
    }

    @Override
    public AccountResponse getAccountById(String accountId) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new NotFoundException("Tai khoan khong ton tai"));
        return mapToAccountResponse(account);
    }

    @Override
    public AccountResponse getCurrentAccount(String username) {
        Account account = accountRepository.findByUsernameIgnoreCase(username)
                .orElseThrow(() -> new NotFoundException("Tai khoan khong ton tai"));
        return mapToAccountResponse(account);
    }

    @Override
    public AccountLinkStatusResponse getAccountLinkStatus(String accountId) {
        accountRepository.findById(accountId)
                .orElseThrow(() -> new NotFoundException("Account not found with id: " + accountId));

        GithubUserMapping githubMapping = githubUserMappingRepository.findByAccountId(accountId).orElse(null);
        boolean githubLinked = githubMapping != null;
        String githubUsername = githubLinked ? githubMapping.getGithubUsername() : null;

        JiraUserMapping jiraMapping = jiraUserMappingRepository.findByAccountId(accountId).orElse(null);
        boolean jiraLinked = jiraMapping != null;
        String jiraAccountId = jiraLinked ? jiraMapping.getJiraAccountId() : null;

        return AccountLinkStatusResponse.builder()
                .githubLinked(githubLinked)
                .githubUsername(githubUsername)
                .jiraLinked(jiraLinked)
                .jiraAccountId(jiraAccountId)
                .build();
    }

    @Override
    public AccountResponse createLecturer(CreateLecturerRequest request) {
        String normalizedUsername = normalizeRequired(request.getUsername());
        String normalizedEmail = normalizeRequired(request.getEmail());

        if (accountRepository.existsByUsernameIgnoreCase(normalizedUsername)) {
            throw new BadRequestException("Username already exists. Please choose another username");
        }

        if (accountRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            throw new BadRequestException("Email already exists");
        }

        Role lecturerRole = roleRepository.findByName("LECTURER")
                .orElseThrow(() -> new NotFoundException("Role LECTURER khong ton tai"));

        AccountDetails details = AccountDetails.builder()
                .fullName(normalizeOptional(request.getFullName()))
                .phone(normalizeOptional(request.getPhone()))
                .address(normalizeOptional(request.getAddress()))
                .dateOfBirth(request.getDateOfBirth())
                .build();

        Account account = Account.builder()
                .username(normalizedUsername)
                .password(passwordEncoder.encode(request.getPassword()))
                .email(normalizedEmail)
                .role(lecturerRole)
                .details(details)
                .emailVerified(true)
                .isActive(true)
                .build();

        return mapToAccountResponse(accountRepository.save(account));
    }

    @Override
    public AccountResponse adminUpdateAccount(String accountId, AdminUpdateAccountRequest request) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new NotFoundException("Tai khoan khong ton tai"));

        if (request.getEmail() != null && !request.getEmail().equals(account.getEmail())) {
            String normalizedEmail = normalizeRequired(request.getEmail());
            if (accountRepository.existsByEmailAndIdNot(normalizedEmail, accountId)) {
                throw new BadRequestException("Email already exists");
            }
            account.setEmail(normalizedEmail);
        }

        AccountDetails details = account.getDetails();
        if (details == null) {
            details = new AccountDetails();
        }

        if (request.getFullName() != null) {
            details.setFullName(normalizeOptional(request.getFullName()));
        }
        if (request.getPhone() != null) {
            details.setPhone(normalizeOptional(request.getPhone()));
        }
        if (request.getAddress() != null) {
            details.setAddress(normalizeOptional(request.getAddress()));
        }
        if (request.getDateOfBirth() != null) {
            details.setDateOfBirth(request.getDateOfBirth());
        }

        account.setDetails(details);

        if (request.getIsActive() != null) {
            account.setActive(request.getIsActive());
        }

        return mapToAccountResponse(accountRepository.save(account));
    }

    @Override
    public List<AccountResponse> getAllAccounts() {
        return accountRepository.findByIsActiveTrue().stream()
                .map(this::mapToAccountResponse)
                .toList();
    }

    @Override
    public List<AccountResponse> getStudents(String keyword) {
        List<Account> students;

        Role studentRole = roleRepository.findByName("STUDENT")
                .orElseThrow(() -> new NotFoundException("Role STUDENT khong ton tai"));

        if (keyword == null || keyword.trim().isEmpty()) {
            students = accountRepository.findByRoleAndIsActiveTrue(studentRole);
        } else {
            students = accountRepository
                    .findByRoleAndIsActiveTrueAndUsernameContainingIgnoreCase(
                            studentRole,
                            keyword.trim()
                    );
        }

        return students.stream()
                .map(this::mapToAccountResponse)
                .toList();
    }

    @Override
    public List<LinkedStudentResponse> getLinkedStudents() {
        Role studentRole = roleRepository.findByName("STUDENT")
                .orElseThrow(() -> new NotFoundException("Role STUDENT khong ton tai"));

        return accountRepository.findByRole(studentRole).stream()
                .filter(Account::isActive)
                .map(account -> LinkedStudentResponse.builder()
                        .id(account.getId())
                        .username(account.getUsername())
                        .email(account.getEmail())
                        .fullName(account.getDetails() != null ? account.getDetails().getFullName() : null)
                        .githubLinked(githubUserMappingRepository.existsByAccountId(account.getId()))
                        .jiraLinked(jiraUserMappingRepository.existsByAccountId(account.getId()))
                        .build())
                .filter(student -> student.isGithubLinked() || student.isJiraLinked())
                .toList();
    }

    private void createAndSendRegistrationOtp(Account account) {
        invalidateActiveRegistrationOtps(account.getId());

        RegistrationOtp otp = RegistrationOtp.builder()
                .account(account)
                .otpCode(generateOtpCode())
                .expiresAt(LocalDateTime.now().plusMinutes(otpExpirationMinutes))
                .used(false)
                .build();

        registrationOtpRepository.saveAndFlush(otp);
        sendRegistrationOtpEmail(account, otp);
    }

    private void invalidateActiveRegistrationOtps(String accountId) {
        List<RegistrationOtp> activeOtps = registrationOtpRepository.findByAccountIdAndUsedFalse(accountId);
        for (RegistrationOtp activeOtp : activeOtps) {
            activeOtp.setUsed(true);
        }
        registrationOtpRepository.saveAllAndFlush(activeOtps);
    }

    private String generateOtpCode() {
        int number = 100000 + SECURE_RANDOM.nextInt(900000);
        return String.valueOf(number);
    }

    private void sendRegistrationOtpEmail(Account account, RegistrationOtp otp) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(account.getEmail());
            message.setSubject("SWP391 Registration OTP");
            message.setText(buildRegistrationOtpEmailBody(account, otp));
            mailSender.send(message);
        } catch (MailException ex) {
            throw new RuntimeException("Unable to send OTP email at the moment", ex);
        }
    }

    private String buildRegistrationOtpEmailBody(Account account, RegistrationOtp otp) {
        String name = account.getDetails() != null && account.getDetails().getFullName() != null
                ? account.getDetails().getFullName()
                : account.getUsername();

        return """
                Hello %s,

                Thank you for registering an SWP391 account.

                Your email verification OTP is: %s

                This OTP will expire in %d minutes.

                If you did not create this account, please ignore this email.

                SWP391 System
                """.formatted(name, otp.getOtpCode(), otpExpirationMinutes);
    }

    private AccountResponse mapToAccountResponse(Account account) {
        return AccountResponse.builder()
                .id(account.getId())
                .username(account.getUsername())
                .email(account.getEmail())
                .fullName(account.getDetails() != null ? account.getDetails().getFullName() : null)
                .phone(account.getDetails() != null ? account.getDetails().getPhone() : null)
                .address(account.getDetails() != null ? account.getDetails().getAddress() : null)
                .dateOfBirth(account.getDetails() != null ? account.getDetails().getDateOfBirth() : null)
                .role(account.getRole().getName())
                .isActive(account.isActive())
                .createdAt(account.getCreatedAt())
                .updatedAt(account.getUpdatedAt())
                .build();
    }

    private String normalizeRequired(String value) {
        return value == null ? null : value.trim();
    }

    private String normalizeOptional(String value) {
        return value == null ? null : value.trim();
    }
}

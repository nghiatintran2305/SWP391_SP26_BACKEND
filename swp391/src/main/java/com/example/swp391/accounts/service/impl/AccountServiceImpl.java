package com.example.swp391.accounts.service.impl;

import com.example.swp391.accounts.dto.request.*;
import com.example.swp391.accounts.dto.response.AccountResponse;
import com.example.swp391.accounts.dto.response.LecturerResponse;
import com.example.swp391.accounts.dto.response.MessageResponse;
import com.example.swp391.accounts.entity.Account;
import com.example.swp391.accounts.entity.AccountDetails;
import com.example.swp391.accounts.entity.Role;
import com.example.swp391.accounts.repository.AccountRepository;
import com.example.swp391.accounts.repository.RoleRepository;
import com.example.swp391.accounts.service.IAccountService;
import com.example.swp391.exception.BadRequestException;
import com.example.swp391.exception.NotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional()
public class AccountServiceImpl implements IAccountService {

    private final AccountRepository accountRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public List<LecturerResponse> getLecturers(String keyword) {

        List<Account> lecturers;

        Role lecturerRole = roleRepository.findByName("LECTURER")
                .orElseThrow(() -> new NotFoundException("Role LECTURER không tồn tại"));

        if (keyword == null || keyword.trim().isEmpty()) {
            lecturers = accountRepository.findByRole(lecturerRole);
        } else {
            lecturers = accountRepository
                    .findByRoleAndUsernameContainingIgnoreCase(
                            lecturerRole,
                            keyword.trim()
                    );
        }

        return lecturers.stream()
                .map(a -> new LecturerResponse(
                        a.getId(),
                        a.getUsername(),
                        a.getEmail(),
                        a.getDetails() != null
                                ? a.getDetails().getFullName()
                                : null
                ))
                .toList();
    }

    // ==================== STUDENT (USER) OPERATIONS ====================

    @Override
    public AccountResponse registerStudent(CreateStudentRequest request) {
        // Validate username uniqueness
        if (accountRepository.existsByUsername(request.getUsername())) {
            throw new BadRequestException("Username đã tồn tại");
        }

        // Validate email uniqueness
        if (accountRepository.existsByEmail(request.getEmail())) {
            throw new BadRequestException("Email đã được sử dụng");
        }

        // Get STUDENT role
        Role studentRole = roleRepository.findByName("STUDENT")
                .orElseThrow(() -> new NotFoundException("Role STUDENT không tồn tại"));

        // Create account details
        AccountDetails details = AccountDetails.builder()
                .fullName(request.getFullName())
                .phone(request.getPhone())
                .address(request.getAddress())
                .build();

        // Create account
        Account account = Account.builder()
                .username(request.getUsername())
                .password(passwordEncoder.encode(request.getPassword()))
                .email(request.getEmail())
                .role(studentRole)
                .details(details)
                .isActive(true)
                .build();

        Account savedAccount = accountRepository.save(account);
        return mapToAccountResponse(savedAccount);
    }

    @Override
    public AccountResponse updateAccount(String accountId, UpdateAccountRequest request) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new NotFoundException("Tài khoản không tồn tại"));

        // Validate email uniqueness if changed
        if (request.getEmail() != null && !request.getEmail().equals(account.getEmail())) {
            if (accountRepository.existsByEmailAndIdNot(request.getEmail(), accountId)) {
                throw new BadRequestException("Email đã được sử dụng");
            }
            account.setEmail(request.getEmail());
        }

        // Update details
        AccountDetails details = account.getDetails();
        if (details == null) {
            details = new AccountDetails();
        }

        if (request.getFullName() != null) {
            details.setFullName(request.getFullName());
        }
        if (request.getPhone() != null) {
            details.setPhone(request.getPhone());
        }
        if (request.getAddress() != null) {
            details.setAddress(request.getAddress());
        }

        account.setDetails(details);
        Account updatedAccount = accountRepository.save(account);
        return mapToAccountResponse(updatedAccount);
    }

    @Override
    public MessageResponse deleteAccount(String accountId) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new NotFoundException("Tài khoản không tồn tại"));

        // Soft delete - deactivate account
        account.setActive(false);
        accountRepository.save(account);

        return MessageResponse.builder()
                .message("Tài khoản đã được xóa thành công")
                .success(true)
                .build();
    }

    @Override
    public MessageResponse changePassword(String accountId, ChangePasswordRequest request) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new NotFoundException("Tài khoản không tồn tại"));

        // Validate old password
        if (!passwordEncoder.matches(request.getOldPassword(), account.getPassword())) {
            throw new BadRequestException("Mật khẩu cũ không chính xác");
        }

        // Validate new password confirmation
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new BadRequestException("Mật khẩu xác nhận không khớp");
        }

        // Update password
        account.setPassword(passwordEncoder.encode(request.getNewPassword()));
        accountRepository.save(account);

        return MessageResponse.builder()
                .message("Đổi mật khẩu thành công")
                .success(true)
                .build();
    }

    @Override
    public AccountResponse getAccountById(String accountId) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new NotFoundException("Tài khoản không tồn tại"));
        return mapToAccountResponse(account);
    }

    @Override
    public AccountResponse getCurrentAccount(String username) {
        Account account = accountRepository.findByUsername(username)
                .orElseThrow(() -> new NotFoundException("Tài khoản không tồn tại"));
        return mapToAccountResponse(account);
    }

    // ==================== ADMIN OPERATIONS ====================

    @Override
    public AccountResponse createLecturer(CreateLecturerRequest request) {
        // Validate username uniqueness
        if (accountRepository.existsByUsername(request.getUsername())) {
            throw new BadRequestException("Username đã tồn tại");
        }

        // Validate email uniqueness
        if (accountRepository.existsByEmail(request.getEmail())) {
            throw new BadRequestException("Email đã được sử dụng");
        }

        // Get LECTURER role
        Role lecturerRole = roleRepository.findByName("LECTURER")
                .orElseThrow(() -> new NotFoundException("Role LECTURER không tồn tại"));

        // Create account details
        AccountDetails details = AccountDetails.builder()
                .fullName(request.getFullName())
                .phone(request.getPhone())
                .address(request.getAddress())
                .build();

        // Create account
        Account account = Account.builder()
                .username(request.getUsername())
                .password(passwordEncoder.encode(request.getPassword()))
                .email(request.getEmail())
                .role(lecturerRole)
                .details(details)
                .isActive(true)
                .build();

        Account savedAccount = accountRepository.save(account);
        return mapToAccountResponse(savedAccount);
    }

    @Override
    public AccountResponse adminUpdateAccount(String accountId, AdminUpdateAccountRequest request) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new NotFoundException("Tài khoản không tồn tại"));

        // Validate email uniqueness if changed
        if (request.getEmail() != null && !request.getEmail().equals(account.getEmail())) {
            if (accountRepository.existsByEmailAndIdNot(request.getEmail(), accountId)) {
                throw new BadRequestException("Email đã được sử dụng");
            }
            account.setEmail(request.getEmail());
        }

        // Update details
        AccountDetails details = account.getDetails();
        if (details == null) {
            details = new AccountDetails();
        }

        if (request.getFullName() != null) {
            details.setFullName(request.getFullName());
        }
        if (request.getPhone() != null) {
            details.setPhone(request.getPhone());
        }
        if (request.getAddress() != null) {
            details.setAddress(request.getAddress());
        }

        account.setDetails(details);

        // Admin can also update isActive status
        if (request.getIsActive() != null) {
            account.setActive(request.getIsActive());
        }

        Account updatedAccount = accountRepository.save(account);
        return mapToAccountResponse(updatedAccount);
    }

    @Override
    public List<AccountResponse> getAllAccounts() {
        return accountRepository.findAll().stream()
                .map(this::mapToAccountResponse)
                .toList();
    }

    @Override
    public List<AccountResponse> getStudents(String keyword) {
        List<Account> students;

        Role studentRole = roleRepository.findByName("STUDENT")
                .orElseThrow(() -> new NotFoundException("Role STUDENT không tồn tại"));

        if (keyword == null || keyword.trim().isEmpty()) {
            students = accountRepository.findByRole(studentRole);
        } else {
            students = accountRepository
                    .findByRoleAndUsernameContainingIgnoreCase(
                            studentRole,
                            keyword.trim()
                    );
        }

        return students.stream()
                .map(this::mapToAccountResponse)
                .toList();
    }

    // ==================== HELPER METHODS ====================

    private AccountResponse mapToAccountResponse(Account account) {
        return AccountResponse.builder()
                .id(account.getId())
                .username(account.getUsername())
                .email(account.getEmail())
                .fullName(account.getDetails() != null ? account.getDetails().getFullName() : null)
                .phone(account.getDetails() != null ? account.getDetails().getPhone() : null)
                .address(account.getDetails() != null ? account.getDetails().getAddress() : null)
                .role(account.getRole().getName())
                .isActive(account.isActive())
                .createdAt(account.getCreatedAt())
                .updatedAt(account.getUpdatedAt())
                .build();
    }
}


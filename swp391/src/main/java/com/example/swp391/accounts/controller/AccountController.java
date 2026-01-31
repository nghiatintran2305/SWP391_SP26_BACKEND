package com.example.swp391.accounts.controller;

import com.example.swp391.accounts.dto.request.*;
import com.example.swp391.accounts.dto.response.AccountResponse;
import com.example.swp391.accounts.dto.response.LecturerResponse;
import com.example.swp391.accounts.dto.response.MessageResponse;
import com.example.swp391.accounts.service.IAccountService;
import com.example.swp391.config.security.SecurityUtil;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/accounts")
@AllArgsConstructor
public class AccountController {
    private final IAccountService accountService;

    // ==================== PUBLIC ENDPOINTS ====================

    /**
     * Đăng ký tài khoản học sinh mới
     */
    @PostMapping("/register/student")
    public ResponseEntity<AccountResponse> registerStudent(
            @Valid @RequestBody CreateStudentRequest request
    ) {
        AccountResponse response = accountService.registerStudent(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // ==================== USER (AUTHENTICATED) ENDPOINTS ====================

    /**
     * Lấy thông tin tài khoản hiện tại
     */
    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<AccountResponse> getCurrentAccount() {
        String username = SecurityUtil.getCurrentUsername();
        AccountResponse response = accountService.getCurrentAccount(username);
        return ResponseEntity.ok(response);
    }

    /**
     * Cập nhật thông tin tài khoản của user
     */
    @PutMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<AccountResponse> updateMyAccount(
            @Valid @RequestBody UpdateAccountRequest request
    ) {
        String username = SecurityUtil.getCurrentUsername();
        AccountResponse currentAccount = accountService.getCurrentAccount(username);
        AccountResponse response = accountService.updateAccount(currentAccount.getId(), request);
        return ResponseEntity.ok(response);
    }

    /**
     * Đổi mật khẩu
     */
    @PutMapping("/me/change-password")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<MessageResponse> changePassword(
            @Valid @RequestBody ChangePasswordRequest request
    ) {
        String username = SecurityUtil.getCurrentUsername();
        AccountResponse currentAccount = accountService.getCurrentAccount(username);
        MessageResponse response = accountService.changePassword(currentAccount.getId(), request);
        return ResponseEntity.ok(response);
    }

    /**
     * Xóa tài khoản của chính mình (soft delete)
     */
    @DeleteMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<MessageResponse> deleteMyAccount() {
        String username = SecurityUtil.getCurrentUsername();
        AccountResponse currentAccount = accountService.getCurrentAccount(username);
        MessageResponse response = accountService.deleteAccount(currentAccount.getId());
        return ResponseEntity.ok(response);
    }

    // ==================== ADMIN ENDPOINTS ====================

    /**
     * Admin: Lấy danh sách tất cả tài khoản
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<AccountResponse>> getAllAccounts() {
        List<AccountResponse> accounts = accountService.getAllAccounts();
        return ResponseEntity.ok(accounts);
    }

    /**
     * Admin: Lấy thông tin tài khoản theo ID
     */
    @GetMapping("/{accountId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AccountResponse> getAccountById(
            @PathVariable String accountId
    ) {
        AccountResponse response = accountService.getAccountById(accountId);
        return ResponseEntity.ok(response);
    }

    /**
     * Admin: Lấy danh sách lecturers
     */
    @GetMapping("/lecturers")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<LecturerResponse>> getLecturers(
            @RequestParam(required = false) String search
    ) {
        List<LecturerResponse> lecturers = accountService.getLecturers(search);
        return ResponseEntity.ok(lecturers);
    }

    /**
     * Admin: Lấy danh sách students
     */
    @GetMapping("/students")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<AccountResponse>> getStudents(
            @RequestParam(required = false) String search
    ) {
        List<AccountResponse> students = accountService.getStudents(search);
        return ResponseEntity.ok(students);
    }

    /**
     * Admin: Tạo tài khoản lecturer mới
     */
    @PostMapping("/lecturers")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AccountResponse> createLecturer(
            @Valid @RequestBody CreateLecturerRequest request
    ) {
        AccountResponse response = accountService.createLecturer(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Admin: Cập nhật tài khoản bất kỳ
     */
    @PutMapping("/{accountId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AccountResponse> adminUpdateAccount(
            @PathVariable String accountId,
            @Valid @RequestBody AdminUpdateAccountRequest request
    ) {
        AccountResponse response = accountService.adminUpdateAccount(accountId, request);
        return ResponseEntity.ok(response);
    }

    /**
     * Admin: Xóa tài khoản (soft delete)
     */
    @DeleteMapping("/{accountId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<MessageResponse> deleteAccount(
            @PathVariable String accountId
    ) {
        MessageResponse response = accountService.deleteAccount(accountId);
        return ResponseEntity.ok(response);
    }
}

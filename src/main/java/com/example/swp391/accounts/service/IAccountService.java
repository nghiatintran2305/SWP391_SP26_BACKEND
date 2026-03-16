package com.example.swp391.accounts.service;

import com.example.swp391.accounts.dto.request.*;
import com.example.swp391.accounts.dto.response.AccountLinkStatusResponse;
import com.example.swp391.accounts.dto.response.AccountResponse;
import com.example.swp391.accounts.dto.response.LecturerResponse;
import com.example.swp391.accounts.dto.response.LinkedStudentResponse;
import com.example.swp391.accounts.dto.response.MessageResponse;

import java.util.List;

public interface IAccountService {
    List<LecturerResponse> getLecturers(String keyword);

    // Student (User) operations
    AccountResponse registerStudent(CreateStudentRequest request);
    AccountResponse updateAccount(String accountId, UpdateAccountRequest request);
    MessageResponse deleteAccount(String accountId);
    MessageResponse changePassword(String accountId, ChangePasswordRequest request);
    AccountResponse getAccountById(String accountId);
    AccountResponse getCurrentAccount(String username);
    AccountLinkStatusResponse getAccountLinkStatus(String accountId);

    // Admin operations
    AccountResponse createLecturer(CreateLecturerRequest request);
    AccountResponse adminUpdateAccount(String accountId, AdminUpdateAccountRequest request);
    List<AccountResponse> getAllAccounts();
    List<AccountResponse> getStudents(String keyword);
    List<LinkedStudentResponse> getLinkedStudents();
}

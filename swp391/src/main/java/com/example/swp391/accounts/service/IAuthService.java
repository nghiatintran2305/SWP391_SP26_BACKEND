package com.example.swp391.accounts.service;

import com.example.swp391.accounts.dto.request.ForgotPasswordRequest;
import com.example.swp391.accounts.dto.request.LoginRequest;
import com.example.swp391.accounts.dto.request.ResetPasswordRequest;
import com.example.swp391.accounts.dto.response.MessageResponse;
import com.example.swp391.accounts.dto.response.LoginResponse;

public interface IAuthService {
    LoginResponse login(LoginRequest request);
    MessageResponse forgotPassword(ForgotPasswordRequest request);
    MessageResponse resetPassword(ResetPasswordRequest request);
}

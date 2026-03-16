package com.example.swp391.accounts.service;

import com.example.swp391.accounts.dto.request.LoginRequest;
import com.example.swp391.accounts.dto.response.LoginResponse;

import java.nio.file.AccessDeniedException;

public interface IAuthService {
    LoginResponse login(LoginRequest request);
}

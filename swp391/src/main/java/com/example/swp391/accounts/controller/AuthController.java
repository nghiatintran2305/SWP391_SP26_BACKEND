package com.example.swp391.accounts.controller;

import com.example.swp391.accounts.dto.request.LoginRequest;
import com.example.swp391.accounts.dto.response.LoginResponse;
import com.example.swp391.accounts.service.IAuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final IAuthService iAuthService;

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(
            @Valid @RequestBody LoginRequest request
    ) {
        System.out.println("LOGIN username = [" + request.getUsername() + "]");
        System.out.println("LOGIN password = [" + request.getPassword() + "]");
        return ResponseEntity.ok(iAuthService.login(request));
    }

    @PostMapping("/logout")
    public ResponseEntity<String> logout(
            @RequestHeader("Authorization") String authorizationHeader
    ) {

        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            return ResponseEntity.badRequest().body("Token không hợp lệ");
        }

        String token = authorizationHeader.substring(7);

        log.info("Logout token: {}...",
                token.substring(0, Math.min(20, token.length())));

        // TODO: blacklist token nếu dùng refresh token / long-lived token

        return ResponseEntity.ok("Logout thành công");
    }
}


package com.example.swp391.accounts.service.impl;

import com.example.swp391.accounts.dto.request.LoginRequest;
import com.example.swp391.accounts.dto.response.LoginResponse;
import com.example.swp391.accounts.entity.Account;
import com.example.swp391.accounts.repository.AccountRepository;
import com.example.swp391.accounts.service.IAuthService;
import com.example.swp391.configs.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements IAuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthServiceImpl.class);

    private final AccountRepository accountRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    @Override
    public LoginResponse login(LoginRequest request) {
        Account account = accountRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new BadCredentialsException("Username or password is incorrect"));

        if (!account.isActive()) {
            throw new BadCredentialsException("Account has been deactivated");
        }

        boolean matches = passwordEncoder.matches(request.getPassword(), account.getPassword());
        if (!matches) {
            throw new BadCredentialsException("Username or password is incorrect");
        }

        String roleName = account.getRole() != null ? account.getRole().getName() : null;

        String token = jwtUtil.generateToken(
                account.getUsername(),
                roleName == null ? List.of() : List.of(roleName));

        return LoginResponse.builder()
                .token(token)
                .roles(roleName == null ? "[]" : String.valueOf(List.of(roleName)))
                .build();
    }
}
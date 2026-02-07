package com.example.swp391.accounts.service.impl;

import com.example.swp391.accounts.dto.request.LoginRequest;
import com.example.swp391.accounts.dto.response.LoginResponse;
import com.example.swp391.accounts.entity.Account;
import com.example.swp391.accounts.enums.LoginType;
import com.example.swp391.accounts.repository.AccountRepository;
import com.example.swp391.accounts.service.IAuthService;
import com.example.swp391.config.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.security.access.AccessDeniedException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements IAuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthServiceImpl.class);

    private final AccountRepository accountRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public LoginResponse login(LoginRequest request) {
        log.info("LOGIN username = [{}], password = [{}]",
                request.getUsername(),
                request.getPassword()
        );

        Account account = accountRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new BadCredentialsException("Username or password is incorrect"));

        boolean matches = passwordEncoder.matches(request.getPassword(), account.getPassword());
        log.debug("password match for user {}: {}", request.getUsername(), matches);

        if (!matches) {
            throw new BadCredentialsException("Username or password is incorrect");
        }

        String roleName = account.getRole() != null
                ? account.getRole().getName()
                : null;

        if (request.getLoginType() == LoginType.ADMIN) {
            if (!"ADMIN".equals(roleName)) {
                throw new AccessDeniedException("Only ADMIN can login to admin page");
            }
        }

        String token = jwtUtil.generateToken(
                account.getUsername(),
                roleName == null ? List.of() : List.of(roleName)
        );


        log.info("LOGIN success username={}", request.getUsername());

        return LoginResponse.builder()
                .token(token)
                .roles(roleName == null ? List.of().toString() : String.valueOf(List.of(roleName)))
                .build();
    }

}
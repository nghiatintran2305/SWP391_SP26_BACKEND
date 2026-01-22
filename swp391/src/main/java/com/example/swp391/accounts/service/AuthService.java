package com.example.swp391.accounts.service;

import com.example.swp391.accounts.dto.request.LoginRequest;
import com.example.swp391.accounts.dto.response.LoginResponse;
import com.example.swp391.accounts.entity.Account;
import com.example.swp391.accounts.repository.AccountRepository;
import com.example.swp391.jwt.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

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

        String token = jwtUtil.generateToken(
                account.getUsername(),
                account.getRoles() == null ? java.util.List.of() :
                        account.getRoles().stream().map(r -> r.getName()).toList()
        );

        return LoginResponse.builder()
                .token(token)
                .roles(account.getRoles() == null ? java.util.List.of() :
                        account.getRoles().stream().map(r -> r.getName()).toList())
                .build();
    }
}
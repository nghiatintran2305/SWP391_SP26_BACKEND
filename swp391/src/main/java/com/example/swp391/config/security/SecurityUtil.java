package com.example.swp391.config.security;

import com.example.swp391.accounts.entity.Account;
import com.example.swp391.accounts.repository.AccountRepository;
import com.example.swp391.exception.UnauthorizedException;
import lombok.AllArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@AllArgsConstructor
@Component
public class SecurityUtil {
    private final AccountRepository accountRepository;

    public Account getCurrentAccount() {
        Authentication auth = SecurityContextHolder
                .getContext()
                .getAuthentication();

        if (auth == null || !auth.isAuthenticated()
                || auth.getPrincipal().equals("anonymousUser")) {
            throw new UnauthorizedException("Chưa xác thực");
        }

        String username = auth.getName();

        return accountRepository.findByUsername(username)
                .orElseThrow(() ->
                        new RuntimeException("Current user not found"));
    }

    /**
     * Static method để lấy username của user hiện tại
     */
    public static String getCurrentUsername() {
        Authentication auth = SecurityContextHolder
                .getContext()
                .getAuthentication();

        if (auth == null || !auth.isAuthenticated()
                || auth.getPrincipal().equals("anonymousUser")) {
            throw new UnauthorizedException("Chưa xác thực");
        }

        return auth.getName();
    }
}

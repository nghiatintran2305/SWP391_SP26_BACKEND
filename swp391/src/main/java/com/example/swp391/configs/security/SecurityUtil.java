package com.example.swp391.configs.security;

import com.example.swp391.accounts.entity.Account;
import com.example.swp391.accounts.repository.AccountRepository;
import com.example.swp391.exceptions.UnauthorizedException;
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
     * Static method to get username of current user
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

    /**
     * Static method to get user ID of current user
     */
    public static String getCurrentUserId(AccountRepository accountRepository) {
        String username = getCurrentUsername();
        Account account = accountRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return account.getId();
    }

    /**
     * Static method to get user ID of current user (used in controller that has AccountRepository)
     */
    public String getCurrentUserId() {
        Account account = getCurrentAccount();
        return account.getId();
    }
}

package com.example.swp391.config;

import com.example.swp391.accounts.entity.Account;
import com.example.swp391.accounts.entity.Role;
import com.example.swp391.accounts.repository.AccountRepository;
import com.example.swp391.accounts.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Set;

@Configuration
@RequiredArgsConstructor
public class DataInitializer {

    private final RoleRepository roleRepository;
    private final AccountRepository accountRepository;
    private final PasswordEncoder passwordEncoder;

    @Bean
    CommandLineRunner initData() {
        return args -> {

            // ===== 1. CREATE ROLES =====
            Role adminRole = createRoleIfNotExists("ADMIN");
            Role lecturerRole = createRoleIfNotExists("LECTURER");
            Role studentRole = createRoleIfNotExists("STUDENT");

            // ===== 2. CREATE ACCOUNTS =====
            createAccountIfNotExists(
                    "admin1", "admin1@gmail.com", "123456", adminRole);
            createAccountIfNotExists(
                    "admin2", "admin2@gmail.com", "123456", adminRole);
            createAccountIfNotExists(
                    "admin3", "admin3@gmail.com", "123456", adminRole);

            createAccountIfNotExists(
                    "lecturer1", "lecturer1@gmail.com", "123456", lecturerRole);
            createAccountIfNotExists(
                    "lecturer2", "lecturer2@gmail.com", "123456", lecturerRole);
            createAccountIfNotExists(
                    "lecturer3", "lecturer3@gmail.com", "123456", lecturerRole);

            createAccountIfNotExists(
                    "student1", "student1@gmail.com", "123456", studentRole);
            createAccountIfNotExists(
                    "student2", "student2@gmail.com", "123456", studentRole);
            createAccountIfNotExists(
                    "student3", "student3@gmail.com", "123456", studentRole);
        };
    }

    private Role createRoleIfNotExists(String roleName) {
        return roleRepository.findByName(roleName)
                .orElseGet(() -> roleRepository.save(
                        Role.builder()
                                .name(roleName)
                                .build()
                ));
    }

    private void createAccountIfNotExists(
            String username,
            String email,
            String rawPassword,
            Role role
    ) {
        if (accountRepository.existsByUsername(username)) {
            return;
        }

        Account account = Account.builder()
                .username(username)
                .email(email)
                .password(passwordEncoder.encode(rawPassword))
                .role(role)
                .isActive(true)
                .build();

        accountRepository.save(account);
    }
}


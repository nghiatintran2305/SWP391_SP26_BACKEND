package com.example.swp391.accounts.service.impl;

import com.example.swp391.accounts.dto.response.LecturerResponse;
import com.example.swp391.accounts.entity.Account;
import com.example.swp391.accounts.entity.Role;
import com.example.swp391.accounts.repository.AccountRepository;
import com.example.swp391.accounts.repository.RoleRepository;
import com.example.swp391.accounts.service.IAccountService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional()
public class AccountServiceImpl implements IAccountService {

    private final AccountRepository accountRepository;
    private final RoleRepository roleRepository;

    @Override
    public List<LecturerResponse> getLecturers(String keyword) {

        List<Account> lecturers;

        Role lecturerRole = roleRepository.findByName("LECTURER")
                .orElseThrow(() -> new RuntimeException("Role not found"));

        if (keyword == null || keyword.trim().isEmpty()) {
            lecturers = accountRepository.findByRole(lecturerRole);
        } else {
            lecturers = accountRepository
                    .findByRoleAndUsernameContainingIgnoreCase(
                            lecturerRole,
                            keyword.trim()
                    );
        }

        return lecturers.stream()
                .map(a -> new LecturerResponse(
                        a.getId(),
                        a.getUsername(),
                        a.getEmail(),
                        a.getDetails() != null
                                ? a.getDetails().getFullName()
                                : null
                ))
                .toList();
    }
}


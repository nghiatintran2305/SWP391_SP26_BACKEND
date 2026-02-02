package com.example.swp391.accounts.repository;

import com.example.swp391.accounts.entity.Account;
import com.example.swp391.accounts.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AccountRepository extends JpaRepository<Account, String> {
    Optional<Account> findByUsername(String username);
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
    boolean existsByEmailAndIdNot(String email, String id);
    List<Account> findByRole(Role role);
    List<Account> findByRoleAndUsernameContainingIgnoreCase(Role role ,String username);
}

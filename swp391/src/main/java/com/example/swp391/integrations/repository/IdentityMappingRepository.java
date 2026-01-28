package com.example.swp391.integrations.repository;

import com.example.swp391.integrations.entity.IdentityMapping;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IdentityMappingRepository extends JpaRepository<IdentityMapping, String> {
    Optional<IdentityMapping> findByAccountId(String accountId);
    boolean existsByAccountId(String accountId);
}

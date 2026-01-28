package com.example.swp391.integrations.repository;

import com.example.swp391.integrations.entity.IntegrationConfig;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IntegrationConfigRepository extends JpaRepository<IntegrationConfig, String> {
    Optional<IntegrationConfig> findByGroupId(String groupId);
    boolean existsByGroupId(String groupId);
}

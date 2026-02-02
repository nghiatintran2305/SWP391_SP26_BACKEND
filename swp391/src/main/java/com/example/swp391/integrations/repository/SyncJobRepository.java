package com.example.swp391.integrations.repository;

import com.example.swp391.integrations.entity.SyncJob;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SyncJobRepository extends JpaRepository<SyncJob, String> {
}

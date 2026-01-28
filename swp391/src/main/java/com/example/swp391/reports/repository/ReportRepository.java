package com.example.swp391.reports.repository;

import com.example.swp391.reports.entity.Report;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReportRepository extends JpaRepository<Report, String> {
    List<Report> findByGroupId(String groupId);
}

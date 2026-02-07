package com.example.swp391.projects.enums;

public enum GroupStatus {
    CREATED,      // mới tạo, chưa cấu hình
    CONFIGURED,   // đã có Jira + Git
    ACTIVE,       // đang làm
    LOCKED,      // khoá, không cho thêm member
    COMPLETED,    // kết thúc
}

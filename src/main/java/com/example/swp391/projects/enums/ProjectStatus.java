package com.example.swp391.projects.enums;

public enum ProjectStatus {
    CONFIGURED,   // has Jira + Git configured
    ACTIVE,       // in progress
    LOCKED,       // locked, cannot add members
    COMPLETED,    // finished
}

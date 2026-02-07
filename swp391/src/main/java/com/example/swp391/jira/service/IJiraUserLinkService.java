package com.example.swp391.jira.service;

import com.example.swp391.jira.entity.JiraUserMapping;
import org.springframework.transaction.annotation.Transactional;

public interface IJiraUserLinkService {
    void unlink();

    String getAuthorizeUrl();

    @Transactional
    void handleCallback(String code);

    JiraUserMapping getCurrentMapping();
}

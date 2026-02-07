package com.example.swp391.jira.controller;

import com.example.swp391.jira.service.impl.JiraUserLinkServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.example.swp391.jira.entity.JiraUserMapping;

@RestController
@RequestMapping("/api/v1/jira")
@RequiredArgsConstructor
public class JiraLinkController {

    private final JiraUserLinkServiceImpl jiraUserLinkService;

    //get authorize url
    @GetMapping("/oauth/authorize")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<String> authorize() {
        return ResponseEntity.ok(jiraUserLinkService.getAuthorizeUrl());
    }

    //handle callback
    @GetMapping("/oauth/callback")
    public ResponseEntity<Void> callback(@RequestParam String code) {
        System.out.println("Received OAuth callback with code: " + code);
        jiraUserLinkService.handleCallback(code);
        return ResponseEntity.ok().build();
    }

    //show link status
    @GetMapping("/link")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<JiraUserMapping> getLinkStatus() {
        return ResponseEntity.ok(
                jiraUserLinkService.getCurrentMapping()
        );
    }

    //unlink jira
    @DeleteMapping("/link")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> unlinkJira() {
        jiraUserLinkService.unlink();
        return ResponseEntity.ok("Đã huỷ liên kết Jira");
    }
}


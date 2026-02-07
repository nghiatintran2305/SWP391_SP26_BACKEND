package com.example.swp391.github.controller;

import com.example.swp391.github.entity.GithubUserMapping;
import com.example.swp391.github.service.IGitUserLinkService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/github")
@RequiredArgsConstructor
public class GithubUserLinkController {

    private final IGitUserLinkService gitUserLinkService;

    //FE gọi để lấy URL redirect sang GitHub OAuth
    @GetMapping("/authorize-url")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<String> getAuthorizeUrl() {
        return ResponseEntity.ok(gitUserLinkService.getAuthorizeUrl());
    }

    //GitHub redirect về sau khi user authorize
    @GetMapping("/callback")
    public ResponseEntity<Void> handleCallback(@RequestParam String code) {
        gitUserLinkService.handleCallback(code);
        return ResponseEntity.noContent().build();
    }

    //Lấy GitHub mapping của user hiện tại
    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<GithubUserMapping> getCurrentMapping() {
        return ResponseEntity.ok(gitUserLinkService.getCurrentMapping());
    }

    //Hủy liên kết GitHub
    @DeleteMapping("/unlink")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> unlink() {
        gitUserLinkService.unlink();
        return ResponseEntity.noContent().build();
    }
}

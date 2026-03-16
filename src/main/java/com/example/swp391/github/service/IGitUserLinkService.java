package com.example.swp391.github.service;

import com.example.swp391.github.entity.GithubUserMapping;
import org.springframework.transaction.annotation.Transactional;

public interface IGitUserLinkService {

    String getAuthorizeUrl();

    @Transactional
    void handleCallback(String code);

    GithubUserMapping getCurrentMapping();

    @Transactional
    void unlink();
}

package com.example.swp391.github.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

@Component
@RequiredArgsConstructor
public class GitAdminClient {

    private final RestTemplate restTemplate;

    @Value("${github.base-url}")
    private String baseUrl;

    public boolean validateUsername(String username) {
        try {
            restTemplate.getForEntity(
                    baseUrl + "/users/" + username,
                    Object.class
            );
            return true;
        } catch (HttpClientErrorException.NotFound e) {
            return false;
        }
    }
}


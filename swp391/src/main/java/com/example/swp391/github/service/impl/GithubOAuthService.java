package com.example.swp391.github.service.impl;

import com.example.swp391.accounts.entity.Account;
import com.example.swp391.config.security.SecurityUtil;
import com.example.swp391.github.dto.response.GithubUserResponse;
import com.example.swp391.github.entity.GithubUserMapping;
import com.example.swp391.github.enums.GithubLinkStatus;
import com.example.swp391.github.repository.GithubUserMappingRepository;
import com.example.swp391.github.service.IGitUserLinkService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class GithubOAuthService{

    @Value("${github.oauth.client-id}")
    private String clientId;

    @Value("${github.oauth.client-secret}")
    private String clientSecret;

    private final GithubUserMappingRepository repo;
    private final SecurityUtil securityUtil;
    private final RestTemplate restTemplate;

    //Tạo link OAuth
    public String buildAuthorizeUrl() {
        return "https://github.com/login/oauth/authorize"
                + "?client_id=" + clientId
                + "&scope=read:user";
    }

    //Callback
    @Transactional
    public void handleCallback(String code) {

        Account account = securityUtil.getCurrentAccount();

        String accessToken = exchangeToken(code);
        GithubUserResponse user = fetchUser(accessToken);

        // tránh link GitHub đã dùng cho account khác
        if (repo.existsByGithubUserId(user.getId())) {
            throw new IllegalStateException("GitHub account đã được liên kết");
        }

        GithubUserMapping mapping = repo.findByAccount(account)
                .orElse(new GithubUserMapping());

        mapping.setAccount(account);
        mapping.setGithubUserId(user.getId());
        mapping.setGithubUsername(user.getLogin());
        mapping.setStatus(GithubLinkStatus.LINKED);

        repo.save(mapping);
    }

    //exchange code -> token
    private String exchangeToken(String code) {

        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));

        Map<String, String> body = Map.of(
                "client_id", clientId,
                "client_secret", clientSecret,
                "code", code
        );

        HttpEntity<Map<String, String>> entity =
                new HttpEntity<>(body, headers);

        ResponseEntity<Map> res = restTemplate.postForEntity(
                "https://github.com/login/oauth/access_token",
                entity,
                Map.class
        );

        return (String) res.getBody().get("access_token");
    }

    //get user info
    private GithubUserResponse fetchUser(String token) {

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);

        HttpEntity<Void> entity = new HttpEntity<>(headers);

        ResponseEntity<GithubUserResponse> res =
                restTemplate.exchange(
                        "https://api.github.com/user",
                        HttpMethod.GET,
                        entity,
                        GithubUserResponse.class
                );

        return res.getBody();
    }

}



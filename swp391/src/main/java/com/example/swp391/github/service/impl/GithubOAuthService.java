package com.example.swp391.github.service.impl;

import com.example.swp391.accounts.entity.Account;
import com.example.swp391.configs.security.SecurityUtil;
import com.example.swp391.github.dto.response.GithubUserResponse;
import com.example.swp391.github.entity.GithubUserMapping;
import com.example.swp391.github.enums.GithubLinkStatus;
import com.example.swp391.github.repository.GithubUserMappingRepository;
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
public class GithubOAuthService {

    @Value("${github.oauth.client-id}")
    private String clientId;

    @Value("${github.oauth.client-secret}")
    private String clientSecret;

    @Value("${github.oauth.redirect-uri}")
    private String redirectUri;

    private final GithubUserMappingRepository repo;
    private final SecurityUtil securityUtil;
    private final RestTemplate restTemplate;

    // Build OAuth link
    public String buildAuthorizeUrl() {
        return "https://github.com/login/oauth/authorize"
                + "?client_id=" + clientId
                + "&redirect_uri=" + redirectUri
                + "&scope=read:user"
                + "&prompt=select_account"
                + "&state=github";
    }

    // Callback
    @Transactional
    public void handleCallback(String code) {

        Account account = securityUtil.getCurrentAccount();

        String accessToken = exchangeToken(code);
        GithubUserResponse user = fetchUser(accessToken);

        GithubUserMapping existingByAccount = repo.findByAccount(account).orElse(null);

        // Nếu account hiện tại đã linked đúng GitHub account này rồi
        // thì coi như thành công, không ném lỗi nữa
        if (existingByAccount != null
                && user.getId().equals(existingByAccount.getGithubUserId())
                && existingByAccount.getStatus() == GithubLinkStatus.LINKED) {
            return;
        }

        // Nếu GitHub account này đã linked ở đâu đó
        if (repo.existsByGithubUserId(user.getId())) {

            GithubUserMapping existingByGithub = repo.findAll().stream()
                    .filter(m -> user.getId().equals(m.getGithubUserId()))
                    .findFirst()
                    .orElse(null);

            // Đã linked với account khác -> chặn
            if (existingByGithub != null
                    && existingByGithub.getAccount() != null
                    && !existingByGithub.getAccount().getId().equals(account.getId())) {
                throw new IllegalStateException("This GitHub account is already linked to another local account");
            }

            // Đã linked với chính account hiện tại -> update lại nhẹ rồi return
            if (existingByGithub != null
                    && existingByGithub.getAccount() != null
                    && existingByGithub.getAccount().getId().equals(account.getId())) {
                existingByGithub.setGithubUsername(user.getLogin());
                existingByGithub.setStatus(GithubLinkStatus.LINKED);
                repo.save(existingByGithub);
                return;
            }
        }

        GithubUserMapping mapping = existingByAccount != null
                ? existingByAccount
                : new GithubUserMapping();

        mapping.setAccount(account);
        mapping.setGithubUserId(user.getId());
        mapping.setGithubUsername(user.getLogin());
        mapping.setStatus(GithubLinkStatus.LINKED);

        repo.save(mapping);
    }

    // exchange code -> token
    private String exchangeToken(String code) {

        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));

        Map<String, String> body = Map.of(
                "client_id", clientId,
                "client_secret", clientSecret,
                "code", code,
                "redirect_uri", redirectUri
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

    // get user info
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
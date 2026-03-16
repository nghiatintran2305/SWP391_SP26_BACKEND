package com.example.swp391.webhook.controller;

import com.example.swp391.webhook.dto.GithubPushEvent;
import com.example.swp391.webhook.service.IWebhookService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/webhooks")
@RequiredArgsConstructor
@Slf4j
public class WebhookController {

    private final IWebhookService webhookService;
    private final ObjectMapper objectMapper;

    //GitHub Webhook endpoint
    @PostMapping("/github")
    public ResponseEntity<Void> handleGithubWebhook(
            @RequestHeader(value = "X-GitHub-Event", required = false) String eventType,
            @RequestBody String payload) {
        
        log.info("Received GitHub webhook event: {}", eventType);

        // Only process push events
        if (!"push".equalsIgnoreCase(eventType)) {
            log.debug("Ignoring non-push event: {}", eventType);
            return ResponseEntity.ok().build();
        }

        try {
            GithubPushEvent event = objectMapper.readValue(payload, GithubPushEvent.class);
            
            // Process the push event
            webhookService.handleGithubPush(event);
            
            return ResponseEntity.ok().build();
            
        } catch (Exception e) {
            log.error("Failed to process GitHub webhook", e);
            return ResponseEntity.badRequest().build();
        }
    }
}

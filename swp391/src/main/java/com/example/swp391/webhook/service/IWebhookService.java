package com.example.swp391.webhook.service;

import com.example.swp391.webhook.dto.GithubPushEvent;

public interface IWebhookService {
    
    //Handle GitHub push event - parse commits and update Jira issues
    void handleGithubPush(GithubPushEvent event);
}

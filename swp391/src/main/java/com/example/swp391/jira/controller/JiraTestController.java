package com.example.swp391.jira.controller;

import com.example.swp391.jira.service.IJiraService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/test/jira")
public class JiraTestController {

    private final IJiraService jiraService;

    @PostMapping("/create-group")
    public String createGroup(@RequestParam String name) {
        return jiraService.createGroup(name);
    }
}

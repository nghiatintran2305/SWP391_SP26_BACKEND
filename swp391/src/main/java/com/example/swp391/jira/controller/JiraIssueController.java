package com.example.swp391.jira.controller;

import com.example.swp391.jira.dto.response.JiraIssueResponse;
import com.example.swp391.jira.service.IJiraService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class JiraIssueController {

    private final IJiraService jiraService;

    //Lấy danh sách Issue (Admin/Giảng viên/Trưởng nhóm)

    @PreAuthorize("hasAnyRole('ADMIN', 'LECTURER', 'LEADER')")
    @GetMapping("/projects/{projectKey}/jira/issues")
    public ResponseEntity<List<JiraIssueResponse>> getProjectIssues(
            @PathVariable String projectKey
    ) {
        List<JiraIssueResponse> issues = jiraService.getProjectIssues(projectKey);
        return ResponseEntity.ok(issues);
    }

    //Lấy chi tiết Issue

    @PreAuthorize("hasAnyRole('ADMIN', 'LECTURER', 'LEADER', 'MEMBER')")
    @GetMapping("/projects/{projectKey}/jira/issues/{issueKey}")
    public ResponseEntity<JiraIssueResponse> getIssue(
            @PathVariable String projectKey,
            @PathVariable String issueKey
    ) {
        JiraIssueResponse issue = jiraService.getIssueByKey(projectKey, issueKey);
        return ResponseEntity.ok(issue);
    }

    //Tạo Issue (Admin/Giảng viên/Trưởng nhóm)

    @PreAuthorize("hasAnyRole('ADMIN', 'LECTURER', 'LEADER')")
    @PostMapping("/projects/{projectKey}/jira/issues")
    public ResponseEntity<JiraIssueResponse> createIssue(
            @PathVariable String projectKey,
            @RequestParam String summary,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) String issueType,
            @RequestParam(required = false) String priority,
            @RequestParam(required = false) String assigneeAccountId
    ) {
        JiraIssueResponse issue = jiraService.createIssue(
                projectKey, summary, description, issueType, priority, assigneeAccountId
        );
        return ResponseEntity.ok(issue);
    }

    //Cập nhật trạng thái Issue

    @PreAuthorize("hasAnyRole('ADMIN', 'LECTURER', 'LEADER', 'MEMBER')")
    @PutMapping("/projects/{projectKey}/jira/issues/{issueKey}/status")
    public ResponseEntity<JiraIssueResponse> updateIssueStatus(
            @PathVariable String projectKey,
            @PathVariable String issueKey,
            @RequestParam String status
    ) {
        JiraIssueResponse issue = jiraService.updateIssueStatus(issueKey, status);
        return ResponseEntity.ok(issue);
    }

    //Xóa Issue

    @PreAuthorize("hasAnyRole('ADMIN', 'LECTURER')")
    @DeleteMapping("/projects/{projectKey}/jira/issues/{issueKey}")
    public ResponseEntity<Void> deleteIssue(
            @PathVariable String projectKey,
            @PathVariable String issueKey
    ) {
        jiraService.deleteIssue(issueKey);
        return ResponseEntity.noContent().build();
    }
}

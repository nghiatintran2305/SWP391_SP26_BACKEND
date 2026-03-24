package com.example.swp391.tasks.controller;

import com.example.swp391.accounts.repository.AccountRepository;
import com.example.swp391.configs.security.SecurityUtil;
import com.example.swp391.tasks.dto.request.CreateTaskRequest;
import com.example.swp391.tasks.dto.request.UpdateTaskRequest;
import com.example.swp391.tasks.dto.response.TaskProgressReport;
import com.example.swp391.tasks.dto.response.TaskResponse;
import com.example.swp391.tasks.dto.response.UserTaskStats;
import com.example.swp391.tasks.enums.TaskStatus;
import com.example.swp391.tasks.service.ITaskService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class TaskController {

    private final ITaskService taskService;
    private final AccountRepository accountRepository;

    //CRUD Task

    @PreAuthorize("hasAnyRole('ADMIN', 'LECTURER')")
    @PostMapping("/projects/{projectId}/tasks")
    public ResponseEntity<TaskResponse> createTask(
            @PathVariable String projectId,
            @Valid @RequestBody CreateTaskRequest request
    ) {
        String currentUserId = SecurityUtil.getCurrentUserId(accountRepository);
        TaskResponse response = taskService.createTask(projectId, request, currentUserId);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'LECTURER', 'STUDENT')")
    @PutMapping("/tasks/{taskId}")
    public ResponseEntity<TaskResponse> updateTask(
            @PathVariable String taskId,
            @Valid @RequestBody UpdateTaskRequest request
    ) {
        TaskResponse response = taskService.updateTask(taskId, request);
        return ResponseEntity.ok(response);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'LECTURER')")
    @DeleteMapping("/tasks/{taskId}")
    public ResponseEntity<Void> deleteTask(@PathVariable String taskId) {
        taskService.deleteTask(taskId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/tasks/{taskId}")
    public ResponseEntity<TaskResponse> getTaskById(@PathVariable String taskId) {
        TaskResponse response = taskService.getTaskById(taskId);
        return ResponseEntity.ok(response);
    }
    //Get list of Tasks
    @PreAuthorize("hasAnyRole('ADMIN', 'LECTURER', 'STUDENT')")
    @GetMapping("/projects/{projectId}/tasks")
    public ResponseEntity<List<TaskResponse>> getTasksByProject(@PathVariable String projectId) {
        List<TaskResponse> responses = taskService.getTasksByProject(projectId);
        return ResponseEntity.ok(responses);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'LECTURER', 'STUDENT')")
    @GetMapping("/projects/{projectId}/requirements")
    public ResponseEntity<List<TaskResponse>> getRequirementsByProject(@PathVariable String projectId) {
        List<TaskResponse> responses = taskService.getRequirementsByProject(projectId);
        return ResponseEntity.ok(responses);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'LECTURER', 'STUDENT')")
    @GetMapping("/projects/{projectId}/tasks/list")
    public ResponseEntity<List<TaskResponse>> getTasksOnlyByProject(@PathVariable String projectId) {
        List<TaskResponse> responses = taskService.getTasksOnlyByProject(projectId);
        return ResponseEntity.ok(responses);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'LECTURER', 'STUDENT')")
    @GetMapping("/projects/{projectId}/tasks/status/{status}")
    public ResponseEntity<List<TaskResponse>> getTasksByProjectAndStatus(
            @PathVariable String projectId,
            @PathVariable TaskStatus status
    ) {
        List<TaskResponse> responses = taskService.getTasksByProjectAndStatus(projectId, status);
        return ResponseEntity.ok(responses);
    }

    //Tasks of User

    @GetMapping("/users/me/tasks")
    public ResponseEntity<List<TaskResponse>> getMyTasks() {
        String currentUserId = SecurityUtil.getCurrentUserId(accountRepository);
        List<TaskResponse> responses = taskService.getTasksAssignedToUser(currentUserId);
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/users/{userId}/tasks")
    public ResponseEntity<List<TaskResponse>> getUserTasks(@PathVariable String userId) {
        List<TaskResponse> responses = taskService.getTasksAssignedToUser(userId);
        return ResponseEntity.ok(responses);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'LECTURER', 'STUDENT')")
    @GetMapping("/projects/{projectId}/members/{userId}/tasks")
    public ResponseEntity<List<TaskResponse>> getTasksByProjectAndUser(
            @PathVariable String projectId,
            @PathVariable String userId
    ) {
        List<TaskResponse> responses = taskService.getTasksByProjectAndUser(projectId, userId);
        return ResponseEntity.ok(responses);
    }

    //Assign Task

    @PreAuthorize("hasAnyRole('ADMIN', 'LECTURER', 'STUDENT')")
    @PutMapping("/tasks/{taskId}/assign/{userId}")
    public ResponseEntity<TaskResponse> assignTask(
            @PathVariable String taskId,
            @PathVariable String userId
    ) {
        TaskResponse response = taskService.assignTaskToUser(taskId, userId);
        return ResponseEntity.ok(response);
    }

    //Update Task status

    @PutMapping("/tasks/{taskId}/status")
    public ResponseEntity<TaskResponse> updateTaskStatus(
            @PathVariable String taskId,
            @RequestParam TaskStatus status
    ) {
        TaskResponse response = taskService.updateTaskStatus(taskId, status);
        return ResponseEntity.ok(response);
    }

    //Progress report

    @PreAuthorize("hasAnyRole('ADMIN', 'LECTURER', 'STUDENT')")
    @GetMapping("/projects/{projectId}/progress")
    public ResponseEntity<TaskProgressReport> getProjectProgress(@PathVariable String projectId) {
        TaskProgressReport report = taskService.getProjectProgressReport(projectId);
        return ResponseEntity.ok(report);
    }

    //User statistics

    @GetMapping("/users/me/stats/tasks")
    public ResponseEntity<UserTaskStats> getMyTaskStats() {
        String currentUserId = SecurityUtil.getCurrentUserId(accountRepository);
        UserTaskStats stats = taskService.getUserTaskStats(currentUserId);
        return ResponseEntity.ok(stats);
    }

    @GetMapping("/users/{userId}/stats/tasks")
    public ResponseEntity<UserTaskStats> getUserTaskStats(@PathVariable String userId) {
        UserTaskStats stats = taskService.getUserTaskStats(userId);
        return ResponseEntity.ok(stats);
    }


}

package com.example.swp391.tasks.service;

import com.example.swp391.tasks.dto.request.CreateTaskRequest;
import com.example.swp391.tasks.dto.request.UpdateTaskRequest;
import com.example.swp391.tasks.dto.response.TaskProgressReport;
import com.example.swp391.tasks.dto.response.TaskResponse;
import com.example.swp391.tasks.dto.response.UserTaskStats;
import com.example.swp391.tasks.enums.TaskStatus;

import java.util.List;

public interface ITaskService {

    TaskResponse createTask(String projectId, CreateTaskRequest request, String createdById);

    TaskResponse updateTask(String taskId, UpdateTaskRequest request);

    void deleteTask(String taskId);

    TaskResponse getTaskById(String taskId);

    List<TaskResponse> getTasksByProject(String projectId);

    List<TaskResponse> getTasksByProjectAndStatus(String projectId, TaskStatus status);

    List<TaskResponse> getTasksAssignedToUser(String accountId);

    List<TaskResponse> getTasksByProjectAndUser(String projectId, String accountId);

    List<TaskResponse> getRequirementsByProject(String projectId);

    List<TaskResponse> getTasksOnlyByProject(String projectId);

    TaskResponse assignTaskToUser(String taskId, String accountId);

    TaskResponse updateTaskStatus(String taskId, TaskStatus status);

    TaskProgressReport getProjectProgressReport(String projectId);

    UserTaskStats getUserTaskStats(String accountId);

}

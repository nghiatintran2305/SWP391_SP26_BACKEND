package com.example.swp391.tasks.service.impl;

import com.example.swp391.accounts.entity.Account;
import com.example.swp391.accounts.repository.AccountRepository;
import com.example.swp391.exceptions.BadRequestException;
import com.example.swp391.exceptions.NotFoundException;
import com.example.swp391.github.repository.GithubUserMappingRepository;
import com.example.swp391.github.service.IGithubService;
import com.example.swp391.jira.dto.response.JiraIssueResponse;
import com.example.swp391.jira.entity.JiraUserMapping;
import com.example.swp391.jira.repository.JiraUserMappingRepository;
import com.example.swp391.jira.service.IJiraService;
import com.example.swp391.projects.entity.Project;
import com.example.swp391.projects.repository.ProjectRepository;
import com.example.swp391.tasks.dto.request.CreateTaskRequest;
import com.example.swp391.tasks.dto.request.UpdateTaskRequest;
import com.example.swp391.tasks.dto.response.TaskProgressReport;
import com.example.swp391.tasks.dto.response.TaskResponse;
import com.example.swp391.tasks.dto.response.UserTaskStats;
import com.example.swp391.tasks.entity.Task;
import com.example.swp391.tasks.enums.TaskStatus;
import com.example.swp391.tasks.repository.TaskRepository;
import com.example.swp391.tasks.service.ITaskService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TaskServiceImpl implements ITaskService {

    private final TaskRepository taskRepository;
    private final ProjectRepository projectRepository;
    private final AccountRepository accountRepository;
    private final JiraUserMappingRepository jiraUserMappingRepository;
    private final GithubUserMappingRepository githubUserMappingRepository;
    private final IJiraService jiraService;
    private final IGithubService githubService;

    @Override
    @Transactional
    public TaskResponse createTask(String projectId, CreateTaskRequest request, String createdById) {
        // Validate project exists
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new NotFoundException("Project not found with id: " + projectId));

        // Validate creator exists
        Account createdBy = accountRepository.findById(createdById)
                .orElseThrow(() -> new NotFoundException("Creator not found with id: " + createdById));

        // Validate required fields
        if (request.getTaskName() == null || request.getTaskName().trim().isEmpty()) {
            throw new BadRequestException("Task name is required");
        }
        if (request.getStatus() == null) {
            throw new BadRequestException("Status is required");
        }
        if (request.getPriority() == null) {
            throw new BadRequestException("Priority is required");
        }

        // Validate project has Jira project key configured
        if (project.getJiraProjectKey() == null || project.getJiraProjectKey().trim().isEmpty()) {
            throw new BadRequestException("Project does not have Jira project key configured");
        }

        Account assignedTo = null;
        String assigneeJiraAccountId = null;

        // Find assignee if assignedToId is provided
        if (request.getAssignedToId() != null && !request.getAssignedToId().isEmpty()) {
            assignedTo = accountRepository.findById(request.getAssignedToId())
                    .orElseThrow(() -> new NotFoundException("Assignee not found with id: " + request.getAssignedToId()));
            
            // Get Jira account ID of the assignee
            assigneeJiraAccountId = jiraUserMappingRepository.findByAccountId(assignedTo.getId())
                    .map(JiraUserMapping::getJiraAccountId)
                    .orElse(null);
        }

        // Create issue on Jira - MUST SUCCEED BEFORE SAVING TO DB
        // Map priority to Jira format
        String jiraPriority = mapPriorityToJira(request.getPriority());
        
        JiraIssueResponse jiraIssue = jiraService.createIssue(
                project.getJiraProjectKey(),
                request.getTaskName(),
                request.getDescription(),
                request.isRequirement() ? "Story" : "Task",
                jiraPriority,
                assigneeJiraAccountId
        );

        // Only save to DB after Jira creation succeeds
        Task task = Task.builder()
                .project(project)
                .taskName(request.getTaskName())
                .description(request.getDescription())
                .jiraIssueId(jiraIssue.getId())
                .jiraIssueKey(jiraIssue.getKey())
                .status(request.getStatus())
                .priority(request.getPriority())
                .assignedTo(assignedTo)
                .createdBy(createdBy)
                .dueDate(request.getDueDate())
                .isRequirement(request.isRequirement())
                .build();

        Task saved = taskRepository.save(task);
        return mapToResponse(saved);
    }

    @Override
    @Transactional
    public TaskResponse updateTask(String taskId, UpdateTaskRequest request) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new NotFoundException("Task not found with id: " + taskId));

        if (request.getTaskName() != null) {
            if (request.getTaskName().trim().isEmpty()) {
                throw new BadRequestException("Task name cannot be empty");
            }
            task.setTaskName(request.getTaskName());
        }
        if (request.getDescription() != null) {
            task.setDescription(request.getDescription());
        }
        if (request.getStatus() != null) {
            // Validate status transition
            if (task.getStatus() == request.getStatus()) {
                throw new BadRequestException("Task is already in status: " + request.getStatus());
            }

            if (task.getJiraIssueKey() != null) {
                String transitionId = mapStatusToTransition(request.getStatus());
                jiraService.updateIssueStatus(task.getJiraIssueKey(), transitionId);
            }

            task.setStatus(request.getStatus());
        }
        if (request.getPriority() != null) {
            task.setPriority(request.getPriority());
        }
        if (request.getDueDate() != null) {
            task.setDueDate(request.getDueDate());
        }
        if (request.getAssignedToId() != null && !request.getAssignedToId().isEmpty()) {
            Account assignedTo = accountRepository.findById(request.getAssignedToId())
                    .orElseThrow(() -> new NotFoundException("User not found with id: " + request.getAssignedToId()));
            task.setAssignedTo(assignedTo);
        }

        Task saved = taskRepository.save(task);
        return mapToResponse(saved);
    }

    @Override
    @Transactional
    public void deleteTask(String taskId) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new NotFoundException("Task not found with id: " + taskId));

        // Delete from Jira - MUST SUCCEED BEFORE DELETING FROM DB
        if (task.getJiraIssueKey() != null) {
            jiraService.deleteIssue(task.getJiraIssueKey());
        }

        taskRepository.delete(task);
    }

    @Override
    public TaskResponse getTaskById(String taskId) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new NotFoundException("Task not found with id: " + taskId));
        return mapToResponse(task);
    }

    @Override
    public List<TaskResponse> getTasksByProject(String projectId) {
        if (!projectRepository.existsById(projectId)) {
            throw new NotFoundException("Project not found with id: " + projectId);
        }
        return taskRepository.findByProjectId(projectId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<TaskResponse> getTasksByProjectAndStatus(String projectId, TaskStatus status) {
        if (!projectRepository.existsById(projectId)) {
            throw new NotFoundException("Project not found with id: " + projectId);
        }
        if (status == null) {
            throw new BadRequestException("Status is required");
        }
        return taskRepository.findByProjectIdAndStatus(projectId, status).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<TaskResponse> getTasksAssignedToUser(String accountId) {
        if (!accountRepository.existsById(accountId)) {
            throw new NotFoundException("User not found with id: " + accountId);
        }
        return taskRepository.findByAssignedToId(accountId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<TaskResponse> getTasksByProjectAndUser(String projectId, String accountId) {
        if (!projectRepository.existsById(projectId)) {
            throw new NotFoundException("Project not found with id: " + projectId);
        }
        if (!accountRepository.existsById(accountId)) {
            throw new NotFoundException("User not found with id: " + accountId);
        }

        Account account = accountRepository.findById(accountId).get();
        return taskRepository.findByProjectIdAndAssignedTo(projectId, account).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<TaskResponse> getRequirementsByProject(String projectId) {
        if (!projectRepository.existsById(projectId)) {
            throw new NotFoundException("Project not found with id: " + projectId);
        }
        return taskRepository.findByProjectIdAndIsRequirement(projectId, true).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<TaskResponse> getTasksOnlyByProject(String projectId) {
        if (!projectRepository.existsById(projectId)) {
            throw new NotFoundException("Project not found with id: " + projectId);
        }
        return taskRepository.findByProjectIdAndIsRequirement(projectId, false).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public TaskResponse assignTaskToUser(String taskId, String accountId) {

        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new NotFoundException("Task not found with id: " + taskId));

        Account assignedTo = accountRepository.findById(accountId)
                .orElseThrow(() -> new NotFoundException("User not found with id: " + accountId));

        // Get jiraAccountId from mapping table
        JiraUserMapping mapping = jiraUserMappingRepository
                .findByAccountId(accountId)
                .orElseThrow(() -> new BadRequestException("User has not linked Jira account"));

        String jiraAccountId = mapping.getJiraAccountId();

        // Update assignee on Jira first
        if (task.getJiraIssueKey() != null) {
            jiraService.assignIssue(task.getJiraIssueKey(), jiraAccountId);
        }

        // After Jira succeeds, then update DB
        task.setAssignedTo(assignedTo);

        Task saved = taskRepository.save(task);

        return mapToResponse(saved);
    }

    @Override
    @Transactional
    public TaskResponse updateTaskStatus(String taskId, TaskStatus status) {
        if (status == null) {
            throw new BadRequestException("Status is required");
        }

        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new NotFoundException("Task not found with id: " + taskId));

        // Validate status transition
        if (task.getStatus() == status) {
            throw new BadRequestException("Task is already in status: " + status);
        }

        if (task.getJiraIssueKey() != null) {
            String transitionId = mapStatusToTransition(status);
            jiraService.updateIssueStatus(
                    task.getJiraIssueKey(),
                    transitionId
            );
        }

        task.setStatus(status);

        Task saved = taskRepository.save(task);

        return mapToResponse(saved);
    }

    private String mapStatusToTransition(TaskStatus status) {
        return switch (status) {
            case TODO -> "11";
            case IN_PROGRESS -> "21";
            case DONE -> "31";
        };
    }

    private String mapPriorityToJira(com.example.swp391.tasks.enums.TaskPriority priority) {
        if (priority == null) {
            return "Medium";
        }
        return switch (priority) {
            case HIGHEST -> "Highest";
            case HIGH -> "High";
            case LOW -> "Low";
            case LOWEST -> "Lowest";
            case MEDIUM -> "Medium";
        };
    }


    @Override
    public TaskProgressReport getProjectProgressReport(String projectId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new NotFoundException("Project not found with id: " + projectId));

        List<Task> tasks = taskRepository.findByProjectId(projectId);

        long totalTasks = tasks.size();
        long completedTasks = tasks.stream()
                .filter(t -> t.getStatus() == TaskStatus.DONE)
                .count();
        long inProgressTasks = tasks.stream()
                .filter(t -> t.getStatus() == TaskStatus.IN_PROGRESS)
                .count();
        long todoTasks = tasks.stream()
                .filter(t -> t.getStatus() == TaskStatus.TODO)
                .count();

        double completionPercentage = totalTasks > 0 ? (double) completedTasks / totalTasks * 100 : 0;

        Map<String, Long> tasksByStatus = tasks.stream()
                .collect(Collectors.groupingBy(t -> t.getStatus().name(), Collectors.counting()));

        Map<String, Long> tasksByPriority = tasks.stream()
                .collect(Collectors.groupingBy(t -> t.getPriority().name(), Collectors.counting()));

        return TaskProgressReport.builder()
                .projectId(projectId)
                .projectName(project.getProjectName())
                .totalTasks(totalTasks)
                .completedTasks(completedTasks)
                .inProgressTasks(inProgressTasks)
                .todoTasks(todoTasks)
                .blockedTasks(0L)
                .completionPercentage(completionPercentage)
                .tasksByStatus(tasksByStatus)
                .tasksByPriority(tasksByPriority)
                .build();
    }

    @Override
    public UserTaskStats getUserTaskStats(String accountId) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new NotFoundException("User not found with id: " + accountId));

        List<Task> tasks = taskRepository.findByAssignedToId(accountId);

        long totalTasks = tasks.size();
        long completedTasks = tasks.stream()
                .filter(t -> t.getStatus() == TaskStatus.DONE)
                .count();
        long inProgressTasks = tasks.stream()
                .filter(t -> t.getStatus() == TaskStatus.IN_PROGRESS)
                .count();
        long todoTasks = tasks.stream()
                .filter(t -> t.getStatus() == TaskStatus.TODO)
                .count();

        double completionPercentage = totalTasks > 0 ? (double) completedTasks / totalTasks * 100 : 0;

        Map<String, Long> tasksByStatus = tasks.stream()
                .collect(Collectors.groupingBy(t -> t.getStatus().name(), Collectors.counting()));

        return UserTaskStats.builder()
                .userId(accountId)
                .userName(account.getDetails() != null ? 
                        account.getDetails().getFullName() : account.getUsername())
                .totalAssignedTasks(totalTasks)
                .completedTasks(completedTasks)
                .inProgressTasks(inProgressTasks)
                .todoTasks(todoTasks)
                .completionPercentage(completionPercentage)
                .tasksByStatus(tasksByStatus)
                .build();
    }

    private TaskResponse mapToResponse(Task task) {
        return TaskResponse.builder()
                .id(task.getId())
                .projectId(task.getProject().getId())
                .projectName(task.getProject().getProjectName())
                .taskName(task.getTaskName())
                .description(task.getDescription())
                .jiraIssueId(task.getJiraIssueId())
                .jiraIssueKey(task.getJiraIssueKey())
                .status(task.getStatus())
                .priority(task.getPriority())
                .assignedToId(task.getAssignedTo() != null ? task.getAssignedTo().getId() : null)
                .assignedToName(task.getAssignedTo() != null && task.getAssignedTo().getDetails() != null ?
                        task.getAssignedTo().getDetails().getFullName() : null)
                .assignedToUsername(task.getAssignedTo() != null ?
                        task.getAssignedTo().getUsername() : null)
                .createdById(task.getCreatedBy().getId())
                .createdByName(task.getCreatedBy().getDetails() != null ?
                        task.getCreatedBy().getDetails().getFullName() : task.getCreatedBy().getUsername())
                .createdAt(task.getCreatedAt())
                .dueDate(task.getDueDate())
                .isRequirement(task.isRequirement())
                .build();
    }
}

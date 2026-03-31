package com.example.swp391.tasks.service.impl;

import com.example.swp391.accounts.entity.Account;
import com.example.swp391.accounts.repository.AccountRepository;
import com.example.swp391.exceptions.BadRequestException;
import com.example.swp391.exceptions.ForbiddenException;
import com.example.swp391.exceptions.NotFoundException;
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
import com.example.swp391.tasks.enums.TaskPriority;
import com.example.swp391.tasks.enums.TaskStatus;
import com.example.swp391.tasks.repository.TaskRepository;
import com.example.swp391.tasks.service.ITaskService;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
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
    private final IJiraService jiraService;
    private final JavaMailSender mailSender;

    @Value("${app.mail.from}")
    private String fromEmail;

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
                .status(TaskStatus.TODO)
                .priority(request.getPriority())
                .assignedTo(assignedTo)
                .createdBy(createdBy)
                .dueDate(request.getDueDate())
                .isRequirement(request.isRequirement())
                .build();

        Task saved = taskRepository.save(task);
        sendTaskAssignmentEmail(saved);
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
        sendTaskAssignmentEmail(saved);

        return mapToResponse(saved);
    }

    @Override
    @Transactional
    public TaskResponse updateTaskStatus(String taskId, TaskStatus status, String currentUserId) {
        if (status == null) {
            throw new BadRequestException("Status is required");
        }
        if (status == TaskStatus.TODO) {
            throw new BadRequestException("TODO is the default lecturer status and cannot be set manually here.");
        }

        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new NotFoundException("Task not found with id: " + taskId));
        Account currentUser = accountRepository.findById(currentUserId)
                .orElseThrow(() -> new NotFoundException("User not found with id: " + currentUserId));

        String currentRole = currentUser.getRole() != null ? currentUser.getRole().getName() : null;
        if (!"STUDENT".equals(currentRole)) {
            throw new ForbiddenException("Only the assigned student can update task progress.");
        }
        if (task.getAssignedTo() == null || !currentUserId.equals(task.getAssignedTo().getId())) {
            throw new ForbiddenException("You can only update tasks that are assigned to your account.");
        }

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

    private String mapPriorityToJira(TaskPriority priority) {
        if (priority == null) {
            return "Medium";
        }
        return switch (priority) {
            case HIGHEST -> "Highest";
            case HIGH -> "High";
            case LOW -> "Low";
            case LOWEST -> "Lowest";
        };
    }

    private void sendTaskAssignmentEmail(Task task) {
        Account assignee = task.getAssignedTo();
        if (assignee == null || assignee.getEmail() == null || assignee.getEmail().isBlank()) {
            return;
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromEmail);
            helper.setTo(assignee.getEmail());
            helper.setSubject("SWP391 Task Assignment Notification");
            helper.setText(buildTaskAssignmentEmailHtml(task), true);
            mailSender.send(message);
        } catch (MailException | MessagingException ex) {
            // Task creation/assignment should still succeed even if mail delivery fails.
        }
    }

    private String buildTaskAssignmentEmailHtml(Task task) {
        Account assignee = task.getAssignedTo();
        String studentName = assignee != null && assignee.getDetails() != null && assignee.getDetails().getFullName() != null
                ? assignee.getDetails().getFullName()
                : assignee != null ? assignee.getUsername() : "Student";
        String projectName = task.getProject() != null ? safe(task.getProject().getProjectName()) : "your project";
        String dueDate = task.getDueDate() != null ? task.getDueDate().toString() : "Not set";
        String description = task.getDescription() != null && !task.getDescription().isBlank()
                ? task.getDescription()
                : "No additional description was provided.";

        return """
                <!DOCTYPE html>
                <html lang="en">
                  <body style="margin:0;padding:0;background:#f8fafc;font-family:Segoe UI,Arial,sans-serif;color:#0f172a;">
                    <div style="max-width:720px;margin:0 auto;padding:32px 18px;">
                      <div style="border-radius:28px;overflow:hidden;border:1px solid #e2e8f0;background:#ffffff;box-shadow:0 24px 60px rgba(15,23,42,0.08);">
                        <div style="padding:28px 32px;background:#0f172a;color:#ffffff;">
                          <div style="font-size:12px;letter-spacing:0.18em;text-transform:uppercase;opacity:0.9;">SWP391 Task Assignment</div>
                          <div style="margin-top:12px;font-size:28px;font-weight:700;line-height:1.2;">A new task has been assigned to you</div>
                        </div>
                        <div style="padding:28px 32px;">
                          <p style="margin:0 0 16px;font-size:15px;line-height:1.7;">Hello %s,</p>
                          <p style="margin:0 0 18px;font-size:15px;line-height:1.7;">
                            Your lecturer has assigned a new task to you. The task is created with the default status <strong style="color:#b45309;">TODO</strong>.
                            You are responsible for updating it to <strong style="color:#2563eb;">IN_PROGRESS</strong> and <strong style="color:#16a34a;">DONE</strong> as you work.
                          </p>

                          <div style="border-radius:20px;background:#f8fafc;border:1px solid rgba(15,23,42,0.06);padding:20px 22px;">
                            <div style="font-size:12px;letter-spacing:0.16em;text-transform:uppercase;color:#64748b;">Project</div>
                            <div style="margin-top:8px;font-size:22px;font-weight:700;color:#0f172a;">%s</div>

                            <div style="margin-top:18px;font-size:12px;letter-spacing:0.16em;text-transform:uppercase;color:#64748b;">Task Name</div>
                            <div style="margin-top:8px;font-size:18px;font-weight:700;color:#0f172a;">%s</div>

                            <div style="margin-top:18px;font-size:12px;letter-spacing:0.16em;text-transform:uppercase;color:#64748b;">Priority</div>
                            <div style="margin-top:8px;font-size:15px;color:#334155;">%s</div>

                            <div style="margin-top:18px;font-size:12px;letter-spacing:0.16em;text-transform:uppercase;color:#64748b;">Due Date</div>
                            <div style="margin-top:8px;font-size:15px;color:#334155;">%s</div>

                            <div style="margin-top:18px;font-size:12px;letter-spacing:0.16em;text-transform:uppercase;color:#64748b;">Description</div>
                            <div style="margin-top:8px;font-size:15px;line-height:1.7;color:#334155;">%s</div>
                          </div>

                          <div style="margin-top:18px;padding:18px 20px;border-radius:18px;background:#eff6ff;border:1px solid #bfdbfe;color:#1d4ed8;">
                            <div style="font-weight:800;font-size:15px;">Action Required</div>
                            <div style="margin-top:8px;font-size:14px;line-height:1.7;">
                              Please start this task on time and keep the status updated inside the platform.
                            </div>
                          </div>

                          <div style="margin-top:20px;padding-top:18px;border-top:1px solid #e2e8f0;font-size:13px;line-height:1.7;color:#64748b;">
                            This email was generated by the SWP391 platform.
                          </div>
                        </div>
                      </div>
                    </div>
                  </body>
                </html>
                """.formatted(
                escapeHtml(studentName),
                escapeHtml(projectName),
                escapeHtml(safe(task.getTaskName())),
                escapeHtml(task.getPriority() != null ? task.getPriority().name() : "LOW"),
                escapeHtml(dueDate),
                nl2br(escapeHtml(description))
        );
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private String escapeHtml(String value) {
        return safe(value)
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    private String nl2br(String value) {
        return safe(value).replace("\n", "<br/>");
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

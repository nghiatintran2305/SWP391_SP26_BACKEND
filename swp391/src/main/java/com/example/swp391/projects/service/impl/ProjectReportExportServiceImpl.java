package com.example.swp391.projects.service.impl;

import com.example.swp391.accounts.dto.response.AccountResponse;
import com.example.swp391.accounts.entity.Account;
import com.example.swp391.accounts.repository.AccountRepository;
import com.example.swp391.exceptions.BadRequestException;
import com.example.swp391.exceptions.NotFoundException;
import com.example.swp391.github.dto.response.CommitSummary;
import com.example.swp391.github.entity.GithubUserMapping;
import com.example.swp391.github.repository.GithubUserMappingRepository;
import com.example.swp391.github.service.IGithubService;
import com.example.swp391.projects.entity.Project;
import com.example.swp391.projects.entity.ProjectMember;
import com.example.swp391.projects.repository.ProjectMemberRepository;
import com.example.swp391.projects.repository.ProjectRepository;
import com.example.swp391.projects.service.ProjectReportExportService;
import com.example.swp391.tasks.entity.Task;
import com.example.swp391.tasks.enums.TaskStatus;
import com.example.swp391.tasks.repository.TaskRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
public class ProjectReportExportServiceImpl implements ProjectReportExportService {

    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final TaskRepository taskRepository;
    private final AccountRepository accountRepository;
    private final GithubUserMappingRepository githubUserMappingRepository;
    private final IGithubService githubService;

    @Override
    public ExportedProjectReport exportMembersReport(String projectId, String format) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new NotFoundException("Project not found with id: " + projectId));

        String normalizedFormat = (format == null || format.isBlank())
                ? "csv"
                : format.trim().toLowerCase(Locale.ROOT);

        if (!normalizedFormat.equals("csv") && !normalizedFormat.equals("excel")) {
            throw new BadRequestException("Unsupported export format. Use csv or excel.");
        }

        List<MemberReportContext> members = loadProjectAccounts(project);
        List<Task> tasks = taskRepository.findByProjectId(projectId);
        Map<String, CommitSummary> commitSummaryByGithubUsername = loadCommitSummary(project.getGithubRepoName());

        List<ReportRow> rows = members.stream()
                .map(member -> toRow(member, tasks, commitSummaryByGithubUsername))
                .toList();

        ReportSummary summary = buildSummary(project, rows, tasks);

        String separator = normalizedFormat.equals("excel") ? "\t" : ",";
        String contentType = normalizedFormat.equals("excel")
                ? "application/vnd.ms-excel"
                : "text/csv";
        String extension = normalizedFormat.equals("excel") ? "xls" : "csv";
        String filename = buildFilename(project.getProjectName(), extension);
        String reportContent = normalizedFormat.equals("excel")
                ? buildExcelHtmlReport(summary, rows)
                : buildDelimitedReport(summary, rows, separator);
        byte[] content = reportContent.getBytes(StandardCharsets.UTF_8);

        return new ExportedProjectReport(filename, contentType, content);
    }

    private List<MemberReportContext> loadProjectAccounts(Project project) {
        Map<String, MemberReportContext> responses = new LinkedHashMap<>();

        projectMemberRepository.findByProjectId(project.getId()).stream()
                .sorted(Comparator.comparing(member -> member.getAccount().getUsername(), String.CASE_INSENSITIVE_ORDER))
                .forEach(member -> responses.put(
                        member.getAccount().getId(),
                        new MemberReportContext(toAccountResponse(member.getAccount()), member.getRoleInGroup().name())
                ));

        if (project.getLecturerId() != null && !responses.containsKey(project.getLecturerId())) {
            accountRepository.findById(project.getLecturerId())
                    .ifPresent(lecturer -> responses.put(
                            lecturer.getId(),
                            new MemberReportContext(toAccountResponse(lecturer), "LECTURER")
                    ));
        }

        return new ArrayList<>(responses.values());
    }

    private AccountResponse toAccountResponse(Account account) {
        AccountResponse response = new AccountResponse();
        response.setId(account.getId());
        response.setUsername(account.getUsername());
        response.setEmail(account.getEmail());
        response.setFullName(account.getDetails() != null ? account.getDetails().getFullName() : null);
        response.setRole(account.getRole() != null ? account.getRole().getName() : null);
        response.setActive(account.isActive());
        return response;
    }

    private Map<String, CommitSummary> loadCommitSummary(String repoName) {
        Map<String, CommitSummary> result = new HashMap<>();

        if (repoName == null || repoName.isBlank()) {
            return result;
        }

        try {
            for (CommitSummary summary : githubService.getTeamCommitSummary(repoName)) {
                result.put(normalizeKey(summary.getUsername()), summary);
            }
        } catch (Exception ignored) {
            // Export should still work even if GitHub stats are temporarily unavailable.
        }

        return result;
    }

    private ReportRow toRow(
            MemberReportContext memberContext,
            List<Task> tasks,
            Map<String, CommitSummary> commitSummaryByGithubUsername
    ) {
        AccountResponse member = memberContext.account();
        String githubUsername = githubUserMappingRepository.findByAccountId(member.getId())
                .map(GithubUserMapping::getGithubUsername)
                .orElse("");

        long assignedTaskCount = tasks.stream()
                .filter(task -> task.getAssignedTo() != null && member.getId().equals(task.getAssignedTo().getId()))
                .count();

        long completedTaskCount = tasks.stream()
                .filter(task -> task.getAssignedTo() != null && member.getId().equals(task.getAssignedTo().getId()))
                .filter(task -> task.getStatus() == TaskStatus.DONE)
                .count();

        String completedWorkItems = tasks.stream()
                .filter(task -> task.getAssignedTo() != null && member.getId().equals(task.getAssignedTo().getId()))
                .filter(task -> task.getStatus() == TaskStatus.DONE)
                .map(Task::getTaskName)
                .filter(taskName -> taskName != null && !taskName.isBlank())
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .reduce((left, right) -> left + " | " + right)
                .orElse("-");

        CommitSummary summary = commitSummaryByGithubUsername.getOrDefault(normalizeKey(githubUsername), new CommitSummary());

        return new ReportRow(
                member.getFullName(),
                member.getUsername(),
                member.getEmail(),
                member.getRole(),
                memberContext.groupRole(),
                member.isActive() ? "Active" : "Inactive",
                githubUsername,
                assignedTaskCount,
                completedTaskCount,
                assignedTaskCount == 0 ? "0%" : Math.round((completedTaskCount * 100.0f) / assignedTaskCount) + "%",
                completedWorkItems,
                summary.getCommits(),
                summary.getAdditions(),
                summary.getDeletions()
        );
    }

    private ReportSummary buildSummary(Project project, List<ReportRow> rows, List<Task> tasks) {
        long activeMembers = rows.stream().filter(row -> "Active".equalsIgnoreCase(row.status())).count();
        long totalCompletedTasks = tasks.stream().filter(task -> task.getStatus() == TaskStatus.DONE).count();
        int totalCommits = rows.stream().mapToInt(ReportRow::commitCount).sum();

        return new ReportSummary(
                project.getProjectName(),
                project.getStatus() != null ? project.getStatus().name() : "N/A",
                project.getGithubRepoName(),
                project.getJiraProjectKey(),
                rows.size(),
                activeMembers,
                tasks.size(),
                totalCompletedTasks,
                totalCommits,
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
        );
    }

    private String buildDelimitedReport(ReportSummary summary, List<ReportRow> rows, String separator) {
        StringBuilder builder = new StringBuilder();

        builder.append('\uFEFF');
        builder.append(joinCells(separator, "Project Summary", "")).append("\r\n");
        builder.append(joinCells(separator, "Project Name", summary.projectName())).append("\r\n");
        builder.append(joinCells(separator, "Status", summary.projectStatus())).append("\r\n");
        builder.append(joinCells(separator, "GitHub Repository", summary.githubRepoName())).append("\r\n");
        builder.append(joinCells(separator, "Jira Key", summary.jiraProjectKey())).append("\r\n");
        builder.append(joinCells(separator, "Total Members", String.valueOf(summary.totalMembers()))).append("\r\n");
        builder.append(joinCells(separator, "Active Members", String.valueOf(summary.activeMembers()))).append("\r\n");
        builder.append(joinCells(separator, "Total Tasks", String.valueOf(summary.totalTasks()))).append("\r\n");
        builder.append(joinCells(separator, "Completed Tasks", String.valueOf(summary.completedTasks()))).append("\r\n");
        builder.append(joinCells(separator, "Total Commits", String.valueOf(summary.totalCommits()))).append("\r\n");
        builder.append(joinCells(separator, "Generated At", summary.generatedAt())).append("\r\n");
        builder.append("\r\n");
        builder.append(joinCells(separator, "Member Details", "")).append("\r\n");
        builder.append(joinCells(separator,
                "Full Name",
                "Username",
                "Email",
                "Account Role",
                "Group Role",
                "Status",
                "GitHub Username",
                "Assigned Tasks",
                "Completed Tasks",
                "Task Completion",
                "Completed Work Items",
                "Commit Count",
                "Additions",
                "Deletions"
        )).append("\r\n");

        for (ReportRow row : rows) {
            builder.append(joinCells(separator,
                    row.fullName(),
                    row.username(),
                    row.email(),
                    row.accountRole(),
                    row.groupRole(),
                    row.status(),
                    row.githubUsername(),
                    String.valueOf(row.assignedTaskCount()),
                    String.valueOf(row.completedTaskCount()),
                    row.taskCompletionRate(),
                    row.completedWorkItems(),
                    String.valueOf(row.commitCount()),
                    String.valueOf(row.additions()),
                    String.valueOf(row.deletions())
            )).append("\r\n");
        }

        return builder.toString();
    }

    private String buildExcelHtmlReport(ReportSummary summary, List<ReportRow> rows) {
        StringBuilder builder = new StringBuilder();

        builder.append('\uFEFF');
        builder.append("""
                <html>
                <head>
                  <meta charset="UTF-8" />
                  <style>
                    body { font-family: Calibri, Arial, sans-serif; margin: 20px; color: #0f172a; }
                    .sheet-title {
                      font-size: 22px;
                      font-weight: 700;
                      color: #0f172a;
                      margin-bottom: 10px;
                    }
                    .sheet-subtitle {
                      font-size: 12px;
                      color: #475569;
                      margin-bottom: 16px;
                    }
                    table {
                      border-collapse: collapse;
                      width: 100%;
                      margin-bottom: 18px;
                    }
                    th, td {
                      border: 1px solid #cbd5e1;
                      padding: 8px 10px;
                      vertical-align: top;
                      font-size: 12px;
                    }
                    .section {
                      background: #0f172a;
                      color: #ffffff;
                      font-weight: 700;
                      text-transform: uppercase;
                      letter-spacing: 0.08em;
                    }
                    .summary-key {
                      width: 220px;
                      background: #e2e8f0;
                      font-weight: 700;
                    }
                    .summary-value {
                      background: #f8fafc;
                    }
                    .table-head th {
                      background: #dbeafe;
                      color: #0f172a;
                      font-weight: 700;
                      text-align: left;
                    }
                    .muted {
                      color: #64748b;
                    }
                    .good {
                      color: #166534;
                      font-weight: 700;
                    }
                    .warning {
                      color: #9a3412;
                      font-weight: 700;
                    }
                  </style>
                </head>
                <body>
                """);

        builder.append("<div class=\"sheet-title\">Project Member Performance Report</div>");
        builder.append("<div class=\"sheet-subtitle\">Lecturer report for admin review. Includes commit activity, completed tasks, and delivered work items.</div>");

        builder.append("<table>");
        builder.append("<tr><td class=\"section\" colspan=\"2\">Project Summary</td></tr>");
        appendSummaryRow(builder, "Project Name", summary.projectName());
        appendSummaryRow(builder, "Status", summary.projectStatus());
        appendSummaryRow(builder, "GitHub Repository", summary.githubRepoName());
        appendSummaryRow(builder, "Jira Key", summary.jiraProjectKey());
        appendSummaryRow(builder, "Total Members", String.valueOf(summary.totalMembers()));
        appendSummaryRow(builder, "Active Members", String.valueOf(summary.activeMembers()));
        appendSummaryRow(builder, "Total Tasks", String.valueOf(summary.totalTasks()));
        appendSummaryRow(builder, "Completed Tasks", String.valueOf(summary.completedTasks()));
        appendSummaryRow(builder, "Total Commits", String.valueOf(summary.totalCommits()));
        appendSummaryRow(builder, "Generated At", summary.generatedAt());
        builder.append("</table>");

        builder.append("<table>");
        builder.append("<tr><td class=\"section\" colspan=\"13\">Member Details</td></tr>");
        builder.append("""
                <tr class="table-head">
                  <th>Full Name</th>
                  <th>Username</th>
                  <th>Email</th>
                  <th>Account Role</th>
                  <th>Group Role</th>
                  <th>Status</th>
                  <th>GitHub Username</th>
                  <th>Assigned Tasks</th>
                  <th>Completed Tasks</th>
                  <th>Task Completion</th>
                  <th>Completed Work Items</th>
                  <th>Commit Count</th>
                  <th>Additions / Deletions</th>
                </tr>
                """);

        for (ReportRow row : rows) {
            String completionClass = row.completedTaskCount() > 0 || row.commitCount() > 0 ? "good" : "warning";
            builder.append("<tr>");
            appendCell(builder, row.fullName());
            appendCell(builder, row.username());
            appendCell(builder, row.email());
            appendCell(builder, row.accountRole());
            appendCell(builder, row.groupRole());
            appendCell(builder, row.status());
            appendCell(builder, row.githubUsername().isBlank() ? "Not linked" : row.githubUsername(), row.githubUsername().isBlank() ? "muted" : "");
            appendCell(builder, String.valueOf(row.assignedTaskCount()));
            appendCell(builder, String.valueOf(row.completedTaskCount()));
            appendCell(builder, row.taskCompletionRate(), completionClass);
            appendCell(builder, row.completedWorkItems());
            appendCell(builder, String.valueOf(row.commitCount()), completionClass);
            appendCell(builder, row.additions() + " / " + row.deletions());
            builder.append("</tr>");
        }

        builder.append("</table>");
        builder.append("</body></html>");
        return builder.toString();
    }

    private void appendSummaryRow(StringBuilder builder, String label, String value) {
        builder.append("<tr>");
        builder.append("<td class=\"summary-key\">").append(escapeHtml(label)).append("</td>");
        builder.append("<td class=\"summary-value\">").append(escapeHtml(value)).append("</td>");
        builder.append("</tr>");
    }

    private void appendCell(StringBuilder builder, String value) {
        appendCell(builder, value, "");
    }

    private void appendCell(StringBuilder builder, String value, String cssClass) {
        builder.append("<td");
        if (cssClass != null && !cssClass.isBlank()) {
            builder.append(" class=\"").append(cssClass).append("\"");
        }
        builder.append(">").append(escapeHtml(value)).append("</td>");
    }

    private String joinCells(String separator, String... values) {
        List<String> safeValues = new ArrayList<>();
        for (String value : values) {
            safeValues.add(value == null ? "" : value);
        }

        return safeValues.stream()
                .map(value -> escapeCell(value, separator))
                .reduce((left, right) -> left + separator + right)
                .orElse("");
    }

    private String escapeCell(String value, String separator) {
        String escaped = value.replace("\"", "\"\"");
        boolean needsQuotes = escaped.contains(separator) || escaped.contains("\n") || escaped.contains("\r");
        return needsQuotes ? "\"" + escaped + "\"" : escaped;
    }

    private String escapeHtml(String value) {
        if (value == null || value.isBlank()) return "-";
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("\r\n", "<br/>")
                .replace("\n", "<br/>");
    }

    private String buildFilename(String projectName, String extension) {
        String safeProjectName = projectName == null ? "project" : normalizeKey(projectName).replace('-', '_');
        if (safeProjectName.isBlank()) {
            safeProjectName = "project";
        }
        return safeProjectName + "_members_report." + extension;
    }

    private String normalizeKey(String value) {
        if (value == null) return "";

        String normalized = Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .trim();

        return normalized.replaceAll("[^a-z0-9]+", "-").replaceAll("(^-|-$)", "");
    }

    private record ReportRow(
            String fullName,
            String username,
            String email,
            String accountRole,
            String groupRole,
            String status,
            String githubUsername,
            long assignedTaskCount,
            long completedTaskCount,
            String taskCompletionRate,
            String completedWorkItems,
            int commitCount,
            int additions,
            int deletions
    ) {}

    private record MemberReportContext(
            AccountResponse account,
            String groupRole
    ) {}

    private record ReportSummary(
            String projectName,
            String projectStatus,
            String githubRepoName,
            String jiraProjectKey,
            int totalMembers,
            long activeMembers,
            int totalTasks,
            long completedTasks,
            int totalCommits,
            String generatedAt
    ) {}
}

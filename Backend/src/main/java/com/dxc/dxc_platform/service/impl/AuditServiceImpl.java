package com.dxc.dxc_platform.service.impl;

import com.dxc.dxc_platform.dto.AuditLogDto;
import com.dxc.dxc_platform.entity.AuditLog;
import com.dxc.dxc_platform.entity.Project;
import com.dxc.dxc_platform.repository.AuditLogRepository;
import com.dxc.dxc_platform.repository.ProjectRepository;
import com.dxc.dxc_platform.repository.TaskRepository;
import com.dxc.dxc_platform.repository.UserRepository;
import com.dxc.dxc_platform.service.AuditService;
import com.dxc.dxc_platform.shared.exception.NotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Transactional
public class AuditServiceImpl implements AuditService {

    private static final Set<String> WORKFLOW_ACTIONS = Set.of(
            "CREATE_PROJECT", "UPDATE_PROJECT", "VALIDATE_PROJECT", "REJECT_PROJECT", "ASSIGN_CHEF",
            "CREATE_TEAM", "UPDATE_TEAM", "DELETE_TEAM", "ASSIGN_MEMBER_TO_TEAM",
            "REMOVE_MEMBER_FROM_TEAM", "ASSIGN_TEAM_TO_PROJECT",
            "CREATE_TASK", "UPDATE_TASK", "DELETE_TASK", "SUBMIT_TASK",
            "VALIDATE_TASK", "REJECT_TASK", "UPDATE_TASK_STATUS"
    );

    private static final Set<String> USER_MANAGEMENT_ACTIONS = Set.of(
            "CREATE_USER", "UPDATE_USER", "DELETE_USER",
            "ENABLE_USER", "DISABLE_USER", "RESET_PASSWORD",
            "ACCOUNT_LOCKED", "ACCOUNT_UNLOCKED",
            "CREATE_ROLE", "UPDATE_ROLE", "DELETE_ROLE"
    );

    // ✅ Actions d'authentification
    private static final Set<String> AUTH_ACTIONS = Set.of(
            "LOGIN_SUCCESS", "LOGIN_FAILED", "LOGOUT"
    );

    private final AuditLogRepository auditLogRepository;
    private final UserRepository userRepository;
    private final ProjectRepository projectRepository;
    private final TaskRepository taskRepository;

    public AuditServiceImpl(AuditLogRepository auditLogRepository,
                            UserRepository userRepository,
                            ProjectRepository projectRepository,
                            TaskRepository taskRepository) {
        this.auditLogRepository = auditLogRepository;
        this.userRepository = userRepository;
        this.projectRepository = projectRepository;
        this.taskRepository = taskRepository;
    }

    @Override
    public void log(String action, String entityType, Long entityId, String details,
                    String performedBy, String ipAddress) {
        AuditLog log = new AuditLog();
        log.setAction(action);
        log.setEntityType(entityType);
        log.setEntityId(entityId);
        log.setDetails(details);
        log.setPerformedBy(performedBy);
        log.setPerformedAt(LocalDateTime.now());
        log.setIpAddress(ipAddress);
        auditLogRepository.save(log);
    }

    @Override
    public Page<AuditLogDto> getLogs(String action, String performedBy,
                                     LocalDateTime startDate, LocalDateTime endDate, Pageable pageable) {
        Page<AuditLog> logs = auditLogRepository.findAllOrderByPerformedAtDesc(pageable);
        return logs.map(this::toDto);
    }

    public Page<AuditLogDto> getWorkflowLogs(String action, String performedBy,
                                             LocalDateTime startDate, LocalDateTime endDate, Pageable pageable) {
        Page<AuditLog> allLogs = auditLogRepository.findAllOrderByPerformedAtDesc(pageable);
        List<AuditLogDto> filtered;

        if (performedBy != null && !performedBy.isBlank()) {
            filtered = getManagerWorkflowLogs(allLogs.getContent(), performedBy, action, startDate, endDate);
        } else {
            filtered = allLogs.getContent().stream()
                    .filter(log -> WORKFLOW_ACTIONS.contains(log.getAction()))
                    .filter(log -> matchesAction(log, action))
                    .filter(log -> matchesDateRange(log, startDate, endDate))
                    .map(this::toDto)
                    .collect(Collectors.toList());
        }

        return new PageImpl<>(filtered, pageable, filtered.size());
    }

    public Page<AuditLogDto> getUserManagementLogs(String action, String performedBy,
                                                   LocalDateTime startDate, LocalDateTime endDate, Pageable pageable) {
        Page<AuditLog> allLogs = auditLogRepository.findAllOrderByPerformedAtDesc(pageable);

        var filtered = allLogs.getContent().stream()
                .filter(log -> USER_MANAGEMENT_ACTIONS.contains(log.getAction()) || AUTH_ACTIONS.contains(log.getAction()))
                .collect(Collectors.toList());

        return new PageImpl<>(filtered.stream().map(this::toDto).collect(Collectors.toList()), pageable, filtered.size());
    }

    // ✅ NOUVELLE MÉTHODE : pour les statistiques de l'admin
    public AuditStatsDto getAuditStats() {
        LocalDateTime todayStart = LocalDateTime.of(LocalDate.now(), LocalTime.MIN);
        LocalDateTime todayEnd = LocalDateTime.of(LocalDate.now(), LocalTime.MAX);
        LocalDateTime yesterdayStart = todayStart.minusDays(1);
        LocalDateTime yesterdayEnd = todayEnd.minusDays(1);

        // Connexions aujourd'hui
        long todayLogins = auditLogRepository.countByActionAndPerformedAtBetween("LOGIN_SUCCESS", todayStart, todayEnd);
        long yesterdayLogins = auditLogRepository.countByActionAndPerformedAtBetween("LOGIN_SUCCESS", yesterdayStart, yesterdayEnd);

        // Connexions échouées
        long failedLogins = auditLogRepository.countByActionAndPerformedAtBetween("LOGIN_FAILED", todayStart, todayEnd);

        // Comptes verrouillés
        long lockedAccounts = auditLogRepository.countByActionAndPerformedAtBetween("ACCOUNT_LOCKED", todayStart.minusDays(30), todayEnd);

        // Réinitialisations MDP ce mois
        long passwordResets = auditLogRepository.countByActionAndPerformedAtBetween("RESET_PASSWORD", todayStart.withDayOfMonth(1), todayEnd);

        // Actions admin
        long adminActions = auditLogRepository.countByActionInAndPerformedAtBetween(
                USER_MANAGEMENT_ACTIONS, todayStart.withDayOfMonth(1), todayEnd);

        // Calcul du trend
        double trend = yesterdayLogins > 0 ? ((todayLogins - yesterdayLogins) * 100.0 / yesterdayLogins) : 0;

        AuditStatsDto stats = new AuditStatsDto();
        stats.setLoginsToday((int) todayLogins);
        stats.setLoginsTrend((int) Math.round(trend));
        stats.setFailedLogins((int) failedLogins);
        stats.setLockedAccounts((int) lockedAccounts);
        stats.setPasswordResets((int) passwordResets);
        stats.setAdminActions((int) adminActions);

        return stats;
    }

    private AuditLogDto toDto(AuditLog log) {
        return new AuditLogDto(
                log.getId(),
                log.getAction(),
                log.getEntityType(),
                log.getEntityId(),
                log.getDetails(),
                log.getPerformedBy(),
                log.getPerformedAt(),
                log.getIpAddress()
        );
    }

    private boolean matchesAction(AuditLog log, String action) {
        return action == null || action.isBlank() || action.equalsIgnoreCase(log.getAction());
    }

    private boolean matchesPerformedBy(AuditLog log, String performedBy) {
        return performedBy == null || performedBy.isBlank()
                || (log.getPerformedBy() != null && performedBy.equalsIgnoreCase(log.getPerformedBy()));
    }

    private boolean matchesDateRange(AuditLog log, LocalDateTime startDate, LocalDateTime endDate) {
        LocalDateTime performedAt = log.getPerformedAt();
        if (performedAt == null) {
            return false;
        }
        if (startDate != null && performedAt.isBefore(startDate)) {
            return false;
        }
        if (endDate != null && performedAt.isAfter(endDate)) {
            return false;
        }
        return true;
    }

    private List<AuditLogDto> getManagerWorkflowLogs(List<AuditLog> logs,
                                                     String managerEmail,
                                                     String action,
                                                     LocalDateTime startDate,
                                                     LocalDateTime endDate) {
        Long managerId = userRepository.findByEmailAndDeletedFalse(managerEmail)
                .orElseThrow(() -> new NotFoundException("USER_NOT_FOUND", "Manager introuvable"))
                .getId();

        List<Project> managerProjects = projectRepository.findAllByDeletedFalseAndManagerId(managerId);
        if (managerProjects.isEmpty()) {
            return List.of();
        }

        Set<Long> projectIds = managerProjects.stream()
                .map(Project::getId)
                .collect(Collectors.toSet());

        Set<Long> teamIds = managerProjects.stream()
                .map(Project::getTeam)
                .filter(team -> team != null)
                .map(team -> team.getId())
                .collect(Collectors.toSet());

        Set<Long> taskIds = new HashSet<>(
                taskRepository.findAllByProjectIdInAndDeletedFalse(List.copyOf(projectIds)).stream()
                        .map(task -> task.getId())
                        .collect(Collectors.toSet())
        );

        return logs.stream()
                .filter(log -> WORKFLOW_ACTIONS.contains(log.getAction()))
                .filter(log -> matchesAction(log, action))
                .filter(log -> matchesDateRange(log, startDate, endDate))
                .filter(log -> belongsToManagerWorkflow(log, projectIds, teamIds, taskIds))
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    private boolean belongsToManagerWorkflow(AuditLog log,
                                             Set<Long> projectIds,
                                             Set<Long> teamIds,
                                             Set<Long> taskIds) {
        if (log.getEntityId() == null || log.getEntityType() == null) {
            return false;
        }

        return switch (log.getEntityType()) {
            case "PROJECT" -> projectIds.contains(log.getEntityId());
            case "TEAM" -> teamIds.contains(log.getEntityId());
            case "TASK" -> taskIds.contains(log.getEntityId());
            default -> false;
        };
    }

    // Classe DTO pour les stats
    public static class AuditStatsDto {
        private int loginsToday;
        private int loginsTrend;
        private int failedLogins;
        private int lockedAccounts;
        private int passwordResets;
        private int adminActions;

        // Getters et setters
        public int getLoginsToday() { return loginsToday; }
        public void setLoginsToday(int loginsToday) { this.loginsToday = loginsToday; }
        public int getLoginsTrend() { return loginsTrend; }
        public void setLoginsTrend(int loginsTrend) { this.loginsTrend = loginsTrend; }
        public int getFailedLogins() { return failedLogins; }
        public void setFailedLogins(int failedLogins) { this.failedLogins = failedLogins; }
        public int getLockedAccounts() { return lockedAccounts; }
        public void setLockedAccounts(int lockedAccounts) { this.lockedAccounts = lockedAccounts; }
        public int getPasswordResets() { return passwordResets; }
        public void setPasswordResets(int passwordResets) { this.passwordResets = passwordResets; }
        public int getAdminActions() { return adminActions; }
        public void setAdminActions(int adminActions) { this.adminActions = adminActions; }
    }
}

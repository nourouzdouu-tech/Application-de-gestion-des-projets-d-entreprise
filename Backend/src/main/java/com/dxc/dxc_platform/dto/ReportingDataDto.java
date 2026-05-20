package com.dxc.dxc_platform.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public class ReportingDataDto {

    // ── Métadonnées ──────────────────────────────────────────────
    private LocalDateTime generatedAt;
    private String role;
    private String userName;

    // ── ADMIN ────────────────────────────────────────────────────
    private List<Map<String, Object>> clients;
    private List<Map<String, Object>> recentUsers;
    private List<Map<String, Object>> failedLogins;
    private List<Map<String, Object>> blockedUsers;

    // ── RESPONSABLE CONTRAT ──────────────────────────────────────
    private List<Map<String, Object>> allProjects;
    private List<Map<String, Object>> projectsByStatus;
    private List<Map<String, Object>> projectsOverdue;
    private List<Map<String, Object>> billedProjects;

    // ── MANAGER ──────────────────────────────────────────────────
    // Réutilise allProjects et projectsByStatus

    // ── CHEF DE PROJET ───────────────────────────────────────────
    private List<Map<String, Object>> myAllProjects;
    private List<Map<String, Object>> myProjectsByStatus;
    private List<Map<String, Object>> myOverdueProjects;
    private List<Map<String, Object>> criticalTasks;
    private Map<String, List<Map<String, Object>>> criticalTasksByProject;

    // ── MEMBRE ÉQUIPE ────────────────────────────────────────────
    private List<Map<String, Object>> myProjectsByYear;

    // ── Compteurs personnels ────────────────────────────────────
    private Integer totalMyTasks;
    private Integer myCompletedTasks;
    private Integer myInProgressTasks;
    private Integer validationsAccepted;
    private Integer validationsRejected;

    // ── Champs additionnels pour rapports ─────────────────────────
    private List<Map<String, Object>> recentActivities;
    private List<Map<String, Object>> rejectedProjects;
    private List<Map<String, Object>> pendingValidationProjects;
    private List<Map<String, Object>> teamPerformance;
    private List<Map<String, Object>> userEfficiency;

    // ── Getters / Setters ─────────────────────────────────────────

    public LocalDateTime getGeneratedAt() { return generatedAt; }
    public void setGeneratedAt(LocalDateTime v) { this.generatedAt = v; }

    public String getRole() { return role; }
    public void setRole(String v) { this.role = v; }

    public String getUserName() { return userName; }
    public void setUserName(String v) { this.userName = v; }

    public List<Map<String, Object>> getClients() { return clients; }
    public void setClients(List<Map<String, Object>> v) { this.clients = v; }

    public List<Map<String, Object>> getRecentUsers() { return recentUsers; }
    public void setRecentUsers(List<Map<String, Object>> v) { this.recentUsers = v; }

    public List<Map<String, Object>> getFailedLogins() { return failedLogins; }
    public void setFailedLogins(List<Map<String, Object>> v) { this.failedLogins = v; }

    public List<Map<String, Object>> getBlockedUsers() { return blockedUsers; }
    public void setBlockedUsers(List<Map<String, Object>> v) { this.blockedUsers = v; }

    public List<Map<String, Object>> getAllProjects() { return allProjects; }
    public void setAllProjects(List<Map<String, Object>> v) { this.allProjects = v; }

    public List<Map<String, Object>> getProjectsByStatus() { return projectsByStatus; }
    public void setProjectsByStatus(List<Map<String, Object>> v) { this.projectsByStatus = v; }

    public List<Map<String, Object>> getProjectsOverdue() { return projectsOverdue; }
    public void setProjectsOverdue(List<Map<String, Object>> v) { this.projectsOverdue = v; }

    public List<Map<String, Object>> getBilledProjects() { return billedProjects; }
    public void setBilledProjects(List<Map<String, Object>> v) { this.billedProjects = v; }

    public List<Map<String, Object>> getMyAllProjects() { return myAllProjects; }
    public void setMyAllProjects(List<Map<String, Object>> v) { this.myAllProjects = v; }

    public List<Map<String, Object>> getMyProjectsByStatus() { return myProjectsByStatus; }
    public void setMyProjectsByStatus(List<Map<String, Object>> v) { this.myProjectsByStatus = v; }

    public List<Map<String, Object>> getMyOverdueProjects() { return myOverdueProjects; }
    public void setMyOverdueProjects(List<Map<String, Object>> v) { this.myOverdueProjects = v; }

    public List<Map<String, Object>> getCriticalTasks() { return criticalTasks; }
    public void setCriticalTasks(List<Map<String, Object>> v) { this.criticalTasks = v; }

    public Map<String, List<Map<String, Object>>> getCriticalTasksByProject() { return criticalTasksByProject; }
    public void setCriticalTasksByProject(Map<String, List<Map<String, Object>>> v) { this.criticalTasksByProject = v; }

    public List<Map<String, Object>> getMyProjectsByYear() { return myProjectsByYear; }
    public void setMyProjectsByYear(List<Map<String, Object>> v) { this.myProjectsByYear = v; }

    public Integer getTotalMyTasks() { return totalMyTasks; }
    public void setTotalMyTasks(Integer v) { this.totalMyTasks = v; }

    public Integer getMyCompletedTasks() { return myCompletedTasks; }
    public void setMyCompletedTasks(Integer v) { this.myCompletedTasks = v; }

    public Integer getMyInProgressTasks() { return myInProgressTasks; }
    public void setMyInProgressTasks(Integer v) { this.myInProgressTasks = v; }

    public Integer getValidationsAccepted() { return validationsAccepted; }
    public void setValidationsAccepted(Integer v) { this.validationsAccepted = v; }

    public Integer getValidationsRejected() { return validationsRejected; }
    public void setValidationsRejected(Integer v) { this.validationsRejected = v; }

    public List<Map<String, Object>> getRecentActivities() { return recentActivities; }
    public void setRecentActivities(List<Map<String, Object>> recentActivities) { this.recentActivities = recentActivities; }

    public List<Map<String, Object>> getRejectedProjects() { return rejectedProjects; }
    public void setRejectedProjects(List<Map<String, Object>> rejectedProjects) { this.rejectedProjects = rejectedProjects; }

    public List<Map<String, Object>> getPendingValidationProjects() { return pendingValidationProjects; }
    public void setPendingValidationProjects(List<Map<String, Object>> pendingValidationProjects) { this.pendingValidationProjects = pendingValidationProjects; }

    public List<Map<String, Object>> getTeamPerformance() { return teamPerformance; }
    public void setTeamPerformance(List<Map<String, Object>> teamPerformance) { this.teamPerformance = teamPerformance; }

    public List<Map<String, Object>> getUserEfficiency() { return userEfficiency; }
    public void setUserEfficiency(List<Map<String, Object>> userEfficiency) { this.userEfficiency = userEfficiency; }
}

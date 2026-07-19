package com.dxc.dxc_platform.controller;
import com.dxc.dxc_platform.dto.reporting.TaskReportDto;
import com.dxc.dxc_platform.dto.ProjectReportDto;
import com.dxc.dxc_platform.dto.UserStatusReportDto;
import com.dxc.dxc_platform.service.ReportingService;
import com.dxc.dxc_platform.shared.util.ExcelGenerator;
import com.dxc.dxc_platform.shared.util.PdfGenerator;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@RestController
@RequestMapping("/api/reporting")
@PreAuthorize("isAuthenticated()")
public class ReportingController {

    private final ReportingService reportingService;

    public ReportingController(ReportingService reportingService) {
        this.reportingService = reportingService;
    }

    /**
     * Liste globale des projets filtrée par année, statut, équipe
     */
    @GetMapping("/projects")
    public ResponseEntity<List<ProjectReportDto>> getProjectsReport(
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long teamId) {
        return ResponseEntity.ok(reportingService.getProjectsReport(year, status, teamId));
    }

    /**
     * Projets qui dépassent la date estimée de fin
     */
    @GetMapping("/projects/overdue")
    public ResponseEntity<List<ProjectReportDto>> getOverdueProjects() {
        return ResponseEntity.ok(reportingService.getOverdueProjects());
    }

    /**
     * Tâches en retard filtrées par projet
     */
    @GetMapping("/tasks/overdue")
    public ResponseEntity<List<TaskReportDto>> getOverdueTasks(
            @RequestParam(required = false) Long projectId) {
        return ResponseEntity.ok(reportingService.getOverdueTasks(projectId));
    }

    /**
     * Utilisateurs sans profil défini
     */
    @GetMapping("/users/no-profile")
    public ResponseEntity<List<com.dxc.dxc_platform.dto.reporting.UserReportDto>> getUsersWithoutProfile() {
        return ResponseEntity.ok(reportingService.getUsersWithoutProfile());
    }

    /**
     * Utilisateurs filtrés par statut (actifs/inactifs) + top réinitialisateurs de mot de passe
     */
    @GetMapping("/users/by-status")
    public ResponseEntity<UserStatusReportDto> getUsersByStatus(
            @RequestParam(required = false) Boolean active) {
        return ResponseEntity.ok(reportingService.getUsersByStatus(active));
    }

    /**
     * Tous les projets (liste simple pour les selects)
     */
    @GetMapping("/projects/select")
    public ResponseEntity<List<com.dxc.dxc_platform.dto.reporting.ProjectSelectDto>> getProjectsForSelect() {
        return ResponseEntity.ok(reportingService.getProjectsForSelect());
    }

    /**
     * Toutes les équipes (liste simple pour les selects)
     */
    @GetMapping("/teams/select")
    public ResponseEntity<List<com.dxc.dxc_platform.dto.reporting.TeamSelectDto>> getTeamsForSelect() {
        return ResponseEntity.ok(reportingService.getTeamsForSelect());
    }

    // ═════════════════════════════════════════════════════════
    // EXPORT EXCEL / PDF
    // ═════════════════════════════════════════════════════════

    /**
     * Export en Excel pour projets globaux
     */
    @GetMapping("/projects/export")
    public ResponseEntity<byte[]> exportProjectsExcel(
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long teamId) {
        List<ProjectReportDto> data = reportingService.getProjectsReport(year, status, teamId);
        byte[] excel = ExcelGenerator.generateProjectsReport(data);
        String filename = "projects_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".xlsx";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(excel);
    }

    /**
     * Export en PDF pour projets globaux
     */
    @GetMapping("/projects/export/pdf")
    public ResponseEntity<byte[]> exportProjectsPdf(
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long teamId) {
        List<ProjectReportDto> data = reportingService.getProjectsReport(year, status, teamId);
        byte[] pdf = PdfGenerator.generateProjectsReport(data);
        String filename = "projects_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".pdf";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    /**
     * Export en Excel pour projets en retard
     */
    @GetMapping("/projects/overdue/export")
    public ResponseEntity<byte[]> exportOverdueProjectsExcel() {
        List<ProjectReportDto> data = reportingService.getOverdueProjects();
        byte[] excel = ExcelGenerator.generateProjectsReport(data);
        String filename = "overdue_projects_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".xlsx";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(excel);
    }

    /**
     * Export en PDF pour projets en retard
     */
    @GetMapping("/projects/overdue/export/pdf")
    public ResponseEntity<byte[]> exportOverdueProjectsPdf() {
        List<ProjectReportDto> data = reportingService.getOverdueProjects();
        byte[] pdf = PdfGenerator.generateProjectsReport(data);
        String filename = "overdue_projects_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".pdf";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    /**
     * Export en Excel pour tâches en retard
     */
    @GetMapping("/tasks/overdue/export")
    public ResponseEntity<byte[]> exportOverdueTasksExcel(@RequestParam(required = false) Long projectId) {
        List<TaskReportDto> data = reportingService.getOverdueTasks(projectId);
        byte[] excel = ExcelGenerator.generateTasksReport(data);
        String filename = "overdue_tasks_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".xlsx";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(excel);
    }

    /**
     * Export en PDF pour tâches en retard
     */
    @GetMapping("/tasks/overdue/export/pdf")
    public ResponseEntity<byte[]> exportOverdueTasksPdf(@RequestParam(required = false) Long projectId) {
        List<TaskReportDto> data = reportingService.getOverdueTasks(projectId);
        byte[] pdf = PdfGenerator.generateTasksReport(data);
        String filename = "overdue_tasks_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".pdf";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    /**
     * Export en Excel pour utilisateurs sans profil
     */
    @GetMapping("/users/no-profile/export")
    public ResponseEntity<byte[]> exportUsersNoProfileExcel() {
        List<com.dxc.dxc_platform.dto.reporting.UserReportDto> data = reportingService.getUsersWithoutProfile();
        byte[] excel = ExcelGenerator.generateUsersReport(data);
        String filename = "users_no_profile_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".xlsx";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(excel);
    }

    /**
     * Export en PDF pour utilisateurs sans profil
     */
    @GetMapping("/users/no-profile/export/pdf")
    public ResponseEntity<byte[]> exportUsersNoProfilePdf() {
        List<com.dxc.dxc_platform.dto.reporting.UserReportDto> data = reportingService.getUsersWithoutProfile();
        byte[] pdf = PdfGenerator.generateUsersReport(data);
        String filename = "users_no_profile_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".pdf";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    /**
     * Export en Excel pour utilisateurs par statut
     */
    @GetMapping("/users/by-status/export")
    public ResponseEntity<byte[]> exportUsersByStatusExcel(@RequestParam(required = false) Boolean active) {
        UserStatusReportDto data = reportingService.getUsersByStatus(active);
        byte[] excel = ExcelGenerator.generateUserStatusReport(data);
        String filename = "users_by_status_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".xlsx";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(excel);
    }

    /**
     * Export en PDF pour utilisateurs par statut
     */
    @GetMapping("/users/by-status/export/pdf")
    public ResponseEntity<byte[]> exportUsersByStatusPdf(@RequestParam(required = false) Boolean active) {
        UserStatusReportDto data = reportingService.getUsersByStatus(active);
        byte[] pdf = PdfGenerator.generateUserStatusReport(data);
        String filename = "users_by_status_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".pdf";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }
}
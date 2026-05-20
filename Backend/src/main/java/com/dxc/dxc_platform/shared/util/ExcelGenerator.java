package com.dxc.dxc_platform.shared.util;

import com.dxc.dxc_platform.dto.ProjectReportDto;
import com.dxc.dxc_platform.dto.UserStatusReportDto;
import com.dxc.dxc_platform.dto.reporting.TaskReportDto;
import com.dxc.dxc_platform.dto.reporting.UserReportDto;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.xssf.usermodel.extensions.XSSFCellBorder;

import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Génère des fichiers Excel (.xlsx) pour les rapports.
 * Dépendance Maven à ajouter dans pom.xml :
 *   <dependency>
 *     <groupId>org.apache.poi</groupId>
 *     <artifactId>poi-ooxml</artifactId>
 *     <version>5.2.5</version>
 *   </dependency>
 */
public class ExcelGenerator {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    // ─────────────────────────────────────────────────────────────────
    // PROJETS (global + overdue)
    // ─────────────────────────────────────────────────────────────────
    public static byte[] generateProjectsReport(List<ProjectReportDto> data) {
        try (Workbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("Projets");

            // ── styles ──────────────────────────────────────────────
            CellStyle headerStyle = buildHeaderStyle(wb);
            CellStyle overdueStyle = buildOverdueStyle(wb);
            CellStyle dateStyle = buildDateStyle(wb);
            CellStyle normalStyle = buildNormalStyle(wb);

            // ── titre ────────────────────────────────────────────────
            Row title = sheet.createRow(0);
            Cell titleCell = title.createCell(0);
            titleCell.setCellValue("Rapport - Liste des projets");
            CellStyle titleStyle = wb.createCellStyle();
            Font titleFont = wb.createFont();
            titleFont.setBold(true);
            titleFont.setFontHeightInPoints((short) 14);
            titleStyle.setFont(titleFont);
            titleCell.setCellStyle(titleStyle);
            sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 7));

            // ── en-têtes ─────────────────────────────────────────────
            String[] headers = {"Projet", "Statut", "Équipe", "Chef de projet",
                    "Date début", "Fin estimée", "Fin réelle", "Retard (j)"};
            Row headerRow = sheet.createRow(2);
            for (int i = 0; i < headers.length; i++) {
                Cell c = headerRow.createCell(i);
                c.setCellValue(headers[i]);
                c.setCellStyle(headerStyle);
            }

            // ── données ──────────────────────────────────────────────
            int rowIdx = 3;
            for (ProjectReportDto p : data) {
                Row row = sheet.createRow(rowIdx++);
                CellStyle rowStyle = p.isOverdue() ? overdueStyle : normalStyle;

                setCell(row, 0, p.getNom(), rowStyle);
                setCell(row, 1, statusLabel(p.getStatus() != null ? p.getStatus().name() : ""), rowStyle);
                setCell(row, 2, p.getTeamNom() != null ? p.getTeamNom() : "—", rowStyle);
                setCell(row, 3, fullName(p.getChefProjetPrenom(), p.getChefProjetNom()), rowStyle);
                setCell(row, 4, p.getDateDebut() != null ? p.getDateDebut().format(DATE_FMT) : "—", dateStyle);
                setCell(row, 5, p.getDateFinEstimee() != null ? p.getDateFinEstimee().format(DATE_FMT) : "—", dateStyle);
                setCell(row, 6, p.getDateFinReelle() != null ? p.getDateFinReelle().format(DATE_FMT) : "—", dateStyle);
                setCell(row, 7, p.isOverdue() ? String.valueOf(p.getJoursRetard()) : "—", rowStyle);
            }

            // ── auto-size ────────────────────────────────────────────
            for (int i = 0; i < headers.length; i++) sheet.autoSizeColumn(i);

            return toBytes(wb);
        } catch (Exception e) {
            throw new RuntimeException("Erreur génération Excel projets", e);
        }
    }

    // ─────────────────────────────────────────────────────────────────
    // TÂCHES EN RETARD
    // ─────────────────────────────────────────────────────────────────
    public static byte[] generateTasksReport(List<TaskReportDto> data) {
        try (Workbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("Tâches en retard");
            CellStyle headerStyle = buildHeaderStyle(wb);
            CellStyle overdueStyle = buildOverdueStyle(wb);
            CellStyle normalStyle = buildNormalStyle(wb);

            addTitle(wb, sheet, "Rapport - Tâches en retard", 6);

            String[] headers = {"Tâche", "Projet", "Priorité", "Statut", "Assignée à", "Échéance", "Retard (j)"};
            Row headerRow = sheet.createRow(2);
            for (int i = 0; i < headers.length; i++) {
                Cell c = headerRow.createCell(i);
                c.setCellValue(headers[i]);
                c.setCellStyle(headerStyle);
            }

            int rowIdx = 3;
            for (TaskReportDto t : data) {
                Row row = sheet.createRow(rowIdx++);
                setCell(row, 0, t.getTitre(), overdueStyle);
                setCell(row, 1, t.getProjectNom() != null ? t.getProjectNom() : "—", normalStyle);
                setCell(row, 2, t.getPriority() != null ? t.getPriority().name() : "—", normalStyle);
                setCell(row, 3, taskStatusLabel(t.getStatus() != null ? t.getStatus().name() : ""), normalStyle);
                setCell(row, 4, fullName(t.getAssignePrenom(), t.getAssigneNom()), normalStyle);
                setCell(row, 5, t.getDateEcheance() != null ? t.getDateEcheance().format(DATE_FMT) : "—", normalStyle);
                setCell(row, 6, String.valueOf(t.getJoursRetard()), overdueStyle);
            }

            for (int i = 0; i < headers.length; i++) sheet.autoSizeColumn(i);
            return toBytes(wb);
        } catch (Exception e) {
            throw new RuntimeException("Erreur génération Excel tâches", e);
        }
    }

    // ─────────────────────────────────────────────────────────────────
    // UTILISATEURS SANS PROFIL
    // ─────────────────────────────────────────────────────────────────
    public static byte[] generateUsersReport(List<UserReportDto> data) {
        try (Workbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("Utilisateurs sans profil");
            CellStyle headerStyle = buildHeaderStyle(wb);
            CellStyle normalStyle = buildNormalStyle(wb);

            addTitle(wb, sheet, "Rapport - Utilisateurs sans profil", 3);

            String[] headers = {"Nom", "Prénom", "Email", "Rôle", "Statut", "Verrouillé"};
            Row headerRow = sheet.createRow(2);
            for (int i = 0; i < headers.length; i++) {
                Cell c = headerRow.createCell(i);
                c.setCellValue(headers[i]);
                c.setCellStyle(headerStyle);
            }

            int rowIdx = 3;
            for (UserReportDto u : data) {
                Row row = sheet.createRow(rowIdx++);
                setCell(row, 0, u.getNom(), normalStyle);
                setCell(row, 1, u.getPrenom(), normalStyle);
                setCell(row, 2, u.getEmail(), normalStyle);
                setCell(row, 3, roleLabel(u.getRole()), normalStyle);
                setCell(row, 4, u.isActive() ? "Actif" : "Inactif", normalStyle);
                setCell(row, 5, u.isLocked() ? "Oui" : "Non", normalStyle);
            }

            for (int i = 0; i < headers.length; i++) sheet.autoSizeColumn(i);
            return toBytes(wb);
        } catch (Exception e) {
            throw new RuntimeException("Erreur génération Excel utilisateurs", e);
        }
    }

    // ─────────────────────────────────────────────────────────────────
    // UTILISATEURS PAR STATUT
    // ─────────────────────────────────────────────────────────────────
    public static byte[] generateUserStatusReport(UserStatusReportDto data) {
        try (Workbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("Utilisateurs par statut");
            CellStyle headerStyle = buildHeaderStyle(wb);
            CellStyle normalStyle = buildNormalStyle(wb);
            CellStyle overdueStyle = buildOverdueStyle(wb);

            addTitle(wb, sheet, "Rapport - Utilisateurs par statut", 6);

            // Résumé stats
            Row statsHeader = sheet.createRow(2);
            setCell(statsHeader, 0, "Actifs", headerStyle);
            setCell(statsHeader, 1, "Inactifs", headerStyle);
            setCell(statsHeader, 2, "Verrouillés", headerStyle);
            setCell(statsHeader, 3, "Top réinitialisateur", headerStyle);
            setCell(statsHeader, 4, "Tentatives", headerStyle);

            Row statsRow = sheet.createRow(3);
            setCell(statsRow, 0, String.valueOf(data.getTotalActive()), normalStyle);
            setCell(statsRow, 1, String.valueOf(data.getTotalInactive()), normalStyle);
            setCell(statsRow, 2, String.valueOf(data.getTotalLocked()), normalStyle);
            if (data.getTopPasswordResetter() != null) {
                setCell(statsRow, 3,
                        fullName(data.getTopPasswordResetter().getPrenom(), data.getTopPasswordResetter().getNom()),
                        overdueStyle);
                setCell(statsRow, 4, String.valueOf(data.getMaxPasswordResetCount()), overdueStyle);
            }

            // En-têtes table
            String[] headers = {"Nom", "Prénom", "Email", "Rôle", "Profil", "Statut", "Verrouillé", "Tentatives échouées"};
            Row headerRow = sheet.createRow(5);
            for (int i = 0; i < headers.length; i++) {
                Cell c = headerRow.createCell(i);
                c.setCellValue(headers[i]);
                c.setCellStyle(headerStyle);
            }

            int rowIdx = 6;
            for (UserReportDto u : data.getUsers()) {
                Row row = sheet.createRow(rowIdx++);
                CellStyle style = u.isLocked() ? overdueStyle : normalStyle;
                setCell(row, 0, u.getNom(), style);
                setCell(row, 1, u.getPrenom(), style);
                setCell(row, 2, u.getEmail(), style);
                setCell(row, 3, roleLabel(u.getRole()), style);
                setCell(row, 4, u.getProfileLibelle() != null ? u.getProfileLibelle() : "—", style);
                setCell(row, 5, u.isActive() ? "Actif" : "Inactif", style);
                setCell(row, 6, u.isLocked() ? "Oui" : "Non", style);
                setCell(row, 7, String.valueOf(u.getFailedAttempts()), style);
            }

            for (int i = 0; i < headers.length; i++) sheet.autoSizeColumn(i);
            return toBytes(wb);
        } catch (Exception e) {
            throw new RuntimeException("Erreur génération Excel utilisateurs par statut", e);
        }
    }

    // ─────────────────────────────────────────────────────────────────
    // Helpers privés
    // ─────────────────────────────────────────────────────────────────

    private static void addTitle(Workbook wb, Sheet sheet, String text, int mergeColumns) {
        Row title = sheet.createRow(0);
        Cell c = title.createCell(0);
        c.setCellValue(text);
        CellStyle s = wb.createCellStyle();
        Font f = wb.createFont();
        f.setBold(true);
        f.setFontHeightInPoints((short) 14);
        s.setFont(f);
        c.setCellStyle(s);
        sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, mergeColumns));
    }

    private static CellStyle buildHeaderStyle(Workbook wb) {
        CellStyle s = wb.createCellStyle();
        Font f = wb.createFont();
        f.setBold(true);
        f.setColor(IndexedColors.WHITE.getIndex());
        s.setFont(f);
        s.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
        s.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        s.setBorderBottom(BorderStyle.THIN);
        s.setAlignment(HorizontalAlignment.LEFT);
        s.setWrapText(false);
        return s;
    }

    private static CellStyle buildOverdueStyle(Workbook wb) {
        CellStyle s = wb.createCellStyle();
        Font f = wb.createFont();
        f.setColor(IndexedColors.RED.getIndex());
        f.setBold(true);
        s.setFont(f);
        s.setFillForegroundColor(IndexedColors.ROSE.getIndex());
        s.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        s.setBorderBottom(BorderStyle.THIN);
        // Supprimer cette ligne : s.setBorderColor(XSSFCellBorder.BorderSide.BOTTOM, IndexedColors.RED.getIndex());
        return s;
    }

    private static CellStyle buildDateStyle(Workbook wb) {
        CellStyle s = wb.createCellStyle();
        s.setBorderBottom(BorderStyle.THIN);
        s.setAlignment(HorizontalAlignment.CENTER);
        return s;
    }

    private static CellStyle buildNormalStyle(Workbook wb) {
        CellStyle s = wb.createCellStyle();
        s.setBorderBottom(BorderStyle.THIN);
        return s;
    }

    private static void setCell(Row row, int col, String value, CellStyle style) {
        Cell c = row.createCell(col);
        c.setCellValue(value != null ? value : "");
        if (style != null) c.setCellStyle(style);
    }

    private static byte[] toBytes(Workbook wb) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        wb.write(out);
        return out.toByteArray();
    }

    private static String fullName(String prenom, String nom) {
        if (prenom == null && nom == null) return "—";
        return ((prenom != null ? prenom : "") + " " + (nom != null ? nom : "")).trim();
    }

    private static String statusLabel(String status) {
        return switch (status) {
            case "PRE_VALIDE"    -> "Pré-validé";
            case "EN_COURS"      -> "En cours";
            case "EN_VALIDATION" -> "En validation";
            case "VALIDE"        -> "Validé";
            case "CLOTURE"       -> "Clôturé";
            case "REJETE"        -> "Rejeté";
            default              -> status;
        };
    }

    private static String taskStatusLabel(String status) {
        return switch (status) {
            case "Validation" -> "En validation";
            case "En_cours"   -> "En cours";
            case "A_faire"    -> "À faire";
            case "A_revoir"   -> "À revoir";
            case "Terminé"    -> "Terminé";
            default           -> status;
        };
    }

    private static String roleLabel(String role) {
        return switch (role != null ? role : "") {
            case "ROLE_ADMIN"       -> "Admin";
            case "ROLE_MANAGER"     -> "Manager";
            case "ROLE_CHEF_PROJET" -> "Chef de projet";
            case "ROLE_MEMBRE"      -> "Membre";
            default                 -> role != null ? role : "—";
        };
    }
}
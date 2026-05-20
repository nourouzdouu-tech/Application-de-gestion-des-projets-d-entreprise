package com.dxc.dxc_platform.shared.util;

import com.dxc.dxc_platform.dto.ProjectReportDto;
import com.dxc.dxc_platform.dto.UserStatusReportDto;
import com.dxc.dxc_platform.dto.reporting.TaskReportDto;
import com.dxc.dxc_platform.dto.reporting.UserReportDto;

import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Génère des fichiers PDF sans dépendance externe (HTML → PDF via iText ou OpenPDF).
 *
 * Dépendance Maven à ajouter dans pom.xml :
 *   <dependency>
 *     <groupId>com.github.librepdf</groupId>
 *     <artifactId>openpdf</artifactId>
 *     <version>1.3.30</version>
 *   </dependency>
 *
 * Imports iText/OpenPDF utilisés :
 *   com.lowagie.text.*
 *   com.lowagie.text.pdf.*
 */
public class PdfGenerator {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    // Couleurs hex → RGB
    private static final int[] COLOR_HEADER   = {30, 64, 175};   // blue-800
    private static final int[] COLOR_OVERDUE  = {220, 38, 38};   // red-600
    private static final int[] COLOR_BG_HEAD  = {239, 246, 255}; // blue-50
    private static final int[] COLOR_BG_ODD   = {248, 250, 252}; // slate-50
    private static final int[] COLOR_TEXT      = {30, 41, 59};   // slate-800

    // ─────────────────────────────────────────────────────────────────
    // PROJETS
    // ─────────────────────────────────────────────────────────────────
    public static byte[] generateProjectsReport(List<ProjectReportDto> data) {
        try {
            com.lowagie.text.Document doc = new com.lowagie.text.Document(
                    com.lowagie.text.PageSize.A4.rotate(), 28f, 28f, 36f, 28f);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            com.lowagie.text.pdf.PdfWriter.getInstance(doc, out);
            doc.open();

            addDocTitle(doc, "Rapport — Liste des projets");
            addMeta(doc, data.size() + " projet(s)");

            com.lowagie.text.pdf.PdfPTable table = new com.lowagie.text.pdf.PdfPTable(8);
            table.setWidthPercentage(100);
            table.setWidths(new float[]{3f, 1.8f, 2f, 2.2f, 1.6f, 1.6f, 1.6f, 1.3f});

            addHeaderCell(table, "Projet");
            addHeaderCell(table, "Statut");
            addHeaderCell(table, "Équipe");
            addHeaderCell(table, "Chef de projet");
            addHeaderCell(table, "Début");
            addHeaderCell(table, "Fin estimée");
            addHeaderCell(table, "Fin réelle");
            addHeaderCell(table, "Retard");

            boolean odd = true;
            for (ProjectReportDto p : data) {
                int[] bg = p.isOverdue() ? new int[]{255, 228, 228} : (odd ? COLOR_BG_ODD : new int[]{255,255,255});
                int[] fg = p.isOverdue() ? COLOR_OVERDUE : COLOR_TEXT;

                addDataCell(table, p.getNom(), bg, fg, false);
                addDataCell(table, statusLabel(p.getStatus() != null ? p.getStatus().name() : ""), bg, fg, false);
                addDataCell(table, p.getTeamNom() != null ? p.getTeamNom() : "—", bg, fg, false);
                addDataCell(table, fullName(p.getChefProjetPrenom(), p.getChefProjetNom()), bg, fg, false);
                addDataCell(table, fmtDate(p.getDateDebut()), bg, fg, false);
                addDataCell(table, fmtDate(p.getDateFinEstimee()), bg, fg, false);
                addDataCell(table, fmtDate(p.getDateFinReelle()), bg, fg, false);
                addDataCell(table, p.isOverdue() ? p.getJoursRetard() + "j" : "—", bg, fg, true);

                odd = !odd;
            }

            doc.add(table);
            addFooter(doc);
            doc.close();
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Erreur génération PDF projets", e);
        }
    }

    // ─────────────────────────────────────────────────────────────────
    // TÂCHES EN RETARD
    // ─────────────────────────────────────────────────────────────────
    public static byte[] generateTasksReport(List<TaskReportDto> data) {
        try {
            com.lowagie.text.Document doc = new com.lowagie.text.Document(
                    com.lowagie.text.PageSize.A4.rotate(), 28f, 28f, 36f, 28f);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            com.lowagie.text.pdf.PdfWriter.getInstance(doc, out);
            doc.open();

            addDocTitle(doc, "Rapport — Tâches en retard");
            addMeta(doc, data.size() + " tâche(s) en retard");

            com.lowagie.text.pdf.PdfPTable table = new com.lowagie.text.pdf.PdfPTable(7);
            table.setWidthPercentage(100);
            table.setWidths(new float[]{3f, 2.5f, 1.5f, 1.8f, 2f, 1.8f, 1.3f});

            addHeaderCell(table, "Tâche");
            addHeaderCell(table, "Projet");
            addHeaderCell(table, "Priorité");
            addHeaderCell(table, "Statut");
            addHeaderCell(table, "Assignée à");
            addHeaderCell(table, "Échéance");
            addHeaderCell(table, "Retard");

            boolean odd = true;
            for (TaskReportDto t : data) {
                int[] bg = odd ? COLOR_BG_ODD : new int[]{255,255,255};

                addDataCell(table, t.getTitre(), bg, COLOR_OVERDUE, false);
                addDataCell(table, t.getProjectNom() != null ? t.getProjectNom() : "—", bg, COLOR_TEXT, false);
                addDataCell(table, t.getPriority() != null ? t.getPriority().name() : "—", bg, COLOR_TEXT, false);
                addDataCell(table, taskStatusLabel(t.getStatus() != null ? t.getStatus().name() : ""), bg, COLOR_TEXT, false);
                addDataCell(table, fullName(t.getAssignePrenom(), t.getAssigneNom()), bg, COLOR_TEXT, false);
                addDataCell(table, fmtDate(t.getDateEcheance()), bg, COLOR_TEXT, false);
                addDataCell(table, t.getJoursRetard() + "j", bg, COLOR_OVERDUE, true);

                odd = !odd;
            }

            doc.add(table);
            addFooter(doc);
            doc.close();
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Erreur génération PDF tâches", e);
        }
    }

    // ─────────────────────────────────────────────────────────────────
    // UTILISATEURS SANS PROFIL
    // ─────────────────────────────────────────────────────────────────
    public static byte[] generateUsersReport(List<UserReportDto> data) {
        try {
            com.lowagie.text.Document doc = new com.lowagie.text.Document(
                    com.lowagie.text.PageSize.A4, 28f, 28f, 36f, 28f);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            com.lowagie.text.pdf.PdfWriter.getInstance(doc, out);
            doc.open();

            addDocTitle(doc, "Rapport — Utilisateurs sans profil");
            addMeta(doc, data.size() + " utilisateur(s) sans profil");

            com.lowagie.text.pdf.PdfPTable table = new com.lowagie.text.pdf.PdfPTable(6);
            table.setWidthPercentage(100);
            table.setWidths(new float[]{2f, 2f, 3f, 2f, 1.5f, 1.5f});

            addHeaderCell(table, "Nom");
            addHeaderCell(table, "Prénom");
            addHeaderCell(table, "Email");
            addHeaderCell(table, "Rôle");
            addHeaderCell(table, "Statut");
            addHeaderCell(table, "Verrouillé");

            boolean odd = true;
            for (UserReportDto u : data) {
                int[] bg = odd ? COLOR_BG_ODD : new int[]{255,255,255};
                addDataCell(table, u.getNom(), bg, COLOR_TEXT, false);
                addDataCell(table, u.getPrenom(), bg, COLOR_TEXT, false);
                addDataCell(table, u.getEmail(), bg, COLOR_TEXT, false);
                addDataCell(table, roleLabel(u.getRole()), bg, COLOR_TEXT, false);
                addDataCell(table, u.isActive() ? "Actif" : "Inactif", bg, u.isActive() ? new int[]{22,163,74} : COLOR_TEXT, false);
                addDataCell(table, u.isLocked() ? "Oui" : "Non", bg, u.isLocked() ? COLOR_OVERDUE : COLOR_TEXT, false);
                odd = !odd;
            }

            doc.add(table);
            addFooter(doc);
            doc.close();
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Erreur génération PDF utilisateurs", e);
        }
    }

    // ─────────────────────────────────────────────────────────────────
    // UTILISATEURS PAR STATUT
    // ─────────────────────────────────────────────────────────────────
    public static byte[] generateUserStatusReport(UserStatusReportDto data) {
        try {
            com.lowagie.text.Document doc = new com.lowagie.text.Document(
                    com.lowagie.text.PageSize.A4.rotate(), 28f, 28f, 36f, 28f);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            com.lowagie.text.pdf.PdfWriter.getInstance(doc, out);
            doc.open();

            addDocTitle(doc, "Rapport — Utilisateurs par statut");

            // Bloc résumé
            com.lowagie.text.Font labelFont = new com.lowagie.text.Font(
                    com.lowagie.text.Font.HELVETICA, 10f, com.lowagie.text.Font.BOLD);
            com.lowagie.text.Font valueFont = new com.lowagie.text.Font(
                    com.lowagie.text.Font.HELVETICA, 12f, com.lowagie.text.Font.BOLD,
                    new java.awt.Color(COLOR_HEADER[0], COLOR_HEADER[1], COLOR_HEADER[2]));

            com.lowagie.text.pdf.PdfPTable summary = new com.lowagie.text.pdf.PdfPTable(4);
            summary.setWidthPercentage(100);
            summary.setSpacingAfter(16f);

            addSummaryCell(summary, "Actifs", String.valueOf(data.getTotalActive()), new int[]{22,163,74});
            addSummaryCell(summary, "Inactifs", String.valueOf(data.getTotalInactive()), new int[]{100,116,139});
            addSummaryCell(summary, "Verrouillés", String.valueOf(data.getTotalLocked()), COLOR_OVERDUE);
            if (data.getTopPasswordResetter() != null) {
                addSummaryCell(summary, "Top réinitialisateur",
                        fullName(data.getTopPasswordResetter().getPrenom(), data.getTopPasswordResetter().getNom())
                                + " (" + data.getMaxPasswordResetCount() + " tentatives)",
                        new int[]{124, 58, 237});
            } else {
                addSummaryCell(summary, "", "", new int[]{255,255,255});
            }
            doc.add(summary);

            // Table utilisateurs
            com.lowagie.text.pdf.PdfPTable table = new com.lowagie.text.pdf.PdfPTable(8);
            table.setWidthPercentage(100);
            table.setWidths(new float[]{2f, 2f, 3f, 2f, 2f, 1.5f, 1.5f, 1.8f});

            addHeaderCell(table, "Nom");
            addHeaderCell(table, "Prénom");
            addHeaderCell(table, "Email");
            addHeaderCell(table, "Rôle");
            addHeaderCell(table, "Profil");
            addHeaderCell(table, "Statut");
            addHeaderCell(table, "Verrouillé");
            addHeaderCell(table, "Tentatives");

            boolean odd = true;
            for (UserReportDto u : data.getUsers()) {
                int[] bg = u.isLocked() ? new int[]{255,228,228} : (odd ? COLOR_BG_ODD : new int[]{255,255,255});
                addDataCell(table, u.getNom(), bg, COLOR_TEXT, false);
                addDataCell(table, u.getPrenom(), bg, COLOR_TEXT, false);
                addDataCell(table, u.getEmail(), bg, COLOR_TEXT, false);
                addDataCell(table, roleLabel(u.getRole()), bg, COLOR_TEXT, false);
                addDataCell(table, u.getProfileLibelle() != null ? u.getProfileLibelle() : "—", bg, COLOR_TEXT, false);
                addDataCell(table, u.isActive() ? "Actif" : "Inactif", bg, u.isActive() ? new int[]{22,163,74} : COLOR_TEXT, false);
                addDataCell(table, u.isLocked() ? "Oui" : "Non", bg, u.isLocked() ? COLOR_OVERDUE : COLOR_TEXT, false);
                addDataCell(table, String.valueOf(u.getFailedAttempts()), bg, u.getFailedAttempts() >= 3 ? COLOR_OVERDUE : COLOR_TEXT, true);
                odd = !odd;
            }

            doc.add(table);
            addFooter(doc);
            doc.close();
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Erreur génération PDF utilisateurs par statut", e);
        }
    }

    // ─────────────────────────────────────────────────────────────────
    // Helpers privés
    // ─────────────────────────────────────────────────────────────────

    private static void addDocTitle(com.lowagie.text.Document doc, String text) throws Exception {
        com.lowagie.text.Font font = new com.lowagie.text.Font(
                com.lowagie.text.Font.HELVETICA, 18f, com.lowagie.text.Font.BOLD,
                new java.awt.Color(COLOR_HEADER[0], COLOR_HEADER[1], COLOR_HEADER[2]));
        com.lowagie.text.Paragraph title = new com.lowagie.text.Paragraph(text, font);
        title.setSpacingAfter(6f);
        doc.add(title);
    }

    private static void addMeta(com.lowagie.text.Document doc, String text) throws Exception {
        com.lowagie.text.Font font = new com.lowagie.text.Font(
                com.lowagie.text.Font.HELVETICA, 9f, com.lowagie.text.Font.NORMAL,
                new java.awt.Color(100, 116, 139));
        com.lowagie.text.Paragraph p = new com.lowagie.text.Paragraph(
                "Généré le " + LocalDate.now().format(DATE_FMT) + "  ·  " + text, font);
        p.setSpacingAfter(14f);
        doc.add(p);
    }

    private static void addFooter(com.lowagie.text.Document doc) throws Exception {
        com.lowagie.text.Font font = new com.lowagie.text.Font(
                com.lowagie.text.Font.HELVETICA, 8f, com.lowagie.text.Font.ITALIC,
                new java.awt.Color(148, 163, 184));
        com.lowagie.text.Paragraph footer = new com.lowagie.text.Paragraph(
                "DXC Platform — Rapport confidentiel", font);
        footer.setSpacingBefore(20f);
        doc.add(footer);
    }

    private static void addHeaderCell(com.lowagie.text.pdf.PdfPTable table, String text) {
        com.lowagie.text.Font font = new com.lowagie.text.Font(
                com.lowagie.text.Font.HELVETICA, 9f, com.lowagie.text.Font.BOLD,
                java.awt.Color.WHITE);
        com.lowagie.text.pdf.PdfPCell cell = new com.lowagie.text.pdf.PdfPCell(
                new com.lowagie.text.Phrase(text, font));
        cell.setBackgroundColor(new java.awt.Color(COLOR_HEADER[0], COLOR_HEADER[1], COLOR_HEADER[2]));
        cell.setPadding(8f);
        cell.setBorderWidth(0.5f);
        cell.setBorderColor(new java.awt.Color(147, 197, 253));
        table.addCell(cell);
    }

    private static void addDataCell(com.lowagie.text.pdf.PdfPTable table, String text,
                                    int[] bgRgb, int[] fgRgb, boolean center) {
        com.lowagie.text.Font font = new com.lowagie.text.Font(
                com.lowagie.text.Font.HELVETICA, 8.5f, com.lowagie.text.Font.NORMAL,
                new java.awt.Color(fgRgb[0], fgRgb[1], fgRgb[2]));
        com.lowagie.text.pdf.PdfPCell cell = new com.lowagie.text.pdf.PdfPCell(
                new com.lowagie.text.Phrase(text != null ? text : "—", font));
        cell.setBackgroundColor(new java.awt.Color(bgRgb[0], bgRgb[1], bgRgb[2]));
        cell.setPadding(6.5f);
        cell.setBorderWidth(0.3f);
        cell.setBorderColor(new java.awt.Color(226, 232, 240));
        if (center) cell.setHorizontalAlignment(com.lowagie.text.Element.ALIGN_CENTER);
        table.addCell(cell);
    }

    private static void addSummaryCell(com.lowagie.text.pdf.PdfPTable table, String label,
                                       String value, int[] color) {
        com.lowagie.text.Font lFont = new com.lowagie.text.Font(
                com.lowagie.text.Font.HELVETICA, 8f, com.lowagie.text.Font.NORMAL,
                new java.awt.Color(100, 116, 139));
        com.lowagie.text.Font vFont = new com.lowagie.text.Font(
                com.lowagie.text.Font.HELVETICA, 14f, com.lowagie.text.Font.BOLD,
                new java.awt.Color(color[0], color[1], color[2]));

        com.lowagie.text.pdf.PdfPCell cell = new com.lowagie.text.pdf.PdfPCell();
        cell.addElement(new com.lowagie.text.Paragraph(label, lFont));
        cell.addElement(new com.lowagie.text.Paragraph(value, vFont));
        cell.setBackgroundColor(new java.awt.Color(248, 250, 252));
        cell.setPadding(12f);
        cell.setBorderWidth(0.5f);
        cell.setBorderColor(new java.awt.Color(226, 232, 240));
        table.addCell(cell);
    }

    private static String fmtDate(java.time.LocalDate d) {
        return d != null ? d.format(DATE_FMT) : "—";
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
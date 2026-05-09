package com.dxc.dxc_platform.service;

import com.dxc.dxc_platform.dto.ReportingDataDto;
import com.lowagie.text.*;
import com.lowagie.text.Font;
import com.lowagie.text.pdf.*;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class ExportPdfService {

    // Couleurs principales
    private static final Color PRIMARY_COLOR = new Color(124, 58, 237);
    private static final Color SUCCESS_COLOR = new Color(34, 197, 94);
    private static final Color DANGER_COLOR = new Color(239, 68, 68);
    private static final Color WARNING_COLOR = new Color(245, 158, 11);
    private static final Color INFO_COLOR = new Color(59, 130, 246);
    private static final Color BG_LIGHT = new Color(248, 250, 252);
    private static final Color TEXT_DARK = new Color(30, 41, 59);
    private static final Color TEXT_MUTED = new Color(100, 116, 139);
    public byte[] exportAdminReport(ReportingDataDto data) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4);
            PdfWriter.getInstance(document, out);
            document.open();

            // ========== EN-TÊTE ==========
            addAdminHeader(document);

            // ========== SECTION 1: KPI CARDS avec chiffres ==========
            addAdminKpiCards(document, data);

            // ========== SECTION 2: ÉVOLUTION DES UTILISATEURS (chiffres + graphique) ==========
            addAdminEvolutionWithNumbers(document, data);

            // ========== SECTION 3: RÉPARTITION PAR RÔLE (chiffres + pourcentages) ==========
            addAdminRoleDistributionWithNumbers(document, data);

            // ========== SECTION 4: RÉPARTITION PAR PROFIL (chiffres + pourcentages) ==========
            addAdminProfileDistributionWithNumbers(document, data);

            // ========== SECTION 5: TOP CLIENTS (chiffres) ==========
            addAdminTopClientsWithNumbers(document, data);

            // ========== SECTION 6: SYNTHÈSE ==========
            addAdminSynthesis(document, data);

            document.close();
            return out.toByteArray();
        } catch (DocumentException | IOException e) {
            throw new RuntimeException("Erreur generation PDF admin", e);
        }
    }

    private void addAdminHeader(Document document) throws DocumentException {
        PdfPTable headerTable = new PdfPTable(1);
        headerTable.setWidthPercentage(100);
        headerTable.setSpacingAfter(15);

        PdfPCell headerCell = new PdfPCell();
        headerCell.setBackgroundColor(PRIMARY_COLOR);
        headerCell.setPadding(20);
        headerCell.setBorder(Rectangle.NO_BORDER);

        Paragraph title = new Paragraph("RAPPORT ADMINISTRATEUR",
                new Font(Font.HELVETICA, 20, Font.BOLD, Color.WHITE));
        title.setAlignment(Element.ALIGN_CENTER);
        headerCell.addElement(title);

        Paragraph date = new Paragraph("Genere le " +
                LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")),
                new Font(Font.HELVETICA, 9, Font.NORMAL, Color.WHITE));
        date.setAlignment(Element.ALIGN_CENTER);
        headerCell.addElement(date);

        headerTable.addCell(headerCell);
        document.add(headerTable);
        document.add(new Paragraph(" "));
    }

    private void addAdminKpiCards(Document document, ReportingDataDto data) throws DocumentException {
        Paragraph sectionTitle = new Paragraph("📊 INDICATEURS CLES",
                new Font(Font.HELVETICA, 14, Font.BOLD, PRIMARY_COLOR));
        sectionTitle.setSpacingBefore(5);
        sectionTitle.setSpacingAfter(10);
        document.add(sectionTitle);

        long totalUsers = data.getUserStats().getTotalUsers();
        long activeUsers = data.getUserStats().getActiveUsers();
        long inactiveUsers = data.getUserStats().getInactiveUsers();
        int newUsers = data.getUserStats().getNewUsersThisMonth();
        double activePercent = data.getUserStats().getActivePercentage();

        // Tableau 2x4 pour les KPI
        PdfPTable kpiTable = new PdfPTable(4);
        kpiTable.setWidthPercentage(100);

        // Ligne 1: Icônes
        addAdminKpiCell(kpiTable, "👥", "TOTAL", String.valueOf(totalUsers), PRIMARY_COLOR);
        addAdminKpiCell(kpiTable, "⚡", "ACTIFS", activeUsers + " (" + String.format("%.1f", activePercent) + "%)", SUCCESS_COLOR);
        addAdminKpiCell(kpiTable, "🔒", "INACTIFS", String.valueOf(inactiveUsers), DANGER_COLOR);
        addAdminKpiCell(kpiTable, "➕", "NOUVEAUX", String.valueOf(newUsers), WARNING_COLOR);

        document.add(kpiTable);
        document.add(new Paragraph(" "));
    }

    private void addAdminKpiCell(PdfPTable table, String icon, String label, String value, Color color) {
        PdfPCell cell = new PdfPCell();
        cell.setBorder(Rectangle.BOX);
        cell.setBorderColor(new Color(229, 231, 235));
        cell.setPadding(10);
        cell.setBackgroundColor(BG_LIGHT);

        Paragraph iconPara = new Paragraph(icon, new Font(Font.HELVETICA, 22));
        iconPara.setAlignment(Element.ALIGN_CENTER);
        cell.addElement(iconPara);

        Paragraph labelPara = new Paragraph(label, new Font(Font.HELVETICA, 8, Font.BOLD, TEXT_MUTED));
        labelPara.setAlignment(Element.ALIGN_CENTER);
        cell.addElement(labelPara);

        Paragraph valuePara = new Paragraph(value, new Font(Font.HELVETICA, 12, Font.BOLD, color));
        valuePara.setAlignment(Element.ALIGN_CENTER);
        cell.addElement(valuePara);

        table.addCell(cell);
    }

    private void addAdminEvolutionWithNumbers(Document document, ReportingDataDto data) throws DocumentException {
        Paragraph title = new Paragraph("📈 EVOLUTION DES UTILISATEURS",
                new Font(Font.HELVETICA, 14, Font.BOLD, PRIMARY_COLOR));
        title.setSpacingBefore(10);
        title.setSpacingAfter(8);
        document.add(title);

        List<ReportingDataDto.EvolutionDataDto> evolution = data.getUserEvolution();
        if (evolution != null && !evolution.isEmpty()) {
            // Tableau avec chiffres
            PdfPTable table = new PdfPTable(2);
            table.setWidthPercentage(60);
            table.setWidths(new float[]{50f, 50f});
            table.setSpacingAfter(10);

            PdfPCell header1 = new PdfPCell(new Paragraph("Mois", new Font(Font.HELVETICA, 9, Font.BOLD, Color.WHITE)));
            header1.setBackgroundColor(PRIMARY_COLOR);
            header1.setPadding(5);
            table.addCell(header1);

            PdfPCell header2 = new PdfPCell(new Paragraph("Nombre", new Font(Font.HELVETICA, 9, Font.BOLD, Color.WHITE)));
            header2.setBackgroundColor(PRIMARY_COLOR);
            header2.setPadding(5);
            table.addCell(header2);

            for (ReportingDataDto.EvolutionDataDto evo : evolution) {
                table.addCell(new PdfPCell(new Paragraph(evo.getMonth(), new Font(Font.HELVETICA, 9, Font.NORMAL))));
                PdfPCell countCell = new PdfPCell(new Paragraph(String.valueOf(evo.getCount()), new Font(Font.HELVETICA, 9, Font.BOLD, PRIMARY_COLOR)));
                countCell.setHorizontalAlignment(Element.ALIGN_CENTER);
                table.addCell(countCell);
            }
            document.add(table);

            // Graphique simple (barres)
            document.add(new Paragraph("Visualisation:", new Font(Font.HELVETICA, 9, Font.ITALIC, TEXT_MUTED)));

            int maxCount = 0;
            for (ReportingDataDto.EvolutionDataDto evo : evolution) {
                if (evo.getCount() > maxCount) maxCount = (int) evo.getCount();
            }
            if (maxCount == 0) maxCount = 1;

            for (ReportingDataDto.EvolutionDataDto evo : evolution) {
                int barLength = (int) ((float) evo.getCount() / maxCount * 40);
                StringBuilder bar = new StringBuilder();
                for (int i = 0; i < barLength; i++) bar.append("█");
                for (int i = barLength; i < 40; i++) bar.append("░");

                Paragraph barPara = new Paragraph(evo.getMonth() + " " + bar.toString() + " " + evo.getCount(),
                        new Font(Font.HELVETICA, 8, Font.NORMAL, PRIMARY_COLOR));
                document.add(barPara);
            }
            document.add(new Paragraph(" "));
        }
    }

    private void addAdminRoleDistributionWithNumbers(Document document, ReportingDataDto data) throws DocumentException {
        Paragraph title = new Paragraph("👥 REPARTITION PAR ROLE",
                new Font(Font.HELVETICA, 14, Font.BOLD, PRIMARY_COLOR));
        title.setSpacingBefore(10);
        title.setSpacingAfter(8);
        document.add(title);

        List<ReportingDataDto.RoleDistributionDto> roles = data.getRoleDistribution();
        if (roles != null && !roles.isEmpty()) {
            long total = 0;
            for (ReportingDataDto.RoleDistributionDto role : roles) total += role.getCount();

            PdfPTable table = new PdfPTable(3);
            table.setWidthPercentage(90);
            table.setWidths(new float[]{40f, 30f, 30f});

            PdfPCell header1 = new PdfPCell(new Paragraph("Role", new Font(Font.HELVETICA, 9, Font.BOLD, Color.WHITE)));
            header1.setBackgroundColor(PRIMARY_COLOR);
            header1.setPadding(6);
            table.addCell(header1);

            PdfPCell header2 = new PdfPCell(new Paragraph("Nombre", new Font(Font.HELVETICA, 9, Font.BOLD, Color.WHITE)));
            header2.setBackgroundColor(PRIMARY_COLOR);
            header2.setPadding(6);
            table.addCell(header2);

            PdfPCell header3 = new PdfPCell(new Paragraph("Pourcentage", new Font(Font.HELVETICA, 9, Font.BOLD, Color.WHITE)));
            header3.setBackgroundColor(PRIMARY_COLOR);
            header3.setPadding(6);
            table.addCell(header3);

            Color[] colors = {PRIMARY_COLOR, INFO_COLOR, SUCCESS_COLOR, WARNING_COLOR, DANGER_COLOR};
            for (int i = 0; i < roles.size(); i++) {
                ReportingDataDto.RoleDistributionDto role = roles.get(i);
                float percent = (role.getCount() * 100f / total);
                Color barColor = colors[i % colors.length];

                table.addCell(new PdfPCell(new Paragraph(role.getRole(), new Font(Font.HELVETICA, 9, Font.NORMAL))));

                PdfPCell countCell = new PdfPCell(new Paragraph(String.valueOf(role.getCount()), new Font(Font.HELVETICA, 9, Font.BOLD, barColor)));
                countCell.setHorizontalAlignment(Element.ALIGN_CENTER);
                table.addCell(countCell);

                // Barre + pourcentage
                int barLength = Math.round(percent / 2);
                StringBuilder bar = new StringBuilder();
                for (int j = 0; j < barLength; j++) bar.append("█");
                for (int j = barLength; j < 50; j++) bar.append("░");

                PdfPCell percentCell = new PdfPCell(new Paragraph(bar.toString() + " " + String.format("%.1f%%", percent),
                        new Font(Font.HELVETICA, 8, Font.NORMAL, barColor)));
                table.addCell(percentCell);
            }
            document.add(table);
            document.add(new Paragraph(" "));
        }
    }

    private void addAdminProfileDistributionWithNumbers(Document document, ReportingDataDto data) throws DocumentException {
        Paragraph title = new Paragraph("🧩 REPARTITION PAR PROFIL",
                new Font(Font.HELVETICA, 14, Font.BOLD, PRIMARY_COLOR));
        title.setSpacingBefore(10);
        title.setSpacingAfter(8);
        document.add(title);

        List<ReportingDataDto.ProfileDistributionDto> profiles = data.getProfileDistribution();
        if (profiles != null && !profiles.isEmpty()) {
            long total = 0;
            for (ReportingDataDto.ProfileDistributionDto profile : profiles) total += profile.getCount();

            PdfPTable table = new PdfPTable(3);
            table.setWidthPercentage(90);
            table.setWidths(new float[]{35f, 30f, 35f});

            PdfPCell header1 = new PdfPCell(new Paragraph("Profil", new Font(Font.HELVETICA, 9, Font.BOLD, Color.WHITE)));
            header1.setBackgroundColor(PRIMARY_COLOR);
            header1.setPadding(6);
            table.addCell(header1);

            PdfPCell header2 = new PdfPCell(new Paragraph("Nombre", new Font(Font.HELVETICA, 9, Font.BOLD, Color.WHITE)));
            header2.setBackgroundColor(PRIMARY_COLOR);
            header2.setPadding(6);
            table.addCell(header2);

            PdfPCell header3 = new PdfPCell(new Paragraph("Pourcentage", new Font(Font.HELVETICA, 9, Font.BOLD, Color.WHITE)));
            header3.setBackgroundColor(PRIMARY_COLOR);
            header3.setPadding(6);
            table.addCell(header3);

            Color[] colors = {PRIMARY_COLOR, INFO_COLOR, SUCCESS_COLOR, WARNING_COLOR, DANGER_COLOR};
            for (int i = 0; i < profiles.size(); i++) {
                ReportingDataDto.ProfileDistributionDto profile = profiles.get(i);
                float percent = (profile.getCount() * 100f / total);
                Color barColor = colors[i % colors.length];

                String displayName = profile.getProfile();
                if (displayName.length() > 15) displayName = displayName.substring(0, 12) + "...";

                table.addCell(new PdfPCell(new Paragraph(displayName, new Font(Font.HELVETICA, 9, Font.NORMAL))));

                PdfPCell countCell = new PdfPCell(new Paragraph(String.valueOf(profile.getCount()), new Font(Font.HELVETICA, 9, Font.BOLD, barColor)));
                countCell.setHorizontalAlignment(Element.ALIGN_CENTER);
                table.addCell(countCell);

                int barLength = Math.round(percent / 2);
                StringBuilder bar = new StringBuilder();
                for (int j = 0; j < barLength; j++) bar.append("█");
                for (int j = barLength; j < 50; j++) bar.append("░");

                PdfPCell percentCell = new PdfPCell(new Paragraph(bar.toString() + " " + String.format("%.1f%%", percent),
                        new Font(Font.HELVETICA, 8, Font.NORMAL, barColor)));
                table.addCell(percentCell);
            }
            document.add(table);
            document.add(new Paragraph(" "));
        }
    }

    private void addAdminTopClientsWithNumbers(Document document, ReportingDataDto data) throws DocumentException {
        Paragraph title = new Paragraph("🏆 TOP CLIENTS",
                new Font(Font.HELVETICA, 14, Font.BOLD, PRIMARY_COLOR));
        title.setSpacingBefore(10);
        title.setSpacingAfter(8);
        document.add(title);

        List<ReportingDataDto.ClientActivityDto> clients = data.getTopClients();
        if (clients != null && !clients.isEmpty()) {
            int maxProjects = 0;
            for (ReportingDataDto.ClientActivityDto client : clients) {
                if (client.getProjectsCount() > maxProjects) maxProjects = (int) client.getProjectsCount();
            }
            if (maxProjects == 0) maxProjects = 1;

            PdfPTable table = new PdfPTable(3);
            table.setWidthPercentage(95);
            table.setWidths(new float[]{10f, 50f, 40f});

            PdfPCell header1 = new PdfPCell(new Paragraph("#", new Font(Font.HELVETICA, 9, Font.BOLD, Color.WHITE)));
            header1.setBackgroundColor(PRIMARY_COLOR);
            header1.setPadding(6);
            table.addCell(header1);

            PdfPCell header2 = new PdfPCell(new Paragraph("Client", new Font(Font.HELVETICA, 9, Font.BOLD, Color.WHITE)));
            header2.setBackgroundColor(PRIMARY_COLOR);
            header2.setPadding(6);
            table.addCell(header2);

            PdfPCell header3 = new PdfPCell(new Paragraph("Projets", new Font(Font.HELVETICA, 9, Font.BOLD, Color.WHITE)));
            header3.setBackgroundColor(PRIMARY_COLOR);
            header3.setPadding(6);
            table.addCell(header3);

            for (int i = 0; i < Math.min(10, clients.size()); i++) {
                ReportingDataDto.ClientActivityDto client = clients.get(i);
                float percent = (client.getProjectsCount() * 100f / maxProjects);

                table.addCell(new PdfPCell(new Paragraph(String.valueOf(i + 1), new Font(Font.HELVETICA, 9, Font.BOLD))));
                table.addCell(new PdfPCell(new Paragraph(client.getClient(), new Font(Font.HELVETICA, 9, Font.NORMAL))));

                int barLength = Math.round(percent);
                StringBuilder bar = new StringBuilder();
                for (int j = 0; j < barLength; j++) bar.append("█");
                for (int j = barLength; j < 100; j++) bar.append("░");

                PdfPCell projectsCell = new PdfPCell(new Paragraph(bar.toString() + " " + client.getProjectsCount(),
                        new Font(Font.HELVETICA, 8, Font.NORMAL, PRIMARY_COLOR)));
                table.addCell(projectsCell);
            }
            document.add(table);
            document.add(new Paragraph(" "));
        }
    }

    private void addAdminSynthesis(Document document, ReportingDataDto data) throws DocumentException {
        Paragraph title = new Paragraph("📊 SYNTHESE",
                new Font(Font.HELVETICA, 14, Font.BOLD, PRIMARY_COLOR));
        title.setSpacingBefore(10);
        title.setSpacingAfter(8);
        document.add(title);

        PdfPTable card = new PdfPTable(3);
        card.setWidthPercentage(100);
        card.setWidths(new float[]{33f, 33f, 34f});

        // Carte 1: Taux d'activité
        PdfPCell activityCell = new PdfPCell();
        activityCell.setBackgroundColor(BG_LIGHT);
        activityCell.setPadding(10);
        activityCell.setBorder(Rectangle.BOX);

        Paragraph activityTitle = new Paragraph("ACTIVITE", new Font(Font.HELVETICA, 9, Font.BOLD, TEXT_MUTED));
        activityTitle.setAlignment(Element.ALIGN_CENTER);
        activityCell.addElement(activityTitle);

        float activePercent = (float) data.getUserStats().getActivePercentage();
        Paragraph percentPara = new Paragraph(String.format("%.1f%%", activePercent), new Font(Font.HELVETICA, 18, Font.BOLD, SUCCESS_COLOR));
        percentPara.setAlignment(Element.ALIGN_CENTER);
        activityCell.addElement(percentPara);

        Paragraph activeText = new Paragraph(data.getUserStats().getActiveUsers() + " / " + data.getUserStats().getTotalUsers() + " utilisateurs",
                new Font(Font.HELVETICA, 8, Font.NORMAL, TEXT_MUTED));
        activeText.setAlignment(Element.ALIGN_CENTER);
        activityCell.addElement(activeText);

        card.addCell(activityCell);

        // Carte 2: Nouveautés
        PdfPCell newCell = new PdfPCell();
        newCell.setBackgroundColor(BG_LIGHT);
        newCell.setPadding(10);
        newCell.setBorder(Rectangle.BOX);

        Paragraph newTitle = new Paragraph("NOUVEAUTES", new Font(Font.HELVETICA, 9, Font.BOLD, TEXT_MUTED));
        newTitle.setAlignment(Element.ALIGN_CENTER);
        newCell.addElement(newTitle);

        Paragraph newUsers = new Paragraph("+" + data.getUserStats().getNewUsersThisMonth(), new Font(Font.HELVETICA, 18, Font.BOLD, WARNING_COLOR));
        newUsers.setAlignment(Element.ALIGN_CENTER);
        newCell.addElement(newUsers);

        Paragraph newText = new Paragraph("nouveaux utilisateurs ce mois", new Font(Font.HELVETICA, 8, Font.NORMAL, TEXT_MUTED));
        newText.setAlignment(Element.ALIGN_CENTER);
        newCell.addElement(newText);

        card.addCell(newCell);

        // Carte 3: Rôles
        PdfPCell rolesCell = new PdfPCell();
        rolesCell.setBackgroundColor(BG_LIGHT);
        rolesCell.setPadding(10);
        rolesCell.setBorder(Rectangle.BOX);

        Paragraph rolesTitle = new Paragraph("DIVERS", new Font(Font.HELVETICA, 9, Font.BOLD, TEXT_MUTED));
        rolesTitle.setAlignment(Element.ALIGN_CENTER);
        rolesCell.addElement(rolesTitle);

        Paragraph rolesCount = new Paragraph(String.valueOf(data.getActiveRolesCount()), new Font(Font.HELVETICA, 18, Font.BOLD, INFO_COLOR));
        rolesCount.setAlignment(Element.ALIGN_CENTER);
        rolesCell.addElement(rolesCount);

        Paragraph rolesText = new Paragraph("roles actifs", new Font(Font.HELVETICA, 8, Font.NORMAL, TEXT_MUTED));
        rolesText.setAlignment(Element.ALIGN_CENTER);
        rolesCell.addElement(rolesText);

        card.addCell(rolesCell);

        document.add(card);
    }
    public byte[] exportChefProjetReport(ReportingDataDto data) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4);
            PdfWriter.getInstance(document, out);
            document.open();

            // ========== EN-TÊTE ==========
            PdfPTable headerTable = new PdfPTable(1);
            headerTable.setWidthPercentage(100);
            headerTable.setSpacingAfter(10);
            headerTable.setTotalWidth(523); // Largeur fixe

            PdfPCell headerCell = new PdfPCell();
            headerCell.setBackgroundColor(PRIMARY_COLOR);
            headerCell.setPadding(15);
            headerCell.setBorder(Rectangle.NO_BORDER);

            Paragraph title = new Paragraph("RAPPORT CHEF DE PROJET",
                    new Font(Font.HELVETICA, 18, Font.BOLD, Color.WHITE));
            title.setAlignment(Element.ALIGN_CENTER);
            headerCell.addElement(title);

            Paragraph date = new Paragraph(LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")),
                    new Font(Font.HELVETICA, 10, Font.NORMAL, Color.WHITE));
            date.setAlignment(Element.ALIGN_CENTER);
            headerCell.addElement(date);

            headerTable.addCell(headerCell);
            document.add(headerTable);
            document.add(new Paragraph(" "));

            // ========== KPI ==========
            Paragraph kpiTitle = new Paragraph("INDICATEURS CLES",
                    new Font(Font.HELVETICA, 12, Font.BOLD, PRIMARY_COLOR));
            kpiTitle.setSpacingAfter(8);
            document.add(kpiTitle);

            long projectsCount = data.getMyProjects() != null ? data.getMyProjects().size() : 0;
            long teamCount = data.getTeamPerformance() != null ? data.getTeamPerformance().size() : 0;
            long completedTasks = data.getTaskStats() != null ? data.getTaskStats().getCompletedTasks() : 0;
            long inProgressTasks = data.getTaskStats() != null ? data.getTaskStats().getInProgressTasks() : 0;
            long pendingTasks = data.getTaskStats() != null ? data.getTaskStats().getPendingTasks() : 0;
            long lateTasks = data.getTaskStats() != null ? data.getTaskStats().getLateTasks() : 0;

            // Ligne 1
            PdfPTable kpiTable1 = new PdfPTable(3);
            kpiTable1.setWidthPercentage(100);
            addKpiCell(kpiTable1, "📁", "PROJETS", String.valueOf(projectsCount), PRIMARY_COLOR);
            addKpiCell(kpiTable1, "👥", "EQUIPE", String.valueOf(teamCount), INFO_COLOR);
            addKpiCell(kpiTable1, "✅", "TERMINEES", String.valueOf(completedTasks), SUCCESS_COLOR);
            document.add(kpiTable1);

            // Ligne 2
            PdfPTable kpiTable2 = new PdfPTable(3);
            kpiTable2.setWidthPercentage(100);
            addKpiCell(kpiTable2, "⏳", "EN COURS", String.valueOf(inProgressTasks), WARNING_COLOR);
            addKpiCell(kpiTable2, "📝", "A FAIRE", String.valueOf(pendingTasks), TEXT_MUTED);
            addKpiCell(kpiTable2, "⚠️", "RETARD", String.valueOf(lateTasks), DANGER_COLOR);
            document.add(kpiTable2);
            document.add(new Paragraph(" "));

            // ========== PROJETS ==========
            Paragraph projectsTitle = new Paragraph("📊 ETAT DES PROJETS",
                    new Font(Font.HELVETICA, 11, Font.BOLD, PRIMARY_COLOR));
            projectsTitle.setSpacingAfter(8);
            document.add(projectsTitle);

            List<ReportingDataDto.ProjectSummaryDto> projects = data.getMyProjects();
            if (projects != null && !projects.isEmpty()) {
                int enCours = 0, enValidation = 0, preValides = 0, rejetes = 0;
                for (ReportingDataDto.ProjectSummaryDto p : projects) {
                    String status = p.getStatus();
                    if ("EN_COURS".equals(status)) enCours++;
                    else if ("EN_VALIDATION".equals(status)) enValidation++;
                    else if ("PRE_VALIDE".equals(status)) preValides++;
                    else if ("REJETE".equals(status)) rejetes++;
                }
                int total = enCours + enValidation + preValides + rejetes;
                if (total > 0) {
                    addProgressBarFixed(document, "En cours", enCours, total, INFO_COLOR);
                    addProgressBarFixed(document, "En validation", enValidation, total, WARNING_COLOR);
                    addProgressBarFixed(document, "Pré-validés", preValides, total, SUCCESS_COLOR);
                    addProgressBarFixed(document, "Rejetés", rejetes, total, DANGER_COLOR);
                }
            } else {
                document.add(new Paragraph("Aucun projet", new Font(Font.HELVETICA, 10, Font.NORMAL)));
            }
            document.add(new Paragraph(" "));

            // ========== TACHES ==========
            Paragraph tasksTitle = new Paragraph("✅ ETAT DES TACHES",
                    new Font(Font.HELVETICA, 11, Font.BOLD, PRIMARY_COLOR));
            tasksTitle.setSpacingAfter(8);
            document.add(tasksTitle);

            long totalTasks = data.getTaskStats() != null ? data.getTaskStats().getTotalTasks() : 0;
            if (totalTasks > 0) {
                int completed = (int) completedTasks;
                int inProgress = (int) inProgressTasks;
                int pending = (int) pendingTasks;
                int late = (int) lateTasks;
                int total = (int) totalTasks;

                addProgressBarFixed(document, "Terminées", completed, total, SUCCESS_COLOR);
                addProgressBarFixed(document, "En cours", inProgress, total, INFO_COLOR);
                addProgressBarFixed(document, "À faire", pending, total, TEXT_MUTED);
                addProgressBarFixed(document, "En retard", late, total, DANGER_COLOR);

                float completionRate = totalTasks > 0 ? (completed * 100f / totalTasks) : 0;
                Paragraph taux = new Paragraph("Taux d'achèvement: " + String.format("%.0f%%", completionRate),
                        new Font(Font.HELVETICA, 9, Font.BOLD, SUCCESS_COLOR));
                taux.setAlignment(Element.ALIGN_CENTER);
                taux.setSpacingBefore(5);
                document.add(taux);
            } else {
                document.add(new Paragraph("Aucune tâche", new Font(Font.HELVETICA, 10, Font.NORMAL)));
            }
            document.add(new Paragraph(" "));

            // ========== PERFORMANCES DE L'EQUIPE ==========
            Paragraph teamTitle = new Paragraph("👥 PERFORMANCES DE L'EQUIPE",
                    new Font(Font.HELVETICA, 12, Font.BOLD, PRIMARY_COLOR));
            teamTitle.setSpacingAfter(10);
            document.add(teamTitle);

            List<ReportingDataDto.TeamPerformanceDto> team = data.getTeamPerformance();
            if (team != null && !team.isEmpty()) {
                PdfPTable table = new PdfPTable(4);
                table.setWidthPercentage(100);
                table.setWidths(new float[]{30f, 40f, 15f, 15f});

                String[] headers = {"Membre", "Email", "Tâches", "Efficacité"};
                for (String header : headers) {
                    PdfPCell headerCell2 = new PdfPCell(new Paragraph(header, new Font(Font.HELVETICA, 9, Font.BOLD, Color.WHITE)));
                    headerCell2.setBackgroundColor(PRIMARY_COLOR);
                    headerCell2.setPadding(6);
                    table.addCell(headerCell2);
                }

                for (ReportingDataDto.TeamPerformanceDto member : team) {
                    table.addCell(new PdfPCell(new Paragraph(member.getMemberName(), new Font(Font.HELVETICA, 8, Font.NORMAL))));
                    table.addCell(new PdfPCell(new Paragraph(member.getEmail(), new Font(Font.HELVETICA, 8, Font.NORMAL))));

                    PdfPCell tasksCell = new PdfPCell(new Paragraph(String.valueOf(member.getCompletedTasks()), new Font(Font.HELVETICA, 8, Font.NORMAL)));
                    tasksCell.setHorizontalAlignment(Element.ALIGN_CENTER);
                    table.addCell(tasksCell);

                    int efficiency = (int) member.getEfficiency();
                    Color effColor = efficiency >= 70 ? SUCCESS_COLOR : (efficiency >= 40 ? WARNING_COLOR : DANGER_COLOR);
                    PdfPCell effCell = new PdfPCell(new Paragraph(efficiency + "%", new Font(Font.HELVETICA, 8, Font.BOLD, effColor)));
                    effCell.setHorizontalAlignment(Element.ALIGN_CENTER);
                    table.addCell(effCell);
                }
                document.add(table);
            } else {
                document.add(new Paragraph("Aucune donnée équipe disponible", new Font(Font.HELVETICA, 10, Font.NORMAL)));
            }

            document.close();
            return out.toByteArray();

        } catch (DocumentException | IOException e) {
            throw new RuntimeException("Erreur generation PDF: " + e.getMessage(), e);
        }
    }

    private void addKpiCell(PdfPTable table, String icon, String label, String value, Color color) {
        PdfPCell cell = new PdfPCell();
        cell.setBorder(Rectangle.BOX);
        cell.setBorderColor(new Color(229, 231, 235));
        cell.setPadding(8);
        cell.setBackgroundColor(BG_LIGHT);

        Paragraph iconPara = new Paragraph(icon, new Font(Font.HELVETICA, 20));
        iconPara.setAlignment(Element.ALIGN_CENTER);
        cell.addElement(iconPara);

        Paragraph labelPara = new Paragraph(label, new Font(Font.HELVETICA, 7, Font.BOLD, TEXT_MUTED));
        labelPara.setAlignment(Element.ALIGN_CENTER);
        cell.addElement(labelPara);

        Paragraph valuePara = new Paragraph(value, new Font(Font.HELVETICA, 14, Font.BOLD, color));
        valuePara.setAlignment(Element.ALIGN_CENTER);
        cell.addElement(valuePara);

        table.addCell(cell);
    }

    // Version corrigée de addProgressBar - sans setWidths problématique
    private void addProgressBarFixed(Document document, String label, int value, int total, Color color) throws DocumentException {
        if (total <= 0) return;

        // Utiliser un Paragraph simple au lieu d'une table complexe
        float percent = (value * 100f / total);
        percent = Math.min(percent, 100);

        // Texte du label
        Paragraph labelPara = new Paragraph(label + " (" + value + "/" + total + ") - " + String.format("%.0f%%", percent),
                new Font(Font.HELVETICA, 9, Font.NORMAL));
        document.add(labelPara);

        // Barre de progression simple (texte)
        int barLength = Math.round(percent / 2); // 50 caractères max
        StringBuilder bar = new StringBuilder();
        bar.append("█".repeat(Math.max(0, barLength)));
        bar.append("░".repeat(Math.max(0, 50 - barLength)));

        Paragraph barPara = new Paragraph(bar.toString(), new Font(Font.HELVETICA, 8, Font.NORMAL, color));
        document.add(barPara);
        document.add(new Paragraph(" "));
    }
    // ================= RAPPORT MANAGER =================

    public byte[] exportManagerReport(ReportingDataDto data) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4);
            PdfWriter.getInstance(document, out);
            document.open();

            // ========== EN-TÊTE ==========
            PdfPTable headerTable = new PdfPTable(1);
            headerTable.setWidthPercentage(100);
            headerTable.setSpacingAfter(10);

            PdfPCell headerCell = new PdfPCell();
            headerCell.setBackgroundColor(PRIMARY_COLOR);
            headerCell.setPadding(15);
            headerCell.setBorder(Rectangle.NO_BORDER);

            Paragraph title = new Paragraph("RAPPORT MANAGER",
                    new Font(Font.HELVETICA, 18, Font.BOLD, Color.WHITE));
            title.setAlignment(Element.ALIGN_CENTER);
            headerCell.addElement(title);

            Paragraph date = new Paragraph(LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")),
                    new Font(Font.HELVETICA, 10, Font.NORMAL, Color.WHITE));
            date.setAlignment(Element.ALIGN_CENTER);
            headerCell.addElement(date);

            headerTable.addCell(headerCell);
            document.add(headerTable);
            document.add(new Paragraph(" "));

            // ========== KPI ==========
            Paragraph kpiTitle = new Paragraph("INDICATEURS CLES",
                    new Font(Font.HELVETICA, 12, Font.BOLD, PRIMARY_COLOR));
            kpiTitle.setSpacingAfter(8);
            document.add(kpiTitle);

            List<ReportingDataDto.ProjectSummaryDto> managerProjects = data.getManagerProjects();
            List<ReportingDataDto.ProjectReviewDto> pendingProjects = data.getPendingProjects();
            List<ReportingDataDto.ProjectReviewDto> processedProjects = data.getProcessedProjects();

            int totalProjects = managerProjects != null ? managerProjects.size() : 0;
            int pendingCount = pendingProjects != null ? pendingProjects.size() : 0;
            int validatedCount = 0;
            int rejectedCount = 0;

            if (processedProjects != null) {
                for (ReportingDataDto.ProjectReviewDto p : processedProjects) {
                    if ("PRE_VALIDE".equals(p.getStatus())) validatedCount++;
                    else if ("REJETE".equals(p.getStatus())) rejectedCount++;
                }
            }

            double validationRate = totalProjects > 0 ? (validatedCount * 100.0 / totalProjects) : 0;

            // 4 cartes KPI
            PdfPTable kpiTable = new PdfPTable(4);
            kpiTable.setWidthPercentage(100);
            addManagerKpiCell(kpiTable, "⏳", "A VALIDER", String.valueOf(pendingCount), WARNING_COLOR);
            addManagerKpiCell(kpiTable, "✅", "VALIDES", String.valueOf(validatedCount), SUCCESS_COLOR);
            addManagerKpiCell(kpiTable, "❌", "REJETES", String.valueOf(rejectedCount), DANGER_COLOR);
            addManagerKpiCell(kpiTable, "📁", "TOTAL", String.valueOf(totalProjects), PRIMARY_COLOR);
            document.add(kpiTable);
            document.add(new Paragraph(" "));

            // ========== RÉPARTITION DES PROJETS ==========
            Paragraph statsTitle = new Paragraph("📊 REPARTITION DES PROJETS",
                    new Font(Font.HELVETICA, 11, Font.BOLD, PRIMARY_COLOR));
            statsTitle.setSpacingAfter(8);
            document.add(statsTitle);

            if (managerProjects != null && !managerProjects.isEmpty()) {
                int enCours = 0, enValidation = 0, preValides = 0, rejetes = 0;
                for (ReportingDataDto.ProjectSummaryDto p : managerProjects) {
                    String status = p.getStatus();
                    if ("EN_COURS".equals(status)) enCours++;
                    else if ("EN_VALIDATION".equals(status)) enValidation++;
                    else if ("PRE_VALIDE".equals(status)) preValides++;
                    else if ("REJETE".equals(status)) rejetes++;
                }
                int total = enCours + enValidation + preValides + rejetes;
                if (total > 0) {
                    addManagerProgressBar(document, "En cours", enCours, total, INFO_COLOR);
                    addManagerProgressBar(document, "En validation", enValidation, total, WARNING_COLOR);
                    addManagerProgressBar(document, "Pré-validés", preValides, total, SUCCESS_COLOR);
                    addManagerProgressBar(document, "Rejetés", rejetes, total, DANGER_COLOR);
                }
            } else {
                document.add(new Paragraph("Aucun projet", new Font(Font.HELVETICA, 10, Font.NORMAL)));
            }
            document.add(new Paragraph(" "));

            // ========== TAUX DE VALIDATION ==========
            Paragraph rateTitle = new Paragraph("📈 TAUX DE VALIDATION",
                    new Font(Font.HELVETICA, 11, Font.BOLD, PRIMARY_COLOR));
            rateTitle.setSpacingAfter(8);
            document.add(rateTitle);

            // Barre de validation
            int validationPercent = (int) Math.min(validationRate, 100);
            StringBuilder validationBar = new StringBuilder();
            validationBar.append("█".repeat(Math.max(0, validationPercent / 2)));
            validationBar.append("░".repeat(Math.max(0, 50 - (validationPercent / 2))));

            Paragraph validationPara = new Paragraph("Taux de validation: " + String.format("%.0f%%", validationRate),
                    new Font(Font.HELVETICA, 10, Font.BOLD, SUCCESS_COLOR));
            document.add(validationPara);

            Paragraph barPara = new Paragraph(validationBar.toString(), new Font(Font.HELVETICA, 8, Font.NORMAL, SUCCESS_COLOR));
            document.add(barPara);
            document.add(new Paragraph(" "));

            // ========== PROJETS EN ATTENTE ==========
            Paragraph pendingTitle = new Paragraph("📋 PROJETS EN ATTENTE DE VALIDATION",
                    new Font(Font.HELVETICA, 11, Font.BOLD, PRIMARY_COLOR));
            pendingTitle.setSpacingAfter(8);
            document.add(pendingTitle);

            if (pendingProjects != null && !pendingProjects.isEmpty()) {
                for (ReportingDataDto.ProjectReviewDto p : pendingProjects.stream().limit(10).toList()) {
                    PdfPTable row = new PdfPTable(2);
                    row.setWidthPercentage(100);
                    row.setWidths(new float[]{70f, 30f});
                    row.setSpacingAfter(4);

                    PdfPCell nameCell = new PdfPCell(new Paragraph(p.getName(), new Font(Font.HELVETICA, 9, Font.NORMAL)));
                    nameCell.setBorder(Rectangle.NO_BORDER);
                    row.addCell(nameCell);

                    PdfPCell clientCell = new PdfPCell(new Paragraph(p.getClient(), new Font(Font.HELVETICA, 8, Font.NORMAL, TEXT_MUTED)));
                    clientCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
                    clientCell.setBorder(Rectangle.NO_BORDER);
                    row.addCell(clientCell);

                    document.add(row);
                }
            } else {
                document.add(new Paragraph("Aucun projet en attente", new Font(Font.HELVETICA, 10, Font.NORMAL)));
            }
            document.add(new Paragraph(" "));

            // ========== HISTORIQUE DES DÉCISIONS ==========
            Paragraph historyTitle = new Paragraph("📜 HISTORIQUE DES DECISIONS",
                    new Font(Font.HELVETICA, 11, Font.BOLD, PRIMARY_COLOR));
            historyTitle.setSpacingAfter(8);
            document.add(historyTitle);

            if (processedProjects != null && !processedProjects.isEmpty()) {
                PdfPTable table = new PdfPTable(3);
                table.setWidthPercentage(100);
                table.setWidths(new float[]{40f, 30f, 30f});

                String[] headers = {"Projet", "Client", "Decision"};
                for (String header : headers) {
                    PdfPCell headerCell2 = new PdfPCell(new Paragraph(header, new Font(Font.HELVETICA, 9, Font.BOLD, Color.WHITE)));
                    headerCell2.setBackgroundColor(PRIMARY_COLOR);
                    headerCell2.setPadding(5);
                    table.addCell(headerCell2);
                }

                for (ReportingDataDto.ProjectReviewDto p : processedProjects.stream().limit(15).toList()) {
                    table.addCell(new PdfPCell(new Paragraph(p.getName(), new Font(Font.HELVETICA, 8, Font.NORMAL))));
                    table.addCell(new PdfPCell(new Paragraph(p.getClient(), new Font(Font.HELVETICA, 8, Font.NORMAL))));

                    String decision = "PRE_VALIDE".equals(p.getStatus()) ? "✅ Valide" : "❌ Rejete";
                    Color decisionColor = "PRE_VALIDE".equals(p.getStatus()) ? SUCCESS_COLOR : DANGER_COLOR;
                    PdfPCell decisionCell = new PdfPCell(new Paragraph(decision, new Font(Font.HELVETICA, 8, Font.BOLD, decisionColor)));
                    decisionCell.setHorizontalAlignment(Element.ALIGN_CENTER);
                    table.addCell(decisionCell);
                }
                document.add(table);
            } else {
                document.add(new Paragraph("Aucun historique", new Font(Font.HELVETICA, 10, Font.NORMAL)));
            }

            document.close();
            return out.toByteArray();

        } catch (DocumentException | IOException e) {
            throw new RuntimeException("Erreur generation PDF manager: " + e.getMessage(), e);
        }
    }

    private void addManagerKpiCell(PdfPTable table, String icon, String label, String value, Color color) {
        PdfPCell cell = new PdfPCell();
        cell.setBorder(Rectangle.BOX);
        cell.setBorderColor(new Color(229, 231, 235));
        cell.setPadding(10);
        cell.setBackgroundColor(BG_LIGHT);

        Paragraph iconPara = new Paragraph(icon, new Font(Font.HELVETICA, 22));
        iconPara.setAlignment(Element.ALIGN_CENTER);
        cell.addElement(iconPara);

        Paragraph labelPara = new Paragraph(label, new Font(Font.HELVETICA, 8, Font.BOLD, TEXT_MUTED));
        labelPara.setAlignment(Element.ALIGN_CENTER);
        cell.addElement(labelPara);

        Paragraph valuePara = new Paragraph(value, new Font(Font.HELVETICA, 18, Font.BOLD, color));
        valuePara.setAlignment(Element.ALIGN_CENTER);
        cell.addElement(valuePara);

        table.addCell(cell);
    }

    private void addManagerProgressBar(Document document, String label, int value, int total, Color color) throws DocumentException {
        if (total <= 0) return;

        float percent = (value * 100f / total);
        percent = Math.min(percent, 100);

        Paragraph labelPara = new Paragraph(label + " (" + value + "/" + total + ") - " + String.format("%.0f%%", percent),
                new Font(Font.HELVETICA, 9, Font.NORMAL));
        document.add(labelPara);

        int barLength = Math.round(percent / 2);
        StringBuilder bar = new StringBuilder();
        bar.append("█".repeat(Math.max(0, barLength)));
        bar.append("░".repeat(Math.max(0, 50 - barLength)));

        Paragraph barPara = new Paragraph(bar.toString(), new Font(Font.HELVETICA, 8, Font.NORMAL, color));
        document.add(barPara);
        document.add(new Paragraph(" "));
    }
    public byte[] exportMembreEquipeReport(ReportingDataDto data) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4);
            PdfWriter.getInstance(document, out);
            document.open();

            // ========== EN-TÊTE ==========
            PdfPTable headerTable = new PdfPTable(1);
            headerTable.setWidthPercentage(100);
            headerTable.setSpacingAfter(10);

            PdfPCell headerCell = new PdfPCell();
            headerCell.setBackgroundColor(PRIMARY_COLOR);
            headerCell.setPadding(15);
            headerCell.setBorder(Rectangle.NO_BORDER);

            Paragraph title = new Paragraph("RAPPORT MEMBRE EQUIPE",
                    new Font(Font.HELVETICA, 18, Font.BOLD, Color.WHITE));
            title.setAlignment(Element.ALIGN_CENTER);
            headerCell.addElement(title);

            Paragraph date = new Paragraph(LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")),
                    new Font(Font.HELVETICA, 10, Font.NORMAL, Color.WHITE));
            date.setAlignment(Element.ALIGN_CENTER);
            headerCell.addElement(date);

            headerTable.addCell(headerCell);
            document.add(headerTable);
            document.add(new Paragraph(" "));

            // ========== INDICATEURS CLES ==========
            Paragraph kpiTitle = new Paragraph("INDICATEURS CLES",
                    new Font(Font.HELVETICA, 12, Font.BOLD, PRIMARY_COLOR));
            kpiTitle.setSpacingAfter(8);
            document.add(kpiTitle);

            long totalTasks = data.getPersonalTaskStats() != null ? data.getPersonalTaskStats().getTotalTasks() : 0;
            long completedTasks = data.getPersonalTaskStats() != null ? data.getPersonalTaskStats().getCompletedTasks() : 0;
            long inProgressTasks = data.getPersonalTaskStats() != null ? data.getPersonalTaskStats().getInProgressTasks() : 0;
            long pendingTasks = data.getPersonalTaskStats() != null ? data.getPersonalTaskStats().getPendingTasks() : 0;
            long lateTasks = data.getPersonalTaskStats() != null ? data.getPersonalTaskStats().getLateTasks() : 0;

            PdfPTable kpiTable = new PdfPTable(5);
            kpiTable.setWidthPercentage(100);
            addMembreKpiCell(kpiTable, "📋", "TOTAL", String.valueOf(totalTasks), PRIMARY_COLOR);
            addMembreKpiCell(kpiTable, "✅", "TERMINEES", String.valueOf(completedTasks), SUCCESS_COLOR);
            addMembreKpiCell(kpiTable, "⏳", "EN COURS", String.valueOf(inProgressTasks), WARNING_COLOR);
            addMembreKpiCell(kpiTable, "📝", "A FAIRE", String.valueOf(pendingTasks), INFO_COLOR);
            addMembreKpiCell(kpiTable, "⚠️", "RETARD", String.valueOf(lateTasks), DANGER_COLOR);
            document.add(kpiTable);
            document.add(new Paragraph(" "));

            // ========== TAUX D'ACHEVEMENT ==========
            Paragraph rateTitle = new Paragraph("📈 TAUX D'ACHEVEMENT",
                    new Font(Font.HELVETICA, 11, Font.BOLD, PRIMARY_COLOR));
            rateTitle.setSpacingAfter(8);
            document.add(rateTitle);

            double completionRate = data.getPersonalCompletionRate();
            int ratePercent = (int) Math.min(completionRate, 100);

            StringBuilder rateBar = new StringBuilder();
            rateBar.append("█".repeat(Math.max(0, ratePercent / 2)));
            rateBar.append("░".repeat(Math.max(0, 50 - (ratePercent / 2))));

            Paragraph ratePara = new Paragraph("Progression: " + String.format("%.0f%%", completionRate),
                    new Font(Font.HELVETICA, 10, Font.BOLD, SUCCESS_COLOR));
            document.add(ratePara);

            Paragraph barPara = new Paragraph(rateBar.toString(), new Font(Font.HELVETICA, 8, Font.NORMAL, SUCCESS_COLOR));
            document.add(barPara);
            document.add(new Paragraph(" "));

            // ========== REPARTITION PAR PRIORITE ==========
            Paragraph priorityTitle = new Paragraph("🎯 REPARTITION PAR PRIORITE",
                    new Font(Font.HELVETICA, 11, Font.BOLD, PRIMARY_COLOR));
            priorityTitle.setSpacingAfter(8);
            document.add(priorityTitle);

            long highCount = data.getPriorityDistribution() != null ? data.getPriorityDistribution().getHighPriorityCount() : 0;
            long mediumCount = data.getPriorityDistribution() != null ? data.getPriorityDistribution().getMediumPriorityCount() : 0;
            long lowCount = data.getPriorityDistribution() != null ? data.getPriorityDistribution().getLowPriorityCount() : 0;
            long total = highCount + mediumCount + lowCount;

            if (total > 0) {
                addMembreProgressBar(document, "Haute priorite", (int) highCount, (int) total, DANGER_COLOR);
                addMembreProgressBar(document, "Priorite moyenne", (int) mediumCount, (int) total, WARNING_COLOR);
                addMembreProgressBar(document, "Basse priorite", (int) lowCount, (int) total, INFO_COLOR);
            } else {
                document.add(new Paragraph("Aucune tache", new Font(Font.HELVETICA, 10, Font.NORMAL)));
            }
            document.add(new Paragraph(" "));

            // ========== EVOLUTION HEBDOMADAIRE ==========
            Paragraph weeklyTitle = new Paragraph("📅 EVOLUTION HEBDOMADAIRE",
                    new Font(Font.HELVETICA, 11, Font.BOLD, PRIMARY_COLOR));
            weeklyTitle.setSpacingAfter(8);
            document.add(weeklyTitle);

            List<ReportingDataDto.WeeklyEvolutionDto> weekly = data.getWeeklyEvolution();
            if (weekly != null && !weekly.isEmpty()) {
                PdfPTable weeklyTable = new PdfPTable(3);
                weeklyTable.setWidthPercentage(100);
                weeklyTable.setWidths(new float[]{30f, 35f, 35f});

                String[] headers = {"Semaine", "Taches", "Taux"};
                for (String header : headers) {
                    PdfPCell headerCell2 = new PdfPCell(new Paragraph(header, new Font(Font.HELVETICA, 9, Font.BOLD, Color.WHITE)));
                    headerCell2.setBackgroundColor(PRIMARY_COLOR);
                    headerCell2.setPadding(5);
                    weeklyTable.addCell(headerCell2);
                }

                for (ReportingDataDto.WeeklyEvolutionDto w : weekly) {
                    weeklyTable.addCell(new PdfPCell(new Paragraph(w.getWeek(), new Font(Font.HELVETICA, 8, Font.NORMAL))));
                    weeklyTable.addCell(new PdfPCell(new Paragraph(w.getCompleted() + "/" + w.getTotal(), new Font(Font.HELVETICA, 8, Font.NORMAL))));
                    weeklyTable.addCell(new PdfPCell(new Paragraph(String.format("%.0f%%", w.getCompletionRate()), new Font(Font.HELVETICA, 8, Font.BOLD, SUCCESS_COLOR))));
                }
                document.add(weeklyTable);
            } else {
                document.add(new Paragraph("Aucune donnee hebdomadaire", new Font(Font.HELVETICA, 10, Font.NORMAL)));
            }
            document.add(new Paragraph(" "));

            // ========== TACHES PRIORITAIRES (HAUTE) ==========
            Paragraph highTasksTitle = new Paragraph("⚠️ TACHES PRIORITAIRES (HAUTE)",
                    new Font(Font.HELVETICA, 11, Font.BOLD, PRIMARY_COLOR));
            highTasksTitle.setSpacingAfter(8);
            document.add(highTasksTitle);

            List<ReportingDataDto.TaskSummaryDto> highPriorityTasks = data.getCurrentTasks() != null ?
                    data.getCurrentTasks().stream()
                            .filter(t -> "HAUTE".equals(t.getPriority()))
                            .limit(10)
                            .toList() : List.of();

            if (!highPriorityTasks.isEmpty()) {
                PdfPTable tasksTable = new PdfPTable(3);
                tasksTable.setWidthPercentage(100);
                tasksTable.setWidths(new float[]{40f, 35f, 25f});

                String[] taskHeaders = {"Tache", "Projet", "Echeance"};
                for (String header : taskHeaders) {
                    PdfPCell headerCell2 = new PdfPCell(new Paragraph(header, new Font(Font.HELVETICA, 9, Font.BOLD, Color.WHITE)));
                    headerCell2.setBackgroundColor(DANGER_COLOR);
                    headerCell2.setPadding(5);
                    tasksTable.addCell(headerCell2);
                }

                for (ReportingDataDto.TaskSummaryDto t : highPriorityTasks) {
                    tasksTable.addCell(new PdfPCell(new Paragraph(t.getTitle(), new Font(Font.HELVETICA, 8, Font.NORMAL))));
                    tasksTable.addCell(new PdfPCell(new Paragraph(t.getProjectName() != null ? t.getProjectName() : "-", new Font(Font.HELVETICA, 8, Font.NORMAL))));

                    String dateStr = t.getEstimatedEndDate() != null ? t.getEstimatedEndDate() : "Non definie";
                    PdfPCell dateCell = new PdfPCell(new Paragraph(dateStr, new Font(Font.HELVETICA, 8, Font.NORMAL, DANGER_COLOR)));
                    tasksTable.addCell(dateCell);
                }
                document.add(tasksTable);
            } else {
                document.add(new Paragraph("Aucune tache prioritaire", new Font(Font.HELVETICA, 10, Font.NORMAL)));
            }
            document.add(new Paragraph(" "));

            // ========== TACHES A FAIRE ==========
            Paragraph todoTitle = new Paragraph("📝 TACHES A FAIRE",
                    new Font(Font.HELVETICA, 11, Font.BOLD, PRIMARY_COLOR));
            todoTitle.setSpacingAfter(8);
            document.add(todoTitle);

            List<ReportingDataDto.TaskSummaryDto> todoTasks = data.getCurrentTasks() != null ?
                    data.getCurrentTasks().stream()
                            .filter(t -> "A_faire".equals(t.getStatus()) || "PENDING".equals(t.getStatus()))
                            .limit(10)
                            .toList() : List.of();

            if (!todoTasks.isEmpty()) {
                PdfPTable todoTable = new PdfPTable(4);
                todoTable.setWidthPercentage(100);
                todoTable.setWidths(new float[]{35f, 30f, 20f, 15f});

                String[] todoHeaders = {"Tache", "Projet", "Priorite", "Echeance"};
                for (String header : todoHeaders) {
                    PdfPCell headerCell2 = new PdfPCell(new Paragraph(header, new Font(Font.HELVETICA, 9, Font.BOLD, Color.WHITE)));
                    headerCell2.setBackgroundColor(INFO_COLOR);
                    headerCell2.setPadding(5);
                    todoTable.addCell(headerCell2);
                }

                for (ReportingDataDto.TaskSummaryDto t : todoTasks) {
                    todoTable.addCell(new PdfPCell(new Paragraph(t.getTitle(), new Font(Font.HELVETICA, 8, Font.NORMAL))));
                    todoTable.addCell(new PdfPCell(new Paragraph(t.getProjectName() != null ? t.getProjectName() : "-", new Font(Font.HELVETICA, 8, Font.NORMAL))));

                    // Priorité
                    Color priorityColor = "HAUTE".equals(t.getPriority()) ? DANGER_COLOR :
                            ("MOYENNE".equals(t.getPriority()) ? WARNING_COLOR : INFO_COLOR);
                    String priorityLabel = "HAUTE".equals(t.getPriority()) ? "Haute" :
                            ("MOYENNE".equals(t.getPriority()) ? "Moyenne" : "Basse");
                    PdfPCell priorityCell = new PdfPCell(new Paragraph(priorityLabel, new Font(Font.HELVETICA, 8, Font.BOLD, priorityColor)));
                    priorityCell.setHorizontalAlignment(Element.ALIGN_CENTER);
                    todoTable.addCell(priorityCell);

                    String dateStr = t.getEstimatedEndDate() != null ? t.getEstimatedEndDate() : "Non definie";
                    PdfPCell dateCell = new PdfPCell(new Paragraph(dateStr, new Font(Font.HELVETICA, 8, Font.NORMAL)));
                    dateCell.setHorizontalAlignment(Element.ALIGN_CENTER);
                    todoTable.addCell(dateCell);
                }
                document.add(todoTable);
            } else {
                document.add(new Paragraph("Aucune tache a faire", new Font(Font.HELVETICA, 10, Font.NORMAL)));
            }

            document.close();
            return out.toByteArray();

        } catch (DocumentException | IOException e) {
            throw new RuntimeException("Erreur generation PDF membre equipe: " + e.getMessage(), e);
        }
    }
    // Ajoutez ces méthodes après la méthode exportMembreEquipeReport

    private void addMembreKpiCell(PdfPTable table, String icon, String label, String value, Color color) {
        PdfPCell cell = new PdfPCell();
        cell.setBorder(Rectangle.BOX);
        cell.setBorderColor(new Color(229, 231, 235));
        cell.setPadding(8);
        cell.setBackgroundColor(BG_LIGHT);

        Paragraph iconPara = new Paragraph(icon, new Font(Font.HELVETICA, 18));
        iconPara.setAlignment(Element.ALIGN_CENTER);
        cell.addElement(iconPara);

        Paragraph labelPara = new Paragraph(label, new Font(Font.HELVETICA, 7, Font.BOLD, TEXT_MUTED));
        labelPara.setAlignment(Element.ALIGN_CENTER);
        cell.addElement(labelPara);

        Paragraph valuePara = new Paragraph(value, new Font(Font.HELVETICA, 12, Font.BOLD, color));
        valuePara.setAlignment(Element.ALIGN_CENTER);
        cell.addElement(valuePara);

        table.addCell(cell);
    }

    private void addMembreProgressBar(Document document, String label, int value, int total, Color color) throws DocumentException {
        if (total <= 0) return;

        float percent = (value * 100f / total);
        percent = Math.min(percent, 100);

        Paragraph labelPara = new Paragraph(label + " (" + value + "/" + total + ") - " + String.format("%.0f%%", percent),
                new Font(Font.HELVETICA, 9, Font.NORMAL));
        document.add(labelPara);

        int barLength = Math.round(percent / 2);
        StringBuilder bar = new StringBuilder();
        for (int i = 0; i < barLength; i++) {
            bar.append("█");
        }
        for (int i = barLength; i < 50; i++) {
            bar.append("░");
        }

        Paragraph barPara = new Paragraph(bar.toString(), new Font(Font.HELVETICA, 8, Font.NORMAL, color));
        document.add(barPara);
        document.add(new Paragraph(" "));
    }
    // ================= RAPPORT RESPONSABLE CONTRAT =================

    public byte[] exportResponsableContratReport(ReportingDataDto data) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4);
            PdfWriter.getInstance(document, out);
            document.open();

            // ========== EN-TÊTE ==========
            addRcHeader(document);

            // ========== KPI CARDS ==========
            addRcKpiCards(document, data);

            // ========== RÉPARTITION DES PROFILS ==========
            addRcProfileDistribution(document, data);

            // ========== PROJETS PAR MOIS ==========
            addRcProjectsByMonth(document, data);

            // ========== TOP CLIENTS ==========
            addRcTopClients(document, data);

            // ========== SYNTHÈSE ==========
            addRcSynthesis(document, data);

            document.close();
            return out.toByteArray();
        } catch (DocumentException | IOException e) {
            throw new RuntimeException("Erreur generation PDF responsable contrat: " + e.getMessage(), e);
        }
    }

    private void addRcHeader(Document document) throws DocumentException {
        PdfPTable headerTable = new PdfPTable(1);
        headerTable.setWidthPercentage(100);
        headerTable.setSpacingAfter(15);

        PdfPCell headerCell = new PdfPCell();
        headerCell.setBackgroundColor(PRIMARY_COLOR);
        headerCell.setPadding(20);
        headerCell.setBorder(Rectangle.NO_BORDER);

        Paragraph title = new Paragraph("RAPPORT RESPONSABLE CONTRAT",
                new Font(Font.HELVETICA, 18, Font.BOLD, Color.WHITE));
        title.setAlignment(Element.ALIGN_CENTER);
        headerCell.addElement(title);

        Paragraph date = new Paragraph(LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")),
                new Font(Font.HELVETICA, 10, Font.NORMAL, Color.WHITE));
        date.setAlignment(Element.ALIGN_CENTER);
        headerCell.addElement(date);

        headerTable.addCell(headerCell);
        document.add(headerTable);
        document.add(new Paragraph(" "));
    }

    private void addRcKpiCards(Document document, ReportingDataDto data) throws DocumentException {
        Paragraph sectionTitle = new Paragraph("INDICATEURS CLES",
                new Font(Font.HELVETICA, 14, Font.BOLD, PRIMARY_COLOR));
        sectionTitle.setSpacingBefore(5);
        sectionTitle.setSpacingAfter(10);
        document.add(sectionTitle);

        long totalProjects = data.getAllProjects() != null ? data.getAllProjects().size() : 0;
        long enValidation = 0;
        long preValides = 0;
        long rejetes = 0;

        if (data.getAllProjects() != null) {
            for (ReportingDataDto.ProjectSummaryDto p : data.getAllProjects()) {
                String status = p.getStatus();
                if ("EN_VALIDATION".equals(status)) enValidation++;
                else if ("PRE_VALIDE".equals(status)) preValides++;
                else if ("REJETE".equals(status)) rejetes++;
            }
        }

        long activeClients = data.getActiveClientsCount();
        double totalHT = data.getBillingStats() != null ? data.getBillingStats().getTotalHT().doubleValue() : 0;
        double totalTTC = data.getBillingStats() != null ? data.getBillingStats().getTotalTTC().doubleValue() : 0;

        PdfPTable kpiTable = new PdfPTable(3);
        kpiTable.setWidthPercentage(100);
        kpiTable.setWidths(new float[]{33f, 33f, 34f});

        addRcKpiCell(kpiTable, "📁", "PROJETS", String.valueOf(totalProjects), PRIMARY_COLOR);
        addRcKpiCell(kpiTable, "⏳", "EN VALIDATION", String.valueOf(enValidation), WARNING_COLOR);
        addRcKpiCell(kpiTable, "✓", "PRE-VALIDES", String.valueOf(preValides), SUCCESS_COLOR);

        PdfPTable kpiTable2 = new PdfPTable(3);
        kpiTable2.setWidthPercentage(100);
        kpiTable2.setWidths(new float[]{33f, 33f, 34f});

        addRcKpiCell(kpiTable2, "❌", "REJETES", String.valueOf(rejetes), DANGER_COLOR);
        addRcKpiCell(kpiTable2, "🏢", "CLIENTS ACTIFS", String.valueOf(activeClients), INFO_COLOR);
        addRcKpiCell(kpiTable2, "💰", "CA HT", String.format("%.0f", totalHT) + " MAD", SUCCESS_COLOR);

        document.add(kpiTable);
        document.add(kpiTable2);
        document.add(new Paragraph(" "));
    }

    private void addRcKpiCell(PdfPTable table, String icon, String label, String value, Color color) {
        PdfPCell cell = new PdfPCell();
        cell.setBorder(Rectangle.BOX);
        cell.setBorderColor(new Color(229, 231, 235));
        cell.setPadding(10);
        cell.setBackgroundColor(BG_LIGHT);

        Paragraph iconPara = new Paragraph(icon, new Font(Font.HELVETICA, 22));
        iconPara.setAlignment(Element.ALIGN_CENTER);
        cell.addElement(iconPara);

        Paragraph labelPara = new Paragraph(label, new Font(Font.HELVETICA, 8, Font.BOLD, TEXT_MUTED));
        labelPara.setAlignment(Element.ALIGN_CENTER);
        cell.addElement(labelPara);

        Paragraph valuePara = new Paragraph(value, new Font(Font.HELVETICA, 12, Font.BOLD, color));
        valuePara.setAlignment(Element.ALIGN_CENTER);
        cell.addElement(valuePara);

        table.addCell(cell);
    }

    private void addRcProfileDistribution(Document document, ReportingDataDto data) throws DocumentException {
        Paragraph title = new Paragraph("REPARTITION DES PROFILS",
                new Font(Font.HELVETICA, 14, Font.BOLD, PRIMARY_COLOR));
        title.setSpacingBefore(10);
        title.setSpacingAfter(10);
        document.add(title);

        List<ReportingDataDto.ProfileDistributionDto> profiles = data.getProfileDistribution();
        if (profiles != null && !profiles.isEmpty()) {
            long total = 0;
            for (ReportingDataDto.ProfileDistributionDto profile : profiles) total += profile.getCount();

            PdfPTable table = new PdfPTable(3);
            table.setWidthPercentage(90);
            table.setWidths(new float[]{35f, 15f, 50f});

            PdfPCell header1 = new PdfPCell(new Paragraph("PROFIL", new Font(Font.HELVETICA, 10, Font.BOLD, Color.WHITE)));
            header1.setBackgroundColor(PRIMARY_COLOR);
            header1.setPadding(8);
            header1.setHorizontalAlignment(Element.ALIGN_CENTER);
            table.addCell(header1);

            PdfPCell header2 = new PdfPCell(new Paragraph("NBR", new Font(Font.HELVETICA, 10, Font.BOLD, Color.WHITE)));
            header2.setBackgroundColor(PRIMARY_COLOR);
            header2.setPadding(8);
            header2.setHorizontalAlignment(Element.ALIGN_CENTER);
            table.addCell(header2);

            PdfPCell header3 = new PdfPCell(new Paragraph("REPARTITION", new Font(Font.HELVETICA, 10, Font.BOLD, Color.WHITE)));
            header3.setBackgroundColor(PRIMARY_COLOR);
            header3.setPadding(8);
            header3.setHorizontalAlignment(Element.ALIGN_CENTER);
            table.addCell(header3);

            Color[] colors = {PRIMARY_COLOR, INFO_COLOR, SUCCESS_COLOR, WARNING_COLOR, DANGER_COLOR};

            for (int i = 0; i < profiles.size(); i++) {
                ReportingDataDto.ProfileDistributionDto profile = profiles.get(i);
                float percent = (profile.getCount() * 100f / total);
                Color barColor = colors[i % colors.length];

                String displayName = profile.getProfile();
                if (displayName.length() > 20) displayName = displayName.substring(0, 18) + "...";

                table.addCell(new PdfPCell(new Paragraph(displayName, new Font(Font.HELVETICA, 9, Font.NORMAL))));

                PdfPCell countCell = new PdfPCell(new Paragraph(String.valueOf(profile.getCount()),
                        new Font(Font.HELVETICA, 10, Font.BOLD, barColor)));
                countCell.setHorizontalAlignment(Element.ALIGN_CENTER);
                table.addCell(countCell);

                int barLength = Math.round(percent);
                StringBuilder bar = new StringBuilder();
                for (int j = 0; j < barLength; j++) bar.append("█");
                for (int j = barLength; j < 100; j++) bar.append("░");

                PdfPCell percentCell = new PdfPCell(new Paragraph(bar.toString() + "  " + String.format("%.1f", percent) + "%",
                        new Font(Font.HELVETICA, 8, Font.NORMAL, barColor)));
                table.addCell(percentCell);
            }
            document.add(table);
            document.add(new Paragraph(" "));
        }
    }

    private void addRcProjectsByMonth(Document document, ReportingDataDto data) throws DocumentException {
        Paragraph title = new Paragraph("PROJETS PAR MOIS",
                new Font(Font.HELVETICA, 14, Font.BOLD, PRIMARY_COLOR));
        title.setSpacingBefore(10);
        title.setSpacingAfter(10);
        document.add(title);

        List<ReportingDataDto.MonthlyProjectDto> monthly = data.getProjectsByMonth();
        if (monthly != null && !monthly.isEmpty()) {
            int maxCount = 0;
            for (ReportingDataDto.MonthlyProjectDto m : monthly) {
                if (m.getCount() > maxCount) maxCount = (int) m.getCount();
            }
            if (maxCount == 0) maxCount = 1;

            PdfPTable table = new PdfPTable(2);
            table.setWidthPercentage(80);
            table.setWidths(new float[]{40f, 60f});

            PdfPCell header1 = new PdfPCell(new Paragraph("MOIS", new Font(Font.HELVETICA, 10, Font.BOLD, Color.WHITE)));
            header1.setBackgroundColor(PRIMARY_COLOR);
            header1.setPadding(8);
            header1.setHorizontalAlignment(Element.ALIGN_CENTER);
            table.addCell(header1);

            PdfPCell header2 = new PdfPCell(new Paragraph("NOMBRE DE PROJETS", new Font(Font.HELVETICA, 10, Font.BOLD, Color.WHITE)));
            header2.setBackgroundColor(PRIMARY_COLOR);
            header2.setPadding(8);
            header2.setHorizontalAlignment(Element.ALIGN_CENTER);
            table.addCell(header2);

            for (ReportingDataDto.MonthlyProjectDto m : monthly) {
                table.addCell(new PdfPCell(new Paragraph(m.getMonth(), new Font(Font.HELVETICA, 9, Font.NORMAL))));

                float percent = (m.getCount() * 100f / maxCount);
                int barLength = Math.round(percent);
                StringBuilder bar = new StringBuilder();
                for (int j = 0; j < barLength; j++) bar.append("█");
                for (int j = barLength; j < 100; j++) bar.append("░");

                PdfPCell countCell = new PdfPCell(new Paragraph(bar.toString() + "  " + m.getCount(),
                        new Font(Font.HELVETICA, 8, Font.NORMAL, PRIMARY_COLOR)));
                table.addCell(countCell);
            }
            document.add(table);
            document.add(new Paragraph(" "));
        }
    }

    private void addRcTopClients(Document document, ReportingDataDto data) throws DocumentException {
        Paragraph title = new Paragraph("TOP CLIENTS",
                new Font(Font.HELVETICA, 14, Font.BOLD, PRIMARY_COLOR));
        title.setSpacingBefore(10);
        title.setSpacingAfter(10);
        document.add(title);

        List<ReportingDataDto.ClientRevenueDto> clients = data.getTopClientsByRevenue();
        if (clients != null && !clients.isEmpty()) {
            double maxHT = 0;
            for (ReportingDataDto.ClientRevenueDto client : clients) {
                if (client.getTotalHT() != null && client.getTotalHT().doubleValue() > maxHT) {
                    maxHT = client.getTotalHT().doubleValue();
                }
            }
            if (maxHT == 0) maxHT = 1;

            PdfPTable table = new PdfPTable(3);
            table.setWidthPercentage(95);
            table.setWidths(new float[]{10f, 50f, 40f});

            PdfPCell header1 = new PdfPCell(new Paragraph("RANG", new Font(Font.HELVETICA, 10, Font.BOLD, Color.WHITE)));
            header1.setBackgroundColor(PRIMARY_COLOR);
            header1.setPadding(8);
            header1.setHorizontalAlignment(Element.ALIGN_CENTER);
            table.addCell(header1);

            PdfPCell header2 = new PdfPCell(new Paragraph("CLIENT", new Font(Font.HELVETICA, 10, Font.BOLD, Color.WHITE)));
            header2.setBackgroundColor(PRIMARY_COLOR);
            header2.setPadding(8);
            table.addCell(header2);

            PdfPCell header3 = new PdfPCell(new Paragraph("CA HT (MAD)", new Font(Font.HELVETICA, 10, Font.BOLD, Color.WHITE)));
            header3.setBackgroundColor(PRIMARY_COLOR);
            header3.setPadding(8);
            header3.setHorizontalAlignment(Element.ALIGN_CENTER);
            table.addCell(header3);

            for (int i = 0; i < Math.min(8, clients.size()); i++) {
                ReportingDataDto.ClientRevenueDto client = clients.get(i);
                double ht = client.getTotalHT() != null ? client.getTotalHT().doubleValue() : 0;
                float percent = (float) (ht * 100 / maxHT);

                String rank = (i == 0) ? "🥇" : (i == 1) ? "🥈" : (i == 2) ? "🥉" : String.valueOf(i + 1);
                PdfPCell rankCell = new PdfPCell(new Paragraph(rank, new Font(Font.HELVETICA, 11, Font.NORMAL)));
                rankCell.setHorizontalAlignment(Element.ALIGN_CENTER);
                rankCell.setPadding(6);
                table.addCell(rankCell);

                String clientName = client.getClient();
                if (clientName.length() > 25) clientName = clientName.substring(0, 22) + "...";
                table.addCell(new PdfPCell(new Paragraph(clientName, new Font(Font.HELVETICA, 9, Font.NORMAL))));

                // Barre + CA
                int barLength = Math.round(percent);
                StringBuilder bar = new StringBuilder();
                for (int j = 0; j < barLength; j++) bar.append("█");
                for (int j = barLength; j < 100; j++) bar.append("░");

                PdfPCell htCell = new PdfPCell(new Paragraph(bar.toString() + "  " + String.format("%.0f", ht),
                        new Font(Font.HELVETICA, 8, Font.NORMAL, SUCCESS_COLOR)));
                table.addCell(htCell);
            }
            document.add(table);
            document.add(new Paragraph(" "));
        }
    }

    private void addRcSynthesis(Document document, ReportingDataDto data) throws DocumentException {
        Paragraph title = new Paragraph("SYNTHESE",
                new Font(Font.HELVETICA, 14, Font.BOLD, PRIMARY_COLOR));
        title.setSpacingBefore(10);
        title.setSpacingAfter(10);
        document.add(title);

        long totalProjects = data.getAllProjects() != null ? data.getAllProjects().size() : 0;
        long enValidation = 0;
        long preValides = 0;

        if (data.getAllProjects() != null) {
            for (ReportingDataDto.ProjectSummaryDto p : data.getAllProjects()) {
                if ("EN_VALIDATION".equals(p.getStatus())) enValidation++;
                else if ("PRE_VALIDE".equals(p.getStatus())) preValides++;
            }
        }

        double validationRate = totalProjects > 0 ? (preValides * 100.0 / totalProjects) : 0;
        double totalHT = data.getBillingStats() != null ? data.getBillingStats().getTotalHT().doubleValue() : 0;
        long activeClients = data.getActiveClientsCount();

        PdfPTable card = new PdfPTable(3);
        card.setWidthPercentage(100);
        card.setWidths(new float[]{33f, 33f, 34f});

        // Carte 1: Taux de validation
        PdfPCell rateCell = new PdfPCell();
        rateCell.setBackgroundColor(BG_LIGHT);
        rateCell.setPadding(12);
        rateCell.setBorder(Rectangle.BOX);

        Paragraph rateTitle = new Paragraph("TAUX DE VALIDATION", new Font(Font.HELVETICA, 9, Font.BOLD, TEXT_MUTED));
        rateTitle.setAlignment(Element.ALIGN_CENTER);
        rateCell.addElement(rateTitle);

        Paragraph rateValue = new Paragraph(String.format("%.1f%%", validationRate), new Font(Font.HELVETICA, 20, Font.BOLD, SUCCESS_COLOR));
        rateValue.setAlignment(Element.ALIGN_CENTER);
        rateCell.addElement(rateValue);

        Paragraph rateSub = new Paragraph(preValides + " / " + totalProjects + " projets", new Font(Font.HELVETICA, 8, Font.NORMAL, TEXT_MUTED));
        rateSub.setAlignment(Element.ALIGN_CENTER);
        rateCell.addElement(rateSub);

        card.addCell(rateCell);

        // Carte 2: CA Total
        PdfPCell caCell = new PdfPCell();
        caCell.setBackgroundColor(BG_LIGHT);
        caCell.setPadding(12);
        caCell.setBorder(Rectangle.BOX);

        Paragraph caTitle = new Paragraph("CHIFFRE D'AFFAIRES", new Font(Font.HELVETICA, 9, Font.BOLD, TEXT_MUTED));
        caTitle.setAlignment(Element.ALIGN_CENTER);
        caCell.addElement(caTitle);

        Paragraph caValue = new Paragraph(String.format("%.0f", totalHT) + " MAD", new Font(Font.HELVETICA, 16, Font.BOLD, SUCCESS_COLOR));
        caValue.setAlignment(Element.ALIGN_CENTER);
        caCell.addElement(caValue);

        Paragraph caSub = new Paragraph("HT", new Font(Font.HELVETICA, 8, Font.NORMAL, TEXT_MUTED));
        caSub.setAlignment(Element.ALIGN_CENTER);
        caCell.addElement(caSub);

        card.addCell(caCell);

        // Carte 3: Clients actifs
        PdfPCell clientCell = new PdfPCell();
        clientCell.setBackgroundColor(BG_LIGHT);
        clientCell.setPadding(12);
        clientCell.setBorder(Rectangle.BOX);

        Paragraph clientTitle = new Paragraph("CLIENTS ACTIFS", new Font(Font.HELVETICA, 9, Font.BOLD, TEXT_MUTED));
        clientTitle.setAlignment(Element.ALIGN_CENTER);
        clientCell.addElement(clientTitle);

        Paragraph clientValue = new Paragraph(String.valueOf(activeClients), new Font(Font.HELVETICA, 20, Font.BOLD, INFO_COLOR));
        clientValue.setAlignment(Element.ALIGN_CENTER);
        clientCell.addElement(clientValue);

        Paragraph clientSub = new Paragraph("entreprises partenaires", new Font(Font.HELVETICA, 8, Font.NORMAL, TEXT_MUTED));
        clientSub.setAlignment(Element.ALIGN_CENTER);
        clientCell.addElement(clientSub);

        card.addCell(clientCell);

        document.add(card);
    }
}
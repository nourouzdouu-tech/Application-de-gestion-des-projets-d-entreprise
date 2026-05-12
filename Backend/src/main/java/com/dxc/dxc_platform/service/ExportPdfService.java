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

    // ── Palette ────────────────────────────────────────────────────────────────
    private static final Color PRIMARY        = new Color(109, 40, 217);   // violet-700
    private static final Color PRIMARY_LIGHT  = new Color(237, 233, 254);  // violet-100
    private static final Color PRIMARY_DARK   = new Color(76,  29, 149);   // violet-900
    private static final Color SUCCESS        = new Color(22, 163, 74);    // green-600
    private static final Color SUCCESS_LIGHT  = new Color(220, 252, 231);  // green-100
    private static final Color DANGER         = new Color(220, 38, 38);    // red-600
    private static final Color DANGER_LIGHT   = new Color(254, 226, 226);  // red-100
    private static final Color WARNING        = new Color(217, 119, 6);    // amber-600
    private static final Color WARNING_LIGHT  = new Color(254, 243, 199);  // amber-100
    private static final Color INFO           = new Color(37, 99, 235);    // blue-600
    private static final Color INFO_LIGHT     = new Color(219, 234, 254);  // blue-100
    private static final Color SURFACE        = new Color(248, 250, 252);  // slate-50
    private static final Color BORDER         = new Color(226, 232, 240);  // slate-200
    private static final Color TEXT_DARK      = new Color(15, 23, 42);     // slate-900
    private static final Color TEXT_MUTED     = new Color(100, 116, 139);  // slate-500
    private static final Color WHITE          = Color.WHITE;

    // Couleur de header dégradé simulé via bande secondaire
    private static final Color HEADER_ACCENT  = new Color(91, 33, 182);   // violet-800

    // ── Fonts ──────────────────────────────────────────────────────────────────
    private Font h1(Color c)     { return new Font(Font.HELVETICA, 20, Font.BOLD,   c); }
    private Font h2(Color c)     { return new Font(Font.HELVETICA, 11, Font.BOLD,   c); }
    private Font h3(Color c)     { return new Font(Font.HELVETICA, 9,  Font.BOLD,   c); }
    private Font body(Color c)   { return new Font(Font.HELVETICA, 9,  Font.NORMAL, c); }
    private Font bodyB(Color c)  { return new Font(Font.HELVETICA, 9,  Font.BOLD,   c); }
    private Font small(Color c)  { return new Font(Font.HELVETICA, 8,  Font.NORMAL, c); }
    private Font smallB(Color c) { return new Font(Font.HELVETICA, 8,  Font.BOLD,   c); }
    private Font caption(Color c){ return new Font(Font.HELVETICA, 7,  Font.NORMAL, c); }
    private Font captionB(Color c){return new Font(Font.HELVETICA, 7,  Font.BOLD,   c); }
    private Font kpiValue(Color c){ return new Font(Font.HELVETICA, 17, Font.BOLD, c); }

    // ── Spacers ────────────────────────────────────────────────────────────────
    private Paragraph spacer(float pts) {
        Paragraph p = new Paragraph(" ");
        p.setSpacingAfter(pts);
        return p;
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  ADMIN REPORT
    // ─────────────────────────────────────────────────────────────────────────
    public byte[] exportAdminReport(ReportingDataDto data) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document doc = new Document(PageSize.A4, 36, 36, 36, 50);
            PdfWriter writer = PdfWriter.getInstance(doc, out);
            writer.setPageEvent(new FooterEvent("Rapport Administrateur"));
            doc.open();

            addHeader(doc, "RAPPORT ADMINISTRATEUR", "Tableau de bord & indicateurs plateforme");
            addAdminKpiCards(doc, data);
            addSectionDivider(doc, "ÉVOLUTION DES UTILISATEURS");
            addAdminEvolution(doc, data);
            addSectionDivider(doc, "RÉPARTITION PAR RÔLE");
            addDistributionTable(doc, buildRoleRows(data), new String[]{"Rôle", "Nb", "Répartition"});
            addSectionDivider(doc, "RÉPARTITION PAR PROFIL");
            addDistributionTable(doc, buildProfileRows(data), new String[]{"Profil", "Nb", "Répartition"});
            addSectionDivider(doc, "TOP CLIENTS");
            addAdminTopClients(doc, data);
            addSectionDivider(doc, "SYNTHÈSE GLOBALE");
            addAdminSynthesis(doc, data);

            doc.close();
            return out.toByteArray();
        } catch (DocumentException | IOException e) {
            throw new RuntimeException("Erreur génération PDF admin", e);
        }
    }

    // ── Header deux bandes ──────────────────────────────────────────────────────
    private void addHeader(Document doc, String title, String subtitle) throws DocumentException {
        // Bande principale
        PdfPTable t = new PdfPTable(1);
        t.setWidthPercentage(100);
        t.setSpacingAfter(0);

        PdfPCell top = new PdfPCell();
        top.setBackgroundColor(PRIMARY);
        top.setPaddingTop(26);
        top.setPaddingBottom(4);
        top.setPaddingLeft(28);
        top.setPaddingRight(28);
        top.setBorder(Rectangle.NO_BORDER);

        Paragraph titleP = new Paragraph(title, h1(WHITE));
        titleP.setAlignment(Element.ALIGN_CENTER);
        titleP.setSpacingAfter(3);
        top.addElement(titleP);

        Paragraph subP = new Paragraph(subtitle, small(new Color(196, 181, 253)));
        subP.setAlignment(Element.ALIGN_CENTER);
        top.addElement(subP);

        t.addCell(top);
        doc.add(t);

        // Bande date (couleur légèrement différente pour l'effet d'accent)
        PdfPTable t2 = new PdfPTable(1);
        t2.setWidthPercentage(100);
        t2.setSpacingAfter(16);

        PdfPCell bot = new PdfPCell();
        bot.setBackgroundColor(HEADER_ACCENT);
        bot.setPaddingTop(6);
        bot.setPaddingBottom(8);
        bot.setPaddingLeft(28);
        bot.setPaddingRight(28);
        bot.setBorder(Rectangle.NO_BORDER);

        Paragraph dateP = new Paragraph(
                "Généré le " + LocalDate.now().format(DateTimeFormatter.ofPattern("dd MMMM yyyy", java.util.Locale.FRENCH)),
                caption(new Color(167, 139, 250))
        );
        dateP.setAlignment(Element.ALIGN_CENTER);
        bot.addElement(dateP);

        t2.addCell(bot);
        doc.add(t2);
    }

    // ── Section divider modernisé ──────────────────────────────────────────────
    private void addSectionDivider(Document doc, String label) throws DocumentException {
        doc.add(spacer(4));

        PdfPTable t = new PdfPTable(new float[]{3f, 97f});
        t.setWidthPercentage(100);
        t.setSpacingAfter(8);

        // Barre verticale colorée
        PdfPCell bar = new PdfPCell(new Phrase(" "));
        bar.setBackgroundColor(PRIMARY);
        bar.setBorder(Rectangle.NO_BORDER);
        bar.setPaddingTop(0);
        bar.setPaddingBottom(0);
        t.addCell(bar);

        // Label sur fond doux
        PdfPCell lbl = new PdfPCell();
        lbl.setBackgroundColor(PRIMARY_LIGHT);
        lbl.setBorder(Rectangle.NO_BORDER);
        lbl.setPaddingLeft(10);
        lbl.setPaddingTop(7);
        lbl.setPaddingBottom(7);
        lbl.setPaddingRight(10);
        Paragraph p = new Paragraph(label, h2(PRIMARY_DARK));
        lbl.addElement(p);
        t.addCell(lbl);

        doc.add(t);
    }

    // ── KPI Cards Admin ────────────────────────────────────────────────────────
    private void addAdminKpiCards(Document doc, ReportingDataDto data) throws DocumentException {
        long total      = data.getUserStats().getTotalUsers();
        long active     = data.getUserStats().getActiveUsers();
        long inactive   = data.getUserStats().getInactiveUsers();
        int  newU       = data.getUserStats().getNewUsersThisMonth();
        double activePct= data.getUserStats().getActivePercentage();

        PdfPTable t = new PdfPTable(4);
        t.setWidthPercentage(100);
        t.setSpacingAfter(6);

        addKpiCard(t, "UTILISATEURS", String.valueOf(total),
                "Total inscrits", PRIMARY, PRIMARY_LIGHT);
        addKpiCard(t, "ACTIFS",
                active + " (" + String.format("%.0f", activePct) + "%)",
                "Comptes actifs", SUCCESS, SUCCESS_LIGHT);
        addKpiCard(t, "INACTIFS", String.valueOf(inactive),
                "Comptes inactifs", DANGER, DANGER_LIGHT);
        addKpiCard(t, "CE MOIS", "+" + newU,
                "Nouveaux inscrits", WARNING, WARNING_LIGHT);

        doc.add(t);
        doc.add(spacer(4));
    }

    private void addKpiCard(PdfPTable table, String label, String value, String sub, Color accent, Color bg) {
        PdfPCell cell = new PdfPCell();
        cell.setBackgroundColor(bg);
        // Bordure basse colorée plus épaisse pour l'effet "tab"
        cell.setBorderColorBottom(accent);
        cell.setBorderWidthBottom(3f);
        cell.setBorderColorTop(BORDER);
        cell.setBorderWidthTop(0.5f);
        cell.setBorderColorLeft(BORDER);
        cell.setBorderWidthLeft(0.5f);
        cell.setBorderColorRight(BORDER);
        cell.setBorderWidthRight(0.5f);
        cell.setPaddingTop(13);
        cell.setPaddingBottom(13);
        cell.setPaddingLeft(8);
        cell.setPaddingRight(8);

        Paragraph lp = new Paragraph(label, captionB(accent));
        lp.setAlignment(Element.ALIGN_CENTER);
        lp.setSpacingAfter(3);
        cell.addElement(lp);

        Paragraph vp = new Paragraph(value, kpiValue(accent));
        vp.setAlignment(Element.ALIGN_CENTER);
        vp.setSpacingAfter(3);
        cell.addElement(vp);

        Paragraph sp = new Paragraph(sub, caption(TEXT_MUTED));
        sp.setAlignment(Element.ALIGN_CENTER);
        cell.addElement(sp);

        table.addCell(cell);
    }

    // ── Évolution (barres horizontales propres) ────────────────────────────────
    private void addAdminEvolution(Document doc, ReportingDataDto data) throws DocumentException {
        List<ReportingDataDto.EvolutionDataDto> evo = data.getUserEvolution();
        if (evo == null || evo.isEmpty()) {
            doc.add(emptyState("Aucune donnée d'évolution"));
            return;
        }
        long max = evo.stream().mapToLong(ReportingDataDto.EvolutionDataDto::getCount).max().orElse(1);

        PdfPTable t = new PdfPTable(new float[]{22f, 9f, 69f});
        t.setWidthPercentage(88);
        t.setSpacingAfter(12);

        addTableHeader(t, new String[]{"Mois", "Nb", "Évolution"});

        boolean alt = false;
        for (ReportingDataDto.EvolutionDataDto e : evo) {
            Color bg = alt ? SURFACE : WHITE;
            alt = !alt;
            int filled = (int) Math.round(e.getCount() * 52.0 / max);
            addCell(t, e.getMonth(), body(TEXT_DARK), bg, Element.ALIGN_LEFT);
            addCell(t, String.valueOf(e.getCount()), bodyB(PRIMARY), bg, Element.ALIGN_CENTER);
            addBarCell(t, filled, 52, PRIMARY, bg);
        }
        doc.add(t);
    }

    // ── Distribution table ──────────────────────────────────────────────────────
    private void addDistributionTable(Document doc, Object[][] rows, String[] headers) throws DocumentException {
        if (rows == null || rows.length == 0) {
            doc.add(emptyState("Aucune donnée"));
            return;
        }
        PdfPTable t = new PdfPTable(new float[]{32f, 10f, 58f});
        t.setWidthPercentage(92);
        t.setSpacingAfter(12);

        addTableHeader(t, headers);

        Color[] accents = {PRIMARY, INFO, SUCCESS, WARNING, DANGER};
        boolean alt = false;
        for (int i = 0; i < rows.length; i++) {
            Color bg  = alt ? SURFACE : WHITE;
            alt = !alt;
            Color accent = accents[i % accents.length];
            String name  = (String) rows[i][0];
            long   count = (long)   rows[i][1];
            float  pct   = (float)  rows[i][2];
            int filled   = Math.round(pct * 52f / 100f);

            addCell(t, name, body(TEXT_DARK), bg, Element.ALIGN_LEFT);
            addCell(t, String.valueOf(count), bodyB(accent), bg, Element.ALIGN_CENTER);
            addBarCellWithPct(t, filled, 52, pct, accent, bg);
        }
        doc.add(t);
    }

    private Object[][] buildRoleRows(ReportingDataDto data) {
        List<ReportingDataDto.RoleDistributionDto> list = data.getRoleDistribution();
        if (list == null) return new Object[0][0];
        long total = list.stream().mapToLong(ReportingDataDto.RoleDistributionDto::getCount).sum();
        Object[][] rows = new Object[list.size()][3];
        for (int i = 0; i < list.size(); i++) {
            rows[i][0] = list.get(i).getRole();
            rows[i][1] = list.get(i).getCount();
            rows[i][2] = total > 0 ? (list.get(i).getCount() * 100f / total) : 0f;
        }
        return rows;
    }

    private Object[][] buildProfileRows(ReportingDataDto data) {
        List<ReportingDataDto.ProfileDistributionDto> list = data.getProfileDistribution();
        if (list == null) return new Object[0][0];
        long total = list.stream().mapToLong(ReportingDataDto.ProfileDistributionDto::getCount).sum();
        Object[][] rows = new Object[list.size()][3];
        for (int i = 0; i < list.size(); i++) {
            String name = list.get(i).getProfile();
            if (name.length() > 22) name = name.substring(0, 20) + "…";
            rows[i][0] = name;
            rows[i][1] = list.get(i).getCount();
            rows[i][2] = total > 0 ? (list.get(i).getCount() * 100f / total) : 0f;
        }
        return rows;
    }

    // ── Top Clients Admin ──────────────────────────────────────────────────────
    private void addAdminTopClients(Document doc, ReportingDataDto data) throws DocumentException {
        List<ReportingDataDto.ClientActivityDto> clients = data.getTopClients();
        if (clients == null || clients.isEmpty()) {
            doc.add(emptyState("Aucun client à afficher"));
            return;
        }
        long max = clients.stream().mapToLong(ReportingDataDto.ClientActivityDto::getProjectsCount).max().orElse(1);

        PdfPTable t = new PdfPTable(new float[]{7f, 38f, 55f});
        t.setWidthPercentage(96);
        t.setSpacingAfter(12);

        addTableHeader(t, new String[]{"#", "Client", "Projets"});

        Color[] medals = {new Color(202, 138, 4), new Color(148, 163, 184), new Color(180, 83, 9)};
        boolean alt = false;
        for (int i = 0; i < Math.min(10, clients.size()); i++) {
            Color bg = alt ? SURFACE : WHITE;
            alt = !alt;
            ReportingDataDto.ClientActivityDto c = clients.get(i);
            int filled = (int) Math.round(c.getProjectsCount() * 52.0 / max);
            String rank = i < 3 ? new String[]{"1er","2e","3e"}[i] : String.valueOf(i + 1);
            Color rankColor = i < 3 ? medals[i] : TEXT_MUTED;

            addCell(t, rank, bodyB(rankColor), bg, Element.ALIGN_CENTER);
            addCell(t, c.getClient(), body(TEXT_DARK), bg, Element.ALIGN_LEFT);
            addBarCellWithCount(t, filled, 52, c.getProjectsCount(), PRIMARY, bg);
        }
        doc.add(t);
    }

    // ── Admin Synthesis ────────────────────────────────────────────────────────
    private void addAdminSynthesis(Document doc, ReportingDataDto data) throws DocumentException {
        float activePct = (float) data.getUserStats().getActivePercentage();
        int   newUsers  = data.getUserStats().getNewUsersThisMonth();
        long  active    = data.getUserStats().getActiveUsers();
        long  total     = data.getUserStats().getTotalUsers();
        long  roles     = data.getActiveRolesCount();

        PdfPTable t = new PdfPTable(3);
        t.setWidthPercentage(100);
        t.setSpacingAfter(10);

        addSynthCard(t, "TAUX D'ACTIVITÉ",
                String.format("%.1f%%", activePct),
                active + " / " + total + " utilisateurs", SUCCESS);
        addSynthCard(t, "NOUVEAUX CE MOIS",
                "+" + newUsers,
                "inscriptions récentes", WARNING);
        addSynthCard(t, "RÔLES ACTIFS",
                String.valueOf(roles),
                "profils en service", INFO);

        doc.add(t);
    }

    private void addSynthCard(PdfPTable table, String title, String value, String sub, Color accent) {
        PdfPCell cell = new PdfPCell();
        cell.setBackgroundColor(WHITE);
        cell.setBorderColor(BORDER);
        cell.setBorderWidth(0.5f);
        cell.setBorderColorTop(accent);
        cell.setBorderWidthTop(3f);
        cell.setPaddingTop(14);
        cell.setPaddingBottom(14);
        cell.setPaddingLeft(10);
        cell.setPaddingRight(10);

        Paragraph tp = new Paragraph(title, captionB(TEXT_MUTED));
        tp.setAlignment(Element.ALIGN_CENTER);
        tp.setSpacingAfter(5);
        cell.addElement(tp);

        Paragraph vp = new Paragraph(value, new Font(Font.HELVETICA, 20, Font.BOLD, accent));
        vp.setAlignment(Element.ALIGN_CENTER);
        vp.setSpacingAfter(4);
        cell.addElement(vp);

        Paragraph sp = new Paragraph(sub, caption(TEXT_MUTED));
        sp.setAlignment(Element.ALIGN_CENTER);
        cell.addElement(sp);

        table.addCell(cell);
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  CHEF DE PROJET REPORT
    // ─────────────────────────────────────────────────────────────────────────
    public byte[] exportChefProjetReport(ReportingDataDto data) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document doc = new Document(PageSize.A4, 36, 36, 36, 50);
            PdfWriter writer = PdfWriter.getInstance(doc, out);
            writer.setPageEvent(new FooterEvent("Rapport Chef de Projet"));
            doc.open();

            addHeader(doc, "RAPPORT CHEF DE PROJET", "Suivi des projets, tâches et équipe");

            long proj    = data.getMyProjects() != null ? data.getMyProjects().size() : 0;
            long team    = data.getTeamPerformance() != null ? data.getTeamPerformance().size() : 0;
            long done    = data.getTaskStats() != null ? data.getTaskStats().getCompletedTasks() : 0;
            long inProg  = data.getTaskStats() != null ? data.getTaskStats().getInProgressTasks() : 0;
            long pending = data.getTaskStats() != null ? data.getTaskStats().getPendingTasks() : 0;
            long late    = data.getTaskStats() != null ? data.getTaskStats().getLateTasks() : 0;

            // 6 KPI en 2 lignes de 3
            PdfPTable row1 = new PdfPTable(3);
            row1.setWidthPercentage(100);
            row1.setSpacingAfter(4);
            addKpiCard(row1, "PROJETS",   String.valueOf(proj),    "sous ma responsabilité", PRIMARY, PRIMARY_LIGHT);
            addKpiCard(row1, "ÉQUIPE",    String.valueOf(team),    "membres assignés",        INFO,    INFO_LIGHT);
            addKpiCard(row1, "TERMINÉES", String.valueOf(done),    "tâches achevées",         SUCCESS, SUCCESS_LIGHT);
            doc.add(row1);

            PdfPTable row2 = new PdfPTable(3);
            row2.setWidthPercentage(100);
            row2.setSpacingAfter(12);
            addKpiCard(row2, "EN COURS",  String.valueOf(inProg),  "tâches actives",          WARNING, WARNING_LIGHT);
            addKpiCard(row2, "À FAIRE",   String.valueOf(pending), "tâches planifiées",       INFO,    INFO_LIGHT);
            addKpiCard(row2, "RETARD",    String.valueOf(late),    "tâches en retard",        DANGER,  DANGER_LIGHT);
            doc.add(row2);

            addSectionDivider(doc, "ÉTAT DES PROJETS");
            addProjectStatus(doc, data.getMyProjects());

            addSectionDivider(doc, "ÉTAT DES TÂCHES");
            long totalTasks = data.getTaskStats() != null ? data.getTaskStats().getTotalTasks() : 0;
            if (totalTasks > 0) {
                addProgressBarsTable(doc,
                        new String[]{"Terminées", "En cours", "À faire", "En retard"},
                        new long[]{done, inProg, pending, late},
                        totalTasks,
                        new Color[]{SUCCESS, INFO, TEXT_MUTED, DANGER});
                float rate = done * 100f / totalTasks;
                addRateChip(doc, "Taux d'achèvement : " + String.format("%.0f%%", rate), SUCCESS, SUCCESS_LIGHT);
            }

            addSectionDivider(doc, "PERFORMANCES DE L'ÉQUIPE");
            addTeamTable(doc, data.getTeamPerformance());

            doc.close();
            return out.toByteArray();
        } catch (DocumentException | IOException e) {
            throw new RuntimeException("Erreur génération PDF chef de projet", e);
        }
    }

    private void addProjectStatus(Document doc, List<ReportingDataDto.ProjectSummaryDto> projects) throws DocumentException {
        if (projects == null || projects.isEmpty()) {
            doc.add(emptyState("Aucun projet"));
            return;
        }
        int enCours = 0, enVal = 0, preVal = 0, rejetes = 0;
        for (ReportingDataDto.ProjectSummaryDto p : projects) {
            switch (p.getStatus() != null ? p.getStatus() : "") {
                case "EN_COURS"      -> enCours++;
                case "EN_VALIDATION" -> enVal++;
                case "PRE_VALIDE"    -> preVal++;
                case "REJETE"        -> rejetes++;
            }
        }
        int total = enCours + enVal + preVal + rejetes;
        if (total > 0) {
            addProgressBarsTable(doc,
                    new String[]{"En cours", "En validation", "Pré-validés", "Rejetés"},
                    new long[]{enCours, enVal, preVal, rejetes},
                    total,
                    new Color[]{INFO, WARNING, SUCCESS, DANGER});
        }
    }

    private void addProgressBarsTable(Document doc, String[] labels, long[] values, long total, Color[] colors) throws DocumentException {
        PdfPTable t = new PdfPTable(new float[]{20f, 8f, 62f, 10f});
        t.setWidthPercentage(90);
        t.setSpacingAfter(10);

        for (int i = 0; i < labels.length; i++) {
            float pct    = total > 0 ? Math.min(values[i] * 100f / total, 100f) : 0f;
            int   filled = Math.round(pct * 52f / 100f);
            Color accent = colors[i % colors.length];

            addCell(t, labels[i], body(TEXT_DARK), WHITE, Element.ALIGN_LEFT);
            addCell(t, String.valueOf(values[i]), bodyB(accent), WHITE, Element.ALIGN_CENTER);
            addBarCell(t, filled, 52, accent, WHITE);
            addCell(t, String.format("%.0f%%", pct), smallB(accent), WHITE, Element.ALIGN_RIGHT);
        }
        doc.add(t);
    }

    private void addRateChip(Document doc, String text, Color color, Color bg) throws DocumentException {
        PdfPTable t = new PdfPTable(1);
        t.setWidthPercentage(38);
        t.setHorizontalAlignment(Element.ALIGN_CENTER);
        t.setSpacingAfter(10);

        PdfPCell cell = new PdfPCell(new Paragraph(text, bodyB(color)));
        cell.setBackgroundColor(bg);
        cell.setBorderColor(color);
        cell.setBorderWidth(0.5f);
        cell.setPaddingTop(5);
        cell.setPaddingBottom(5);
        cell.setPaddingLeft(10);
        cell.setPaddingRight(10);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        t.addCell(cell);
        doc.add(t);
    }

    private void addTeamTable(Document doc, List<ReportingDataDto.TeamPerformanceDto> team) throws DocumentException {
        if (team == null || team.isEmpty()) {
            doc.add(emptyState("Aucune donnée équipe"));
            return;
        }
        PdfPTable t = new PdfPTable(new float[]{26f, 36f, 14f, 24f});
        t.setWidthPercentage(100);
        t.setSpacingAfter(10);

        addTableHeader(t, new String[]{"Membre", "Email", "Tâches", "Efficacité"});

        boolean alt = false;
        for (ReportingDataDto.TeamPerformanceDto m : team) {
            Color bg = alt ? SURFACE : WHITE;
            alt = !alt;
            int eff = (int) m.getEfficiency();
            Color effColor = eff >= 70 ? SUCCESS : (eff >= 40 ? WARNING : DANGER);
            Color effBg    = eff >= 70 ? SUCCESS_LIGHT : (eff >= 40 ? WARNING_LIGHT : DANGER_LIGHT);

            addCell(t, m.getMemberName(), bodyB(TEXT_DARK), bg, Element.ALIGN_LEFT);
            addCell(t, m.getEmail(), small(TEXT_MUTED), bg, Element.ALIGN_LEFT);
            addCell(t, String.valueOf(m.getCompletedTasks()), bodyB(INFO), bg, Element.ALIGN_CENTER);

            // Cellule efficacité avec badge coloré
            PdfPCell effCell = new PdfPCell();
            effCell.setBackgroundColor(bg);
            effCell.setBorder(Rectangle.BOX);
            effCell.setBorderColor(BORDER);
            effCell.setBorderWidth(0.5f);
            effCell.setPaddingTop(5);
            effCell.setPaddingBottom(5);
            effCell.setHorizontalAlignment(Element.ALIGN_CENTER);

            PdfPTable badge = new PdfPTable(1);
            badge.setWidthPercentage(60);
            badge.setHorizontalAlignment(Element.ALIGN_CENTER);
            PdfPCell bc = new PdfPCell(new Paragraph(eff + "%", bodyB(effColor)));
            bc.setBackgroundColor(effBg);
            bc.setBorder(Rectangle.NO_BORDER);
            bc.setHorizontalAlignment(Element.ALIGN_CENTER);
            bc.setPaddingTop(2);
            bc.setPaddingBottom(2);
            bc.setPaddingLeft(4);
            bc.setPaddingRight(4);
            badge.addCell(bc);
            effCell.addElement(badge);
            t.addCell(effCell);
        }
        doc.add(t);
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  MANAGER REPORT
    // ─────────────────────────────────────────────────────────────────────────
    public byte[] exportManagerReport(ReportingDataDto data) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document doc = new Document(PageSize.A4, 36, 36, 36, 50);
            PdfWriter writer = PdfWriter.getInstance(doc, out);
            writer.setPageEvent(new FooterEvent("Rapport Manager"));
            doc.open();

            addHeader(doc, "RAPPORT MANAGER", "Validation et supervision des projets");

            List<ReportingDataDto.ProjectSummaryDto> mgrProj   = data.getManagerProjects();
            List<ReportingDataDto.ProjectReviewDto>  pending   = data.getPendingProjects();
            List<ReportingDataDto.ProjectReviewDto>  processed = data.getProcessedProjects();

            int totalP = mgrProj != null ? mgrProj.size() : 0;
            int pendingC = pending != null ? pending.size() : 0;
            int validatedC = 0, rejectedC = 0;
            if (processed != null) {
                for (var p : processed) {
                    if ("PRE_VALIDE".equals(p.getStatus())) validatedC++;
                    else if ("REJETE".equals(p.getStatus())) rejectedC++;
                }
            }
            double validRate = totalP > 0 ? (validatedC * 100.0 / totalP) : 0;

            PdfPTable kpi = new PdfPTable(4);
            kpi.setWidthPercentage(100);
            kpi.setSpacingAfter(12);
            addKpiCard(kpi, "À VALIDER", String.valueOf(pendingC),   "en attente",  WARNING, WARNING_LIGHT);
            addKpiCard(kpi, "VALIDÉS",   String.valueOf(validatedC), "pré-validés", SUCCESS, SUCCESS_LIGHT);
            addKpiCard(kpi, "REJETÉS",   String.valueOf(rejectedC),  "refusés",     DANGER,  DANGER_LIGHT);
            addKpiCard(kpi, "TOTAL",     String.valueOf(totalP),     "tous projets",PRIMARY, PRIMARY_LIGHT);
            doc.add(kpi);

            addSectionDivider(doc, "RÉPARTITION DES PROJETS");
            addProjectStatus(doc, mgrProj);

            addSectionDivider(doc, "TAUX DE VALIDATION");
            addProgressBarsTable(doc,
                    new String[]{"Validés", "Rejetés"},
                    new long[]{validatedC, rejectedC},
                    totalP > 0 ? totalP : 1,
                    new Color[]{SUCCESS, DANGER});
            addRateChip(doc, String.format("Taux de validation : %.0f%%", validRate), SUCCESS, SUCCESS_LIGHT);

            addSectionDivider(doc, "PROJETS EN ATTENTE DE VALIDATION");
            addPendingProjectsList(doc, pending);

            addSectionDivider(doc, "HISTORIQUE DES DÉCISIONS");
            addProcessedTable(doc, processed);

            doc.close();
            return out.toByteArray();
        } catch (DocumentException | IOException e) {
            throw new RuntimeException("Erreur génération PDF manager", e);
        }
    }

    private void addPendingProjectsList(Document doc, List<ReportingDataDto.ProjectReviewDto> list) throws DocumentException {
        if (list == null || list.isEmpty()) {
            doc.add(emptyState("Aucun projet en attente"));
            return;
        }
        PdfPTable t = new PdfPTable(new float[]{65f, 35f});
        t.setWidthPercentage(96);
        t.setSpacingAfter(10);
        addTableHeaderColored(t, new String[]{"Projet", "Client"}, WARNING);

        boolean alt = false;
        for (var p : list.stream().limit(10).toList()) {
            Color bg = alt ? WARNING_LIGHT : WHITE;
            alt = !alt;
            addCell(t, p.getName(), body(TEXT_DARK), bg, Element.ALIGN_LEFT);
            addCell(t, p.getClient(), small(TEXT_MUTED), bg, Element.ALIGN_LEFT);
        }
        doc.add(t);
    }

    private void addProcessedTable(Document doc, List<ReportingDataDto.ProjectReviewDto> list) throws DocumentException {
        if (list == null || list.isEmpty()) {
            doc.add(emptyState("Aucun historique"));
            return;
        }
        PdfPTable t = new PdfPTable(new float[]{42f, 30f, 28f});
        t.setWidthPercentage(100);
        t.setSpacingAfter(10);
        addTableHeader(t, new String[]{"Projet", "Client", "Décision"});

        boolean alt = false;
        for (var p : list.stream().limit(15).toList()) {
            Color bg = alt ? SURFACE : WHITE;
            alt = !alt;
            boolean ok = "PRE_VALIDE".equals(p.getStatus());
            addCell(t, p.getName(), body(TEXT_DARK), bg, Element.ALIGN_LEFT);
            addCell(t, p.getClient(), small(TEXT_MUTED), bg, Element.ALIGN_LEFT);
            // Badge décision
            addCell(t, ok ? "✓  Validé" : "✗  Rejeté", bodyB(ok ? SUCCESS : DANGER), ok ? SUCCESS_LIGHT : DANGER_LIGHT, Element.ALIGN_CENTER);
        }
        doc.add(t);
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  MEMBRE ÉQUIPE REPORT
    // ─────────────────────────────────────────────────────────────────────────
    public byte[] exportMembreEquipeReport(ReportingDataDto data) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document doc = new Document(PageSize.A4, 36, 36, 36, 50);
            PdfWriter writer = PdfWriter.getInstance(doc, out);
            writer.setPageEvent(new FooterEvent("Rapport Membre Équipe"));
            doc.open();

            addHeader(doc, "RAPPORT MEMBRE ÉQUIPE", "Suivi personnel de mes tâches et performances");

            long total   = data.getPersonalTaskStats() != null ? data.getPersonalTaskStats().getTotalTasks() : 0;
            long done    = data.getPersonalTaskStats() != null ? data.getPersonalTaskStats().getCompletedTasks() : 0;
            long inProg  = data.getPersonalTaskStats() != null ? data.getPersonalTaskStats().getInProgressTasks() : 0;
            long pending = data.getPersonalTaskStats() != null ? data.getPersonalTaskStats().getPendingTasks() : 0;
            long late    = data.getPersonalTaskStats() != null ? data.getPersonalTaskStats().getLateTasks() : 0;

            PdfPTable kpi = new PdfPTable(5);
            kpi.setWidthPercentage(100);
            kpi.setSpacingAfter(12);
            addKpiCard(kpi, "TOTAL",     String.valueOf(total),   "mes tâches", PRIMARY, PRIMARY_LIGHT);
            addKpiCard(kpi, "TERMINÉES", String.valueOf(done),    "achevées",   SUCCESS, SUCCESS_LIGHT);
            addKpiCard(kpi, "EN COURS",  String.valueOf(inProg),  "actives",    WARNING, WARNING_LIGHT);
            addKpiCard(kpi, "À FAIRE",   String.valueOf(pending), "planifiées", INFO,    INFO_LIGHT);
            addKpiCard(kpi, "RETARD",    String.valueOf(late),    "en retard",  DANGER,  DANGER_LIGHT);
            doc.add(kpi);

            addSectionDivider(doc, "TAUX D'ACHÈVEMENT");
            double rate = data.getPersonalCompletionRate();
            addProgressBarsTable(doc,
                    new String[]{"Terminées", "En cours", "À faire", "En retard"},
                    new long[]{done, inProg, pending, late},
                    total > 0 ? total : 1,
                    new Color[]{SUCCESS, INFO, TEXT_MUTED, DANGER});
            addRateChip(doc, "Progression globale : " + String.format("%.0f%%", rate), SUCCESS, SUCCESS_LIGHT);

            addSectionDivider(doc, "RÉPARTITION PAR PRIORITÉ");
            long high    = data.getPriorityDistribution() != null ? data.getPriorityDistribution().getHighPriorityCount()   : 0;
            long medium  = data.getPriorityDistribution() != null ? data.getPriorityDistribution().getMediumPriorityCount() : 0;
            long low     = data.getPriorityDistribution() != null ? data.getPriorityDistribution().getLowPriorityCount()    : 0;
            long prioTotal = high + medium + low;
            if (prioTotal > 0) {
                addProgressBarsTable(doc,
                        new String[]{"Haute", "Moyenne", "Basse"},
                        new long[]{high, medium, low},
                        prioTotal,
                        new Color[]{DANGER, WARNING, INFO});
            }

            addSectionDivider(doc, "ÉVOLUTION HEBDOMADAIRE");
            addWeeklyTable(doc, data.getWeeklyEvolution());

            addSectionDivider(doc, "TÂCHES PRIORITAIRES (HAUTE)");
            addHighPriorityTasksTable(doc, data.getCurrentTasks());

            addSectionDivider(doc, "TÂCHES À FAIRE");
            addTodoTasksTable(doc, data.getCurrentTasks());

            doc.close();
            return out.toByteArray();
        } catch (DocumentException | IOException e) {
            throw new RuntimeException("Erreur génération PDF membre équipe", e);
        }
    }

    private void addWeeklyTable(Document doc, List<ReportingDataDto.WeeklyEvolutionDto> weekly) throws DocumentException {
        if (weekly == null || weekly.isEmpty()) {
            doc.add(emptyState("Aucune donnée hebdomadaire"));
            return;
        }
        PdfPTable t = new PdfPTable(new float[]{28f, 18f, 18f, 36f});
        t.setWidthPercentage(86);
        t.setSpacingAfter(10);
        addTableHeader(t, new String[]{"Semaine", "Terminées", "Total", "Taux"});

        boolean alt = false;
        for (var w : weekly) {
            Color bg = alt ? SURFACE : WHITE;
            alt = !alt;
            double cr = w.getCompletionRate();
            Color rateColor = cr >= 70 ? SUCCESS : (cr >= 40 ? WARNING : DANGER);
            addCell(t, w.getWeek(), body(TEXT_DARK), bg, Element.ALIGN_LEFT);
            addCell(t, String.valueOf(w.getCompleted()), bodyB(SUCCESS), bg, Element.ALIGN_CENTER);
            addCell(t, String.valueOf(w.getTotal()), body(TEXT_MUTED), bg, Element.ALIGN_CENTER);
            // Badge taux
            addCell(t, String.format("%.0f%%", cr), bodyB(rateColor), cr >= 70 ? SUCCESS_LIGHT : (cr >= 40 ? WARNING_LIGHT : DANGER_LIGHT), Element.ALIGN_CENTER);
        }
        doc.add(t);
    }

    private void addHighPriorityTasksTable(Document doc, List<ReportingDataDto.TaskSummaryDto> tasks) throws DocumentException {
        List<ReportingDataDto.TaskSummaryDto> filtered = tasks != null
                ? tasks.stream().filter(t -> "HAUTE".equals(t.getPriority())).limit(10).toList()
                : List.of();
        if (filtered.isEmpty()) {
            doc.add(emptyState("Aucune tâche prioritaire"));
            return;
        }
        PdfPTable t = new PdfPTable(new float[]{40f, 34f, 26f});
        t.setWidthPercentage(100);
        t.setSpacingAfter(10);
        addTableHeaderColored(t, new String[]{"Tâche", "Projet", "Échéance"}, DANGER);

        boolean alt = false;
        for (var task : filtered) {
            Color bg = alt ? new Color(255, 245, 245) : WHITE;
            alt = !alt;
            addCell(t, task.getTitle(), body(TEXT_DARK), bg, Element.ALIGN_LEFT);
            addCell(t, task.getProjectName() != null ? task.getProjectName() : "–", small(TEXT_MUTED), bg, Element.ALIGN_LEFT);
            addCell(t, task.getEstimatedEndDate() != null ? task.getEstimatedEndDate() : "Non définie", smallB(DANGER), bg, Element.ALIGN_CENTER);
        }
        doc.add(t);
    }

    private void addTodoTasksTable(Document doc, List<ReportingDataDto.TaskSummaryDto> tasks) throws DocumentException {
        List<ReportingDataDto.TaskSummaryDto> filtered = tasks != null
                ? tasks.stream()
                .filter(t -> "A_faire".equals(t.getStatus()) || "PENDING".equals(t.getStatus()))
                .limit(10).toList()
                : List.of();
        if (filtered.isEmpty()) {
            doc.add(emptyState("Aucune tâche à faire"));
            return;
        }
        PdfPTable t = new PdfPTable(new float[]{35f, 30f, 17f, 18f});
        t.setWidthPercentage(100);
        t.setSpacingAfter(10);
        addTableHeaderColored(t, new String[]{"Tâche", "Projet", "Priorité", "Échéance"}, INFO);

        boolean alt = false;
        for (var task : filtered) {
            Color bg = alt ? SURFACE : WHITE;
            alt = !alt;
            boolean isHigh = "HAUTE".equals(task.getPriority());
            boolean isMed  = "MOYENNE".equals(task.getPriority());
            Color pc  = isHigh ? DANGER : isMed ? WARNING : INFO;
            Color pcBg= isHigh ? DANGER_LIGHT : isMed ? WARNING_LIGHT : INFO_LIGHT;
            String pl = isHigh ? "Haute" : isMed ? "Moyenne" : "Basse";

            addCell(t, task.getTitle(), body(TEXT_DARK), bg, Element.ALIGN_LEFT);
            addCell(t, task.getProjectName() != null ? task.getProjectName() : "–", small(TEXT_MUTED), bg, Element.ALIGN_LEFT);
            addCell(t, pl, bodyB(pc), pcBg, Element.ALIGN_CENTER);
            addCell(t, task.getEstimatedEndDate() != null ? task.getEstimatedEndDate() : "–", small(TEXT_MUTED), bg, Element.ALIGN_CENTER);
        }
        doc.add(t);
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  RESPONSABLE CONTRAT REPORT
    // ─────────────────────────────────────────────────────────────────────────
    public byte[] exportResponsableContratReport(ReportingDataDto data) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document doc = new Document(PageSize.A4, 36, 36, 36, 50);
            PdfWriter writer = PdfWriter.getInstance(doc, out);
            writer.setPageEvent(new FooterEvent("Rapport Responsable Contrat"));
            doc.open();

            addHeader(doc, "RAPPORT RESPONSABLE CONTRAT", "Gestion contractuelle & facturation");
            addRcKpiCards(doc, data);

            addSectionDivider(doc, "RÉPARTITION DES PROFILS");
            addDistributionTable(doc, buildProfileRows(data), new String[]{"Profil", "Nb", "Répartition"});

            addSectionDivider(doc, "PROJETS PAR MOIS");
            addRcProjectsByMonth(doc, data);

            addSectionDivider(doc, "TOP CLIENTS PAR CA");
            addRcTopClients(doc, data);

            addSectionDivider(doc, "SYNTHÈSE CONTRACTUELLE");
            addRcSynthesis(doc, data);

            doc.close();
            return out.toByteArray();
        } catch (DocumentException | IOException e) {
            throw new RuntimeException("Erreur génération PDF responsable contrat", e);
        }
    }

    private void addRcKpiCards(Document doc, ReportingDataDto data) throws DocumentException {
        long totalP = data.getAllProjects() != null ? data.getAllProjects().size() : 0;
        long enVal = 0, preVal = 0, rej = 0;
        if (data.getAllProjects() != null) {
            for (var p : data.getAllProjects()) {
                if      ("EN_VALIDATION".equals(p.getStatus())) enVal++;
                else if ("PRE_VALIDE".equals(p.getStatus()))    preVal++;
                else if ("REJETE".equals(p.getStatus()))        rej++;
            }
        }
        long   clients = data.getActiveClientsCount();
        double totalHT = data.getBillingStats() != null ? data.getBillingStats().getTotalHT().doubleValue() : 0;

        PdfPTable r1 = new PdfPTable(3);
        r1.setWidthPercentage(100);
        r1.setSpacingAfter(4);
        addKpiCard(r1, "PROJETS",       String.valueOf(totalP), "tous statuts",      PRIMARY, PRIMARY_LIGHT);
        addKpiCard(r1, "EN VALIDATION", String.valueOf(enVal),  "en cours d'examen", WARNING, WARNING_LIGHT);
        addKpiCard(r1, "PRÉ-VALIDÉS",   String.valueOf(preVal), "approuvés",         SUCCESS, SUCCESS_LIGHT);

        PdfPTable r2 = new PdfPTable(3);
        r2.setWidthPercentage(100);
        r2.setSpacingAfter(12);
        addKpiCard(r2, "REJETÉS",        String.valueOf(rej),  "refusés",             DANGER,  DANGER_LIGHT);
        addKpiCard(r2, "CLIENTS ACTIFS", String.valueOf(clients), "entreprises",       INFO,    INFO_LIGHT);
        addKpiCard(r2, "CA HT",          String.format("%.0f MAD", totalHT), "hors taxes", SUCCESS, SUCCESS_LIGHT);

        doc.add(r1);
        doc.add(r2);
    }

    private void addRcProjectsByMonth(Document doc, ReportingDataDto data) throws DocumentException {
        List<ReportingDataDto.MonthlyProjectDto> monthly = data.getProjectsByMonth();
        if (monthly == null || monthly.isEmpty()) {
            doc.add(emptyState("Aucune donnée mensuelle"));
            return;
        }
        long max = monthly.stream().mapToLong(ReportingDataDto.MonthlyProjectDto::getCount).max().orElse(1);

        PdfPTable t = new PdfPTable(new float[]{28f, 10f, 62f});
        t.setWidthPercentage(84);
        t.setSpacingAfter(12);
        addTableHeader(t, new String[]{"Mois", "Nb", "Volume"});

        boolean alt = false;
        for (var m : monthly) {
            Color bg = alt ? SURFACE : WHITE;
            alt = !alt;
            int filled = (int) Math.round(m.getCount() * 52.0 / max);
            addCell(t, m.getMonth(), body(TEXT_DARK), bg, Element.ALIGN_LEFT);
            addCell(t, String.valueOf(m.getCount()), bodyB(PRIMARY), bg, Element.ALIGN_CENTER);
            addBarCell(t, filled, 52, PRIMARY, bg);
        }
        doc.add(t);
    }

    private void addRcTopClients(Document doc, ReportingDataDto data) throws DocumentException {
        List<ReportingDataDto.ClientRevenueDto> clients = data.getTopClientsByRevenue();
        if (clients == null || clients.isEmpty()) {
            doc.add(emptyState("Aucun client"));
            return;
        }
        double maxHT = clients.stream()
                .mapToDouble(c -> c.getTotalHT() != null ? c.getTotalHT().doubleValue() : 0)
                .max().orElse(1);

        PdfPTable t = new PdfPTable(new float[]{7f, 40f, 53f});
        t.setWidthPercentage(96);
        t.setSpacingAfter(12);
        addTableHeaderColored(t, new String[]{"#", "Client", "CA HT (MAD)"}, SUCCESS);

        String[] rankLabels = {"1er", "2e", "3e"};
        Color[]  medals     = {new Color(202,138,4), new Color(148,163,184), new Color(180,83,9)};
        boolean alt = false;
        for (int i = 0; i < Math.min(8, clients.size()); i++) {
            Color bg = alt ? SURFACE : WHITE;
            alt = !alt;
            var c = clients.get(i);
            double ht = c.getTotalHT() != null ? c.getTotalHT().doubleValue() : 0;
            int    filled = (int) Math.round(ht * 52.0 / maxHT);
            String name   = c.getClient().length() > 28 ? c.getClient().substring(0, 26) + "…" : c.getClient();
            String rank   = i < 3 ? rankLabels[i] : String.valueOf(i + 1);
            Color  rc     = i < 3 ? medals[i] : TEXT_MUTED;

            addCell(t, rank, bodyB(rc), bg, Element.ALIGN_CENTER);
            addCell(t, name, body(TEXT_DARK), bg, Element.ALIGN_LEFT);
            addBarCellWithCount(t, filled, 52, (long) ht, SUCCESS, bg);
        }
        doc.add(t);
    }

    private void addRcSynthesis(Document doc, ReportingDataDto data) throws DocumentException {
        long totalP = data.getAllProjects() != null ? data.getAllProjects().size() : 0;
        long preVal = 0;
        if (data.getAllProjects() != null)
            for (var p : data.getAllProjects())
                if ("PRE_VALIDE".equals(p.getStatus())) preVal++;

        double vRate   = totalP > 0 ? (preVal * 100.0 / totalP) : 0;
        double totalHT = data.getBillingStats() != null ? data.getBillingStats().getTotalHT().doubleValue() : 0;
        long   clients = data.getActiveClientsCount();

        PdfPTable t = new PdfPTable(3);
        t.setWidthPercentage(100);
        t.setSpacingAfter(10);

        addSynthCard(t, "TAUX DE VALIDATION",
                String.format("%.1f%%", vRate),
                preVal + " / " + totalP + " projets", SUCCESS);
        addSynthCard(t, "CHIFFRE D'AFFAIRES",
                String.format("%.0f MAD", totalHT),
                "hors taxes (HT)", SUCCESS);
        addSynthCard(t, "CLIENTS ACTIFS",
                String.valueOf(clients),
                "entreprises partenaires", INFO);

        doc.add(t);
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  HELPERS TABLEAUX
    // ─────────────────────────────────────────────────────────────────────────

    /** État vide stylisé */
    private Paragraph emptyState(String msg) {
        Paragraph p = new Paragraph(msg, small(TEXT_MUTED));
        p.setAlignment(Element.ALIGN_CENTER);
        p.setSpacingBefore(4);
        p.setSpacingAfter(10);
        return p;
    }

    private void addTableHeader(PdfPTable t, String[] labels) {
        addTableHeaderColored(t, labels, PRIMARY);
    }

    private void addTableHeaderColored(PdfPTable t, String[] labels, Color bg) {
        for (String label : labels) {
            PdfPCell cell = new PdfPCell(new Paragraph(label, bodyB(WHITE)));
            cell.setBackgroundColor(bg);
            cell.setBorder(Rectangle.NO_BORDER);
            cell.setPaddingTop(7);
            cell.setPaddingBottom(7);
            cell.setPaddingLeft(7);
            cell.setPaddingRight(7);
            t.addCell(cell);
        }
    }

    private void addCell(PdfPTable t, String text, Font font, Color bg, int align) {
        PdfPCell cell = new PdfPCell(new Paragraph(text != null ? text : "", font));
        cell.setBackgroundColor(bg);
        cell.setBorder(Rectangle.BOX);
        cell.setBorderColor(BORDER);
        cell.setBorderWidth(0.5f);
        cell.setHorizontalAlignment(align);
        cell.setPaddingTop(5);
        cell.setPaddingBottom(5);
        cell.setPaddingLeft(7);
        cell.setPaddingRight(7);
        t.addCell(cell);
    }

    // Barre utilisant des blocs Unicode de largeur fine pour un rendu plus propre
    private String buildBar(int filled, int total) {
        // Bloc plein + bloc vide (léger)
        return "█".repeat(Math.max(0, filled)) + "▒".repeat(Math.max(0, total - filled));
    }

    private void addBarCell(PdfPTable t, int filled, int total, Color color, Color bg) {
        String bar = buildBar(filled, total);
        Font barFont = new Font(Font.HELVETICA, 6, Font.NORMAL, color);
        PdfPCell cell = new PdfPCell(new Paragraph(bar, barFont));
        cell.setBackgroundColor(bg);
        cell.setBorder(Rectangle.BOX);
        cell.setBorderColor(BORDER);
        cell.setBorderWidth(0.5f);
        cell.setPaddingTop(6);
        cell.setPaddingBottom(4);
        cell.setPaddingLeft(6);
        cell.setPaddingRight(6);
        t.addCell(cell);
    }

    private void addBarCellWithPct(PdfPTable t, int filled, int total, float pct, Color color, Color bg) {
        String bar = buildBar(filled, total) + "  " + String.format("%.1f%%", pct);
        Font barFont = new Font(Font.HELVETICA, 6, Font.NORMAL, color);
        PdfPCell cell = new PdfPCell(new Paragraph(bar, barFont));
        cell.setBackgroundColor(bg);
        cell.setBorder(Rectangle.BOX);
        cell.setBorderColor(BORDER);
        cell.setBorderWidth(0.5f);
        cell.setPaddingTop(6);
        cell.setPaddingBottom(4);
        cell.setPaddingLeft(6);
        cell.setPaddingRight(6);
        t.addCell(cell);
    }

    private void addBarCellWithCount(PdfPTable t, int filled, int total, long count, Color color, Color bg) {
        String bar = buildBar(filled, total) + "  " + count;
        Font barFont = new Font(Font.HELVETICA, 6, Font.NORMAL, color);
        PdfPCell cell = new PdfPCell(new Paragraph(bar, barFont));
        cell.setBackgroundColor(bg);
        cell.setBorder(Rectangle.BOX);
        cell.setBorderColor(BORDER);
        cell.setBorderWidth(0.5f);
        cell.setPaddingTop(6);
        cell.setPaddingBottom(4);
        cell.setPaddingLeft(6);
        cell.setPaddingRight(6);
        t.addCell(cell);
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  FOOTER EVENT
    // ─────────────────────────────────────────────────────────────────────────
    private static class FooterEvent extends PdfPageEventHelper {
        private final String reportName;
        FooterEvent(String reportName) { this.reportName = reportName; }

        @Override
        public void onEndPage(PdfWriter writer, Document document) {
            try {
                PdfContentByte cb   = writer.getDirectContent();
                Rectangle      page = document.getPageSize();
                float y = document.bottomMargin() - 10;

                // Ligne de séparation fine
                cb.setColorStroke(new Color(226, 232, 240));
                cb.setLineWidth(0.5f);
                cb.moveTo(document.leftMargin(), y + 14);
                cb.lineTo(page.getWidth() - document.rightMargin(), y + 14);
                cb.stroke();

                com.lowagie.text.pdf.BaseFont bf =
                        com.lowagie.text.pdf.BaseFont.createFont(
                                com.lowagie.text.pdf.BaseFont.HELVETICA,
                                com.lowagie.text.pdf.BaseFont.WINANSI, false);

                // Texte gauche : nom du rapport
                cb.beginText();
                cb.setFontAndSize(bf, 7);
                cb.setColorFill(new Color(148, 163, 184));
                cb.setTextMatrix(document.leftMargin(), y);
                cb.showText("DXC Platform  ·  " + reportName
                        + "  ·  " + LocalDate.now().format(
                        DateTimeFormatter.ofPattern("dd/MM/yyyy")));
                cb.endText();

                // Numéro de page (droite)
                cb.beginText();
                cb.setFontAndSize(bf, 7);
                cb.setColorFill(new Color(148, 163, 184));
                cb.setTextMatrix(page.getWidth() - document.rightMargin() - 28, y);
                cb.showText("Page " + writer.getPageNumber());
                cb.endText();

            } catch (Exception ignored) {}
        }
    }
}
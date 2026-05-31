package com.dxc.dxc_platform.service;

import com.dxc.dxc_platform.dto.RiskPredictionResult;
import com.dxc.dxc_platform.entity.Project;
import org.springframework.stereotype.Service;

@Service
public class RiskAlertService {

    private final EmailService emailService;

    public RiskAlertService(EmailService emailService) {
        this.emailService = emailService;
    }

    public void sendRiskAlert(Project project, RiskPredictionResult result) {
        if (project.getChefProjet() == null
                || project.getChefProjet().getEmail() == null) {
            System.out.println("⚠️ Pas de chef de projet — alerte risque non envoyée");
            return;
        }

        String subject = "[Alerte Risque] " + project.getName()
                + " — Risque " + result.getLevel().name();

        String body = String.format("""
                Bonjour %s,

                Le projet "%s" (échéance : %s) présente un niveau de risque %s.

                Score de risque : %.0f%%
                Analyse IA : %s

                Veuillez prendre les mesures nécessaires.

                ---
                Cet email a été envoyé automatiquement.
                © DXC Technology - Plateforme de gestion de projets
                """,
                project.getChefProjet().getPrenom(),
                project.getName(),
                project.getEndDate(),
                result.getLevel().name(),
                result.getScore() * 100,
                result.getReason()
        );

        // sendSimpleEmail existe bien dans ton EmailService ✅
        emailService.sendSimpleEmail(
                project.getChefProjet().getEmail(),
                subject,
                body
        );

        System.out.println("✅ Alerte risque envoyée à "
                + project.getChefProjet().getEmail());
    }
}
package com.dxc.dxc_platform.service;

import com.dxc.dxc_platform.entity.Task;
import com.dxc.dxc_platform.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import jakarta.mail.internet.MimeMessage;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;

    @Value("${spring.mail.username}")
    private String fromEmail;

    /**
     * Envoyer un email simple en texte
     */
    public void sendSimpleEmail(String to, String subject, String content) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(to);
            message.setSubject(subject);
            message.setText(content);
            mailSender.send(message);
            log.info("Email envoyé à {}", to);
        } catch (Exception e) {
            log.error("Erreur lors de l'envoi d'email à {}: {}", to, e.getMessage());
        }
    }

    /**
     * Envoyer un email HTML
     */
    public void sendHtmlEmail(String to, String subject, String htmlContent) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);
            mailSender.send(message);
            log.info("Email HTML envoyé à {}", to);
        } catch (Exception e) {
            log.error("Erreur lors de l'envoi d'email HTML à {}: {}", to, e.getMessage());
        }
    }

    /**
     * Envoyer un email avec template Thymeleaf
     */
    public void sendTemplateEmail(String to, String subject, String templateName, Context context) {
        try {
            String htmlContent = templateEngine.process(templateName, context);
            sendHtmlEmail(to, subject, htmlContent);
        } catch (Exception e) {
            log.error("Erreur template email: {}", e.getMessage());
        }
    }

    // ================= NOTIFICATIONS SPÉCIFIQUES =================

    /**
     * Notification d'assignation de tâche
     */
    public void notifyTaskAssigned(Task task, User assignedTo, User assignedBy) {
        String subject = "📋 Nouvelle tâche assignée - " + task.getTitle();
        String content = String.format("""
            Bonjour %s,
            
            Une nouvelle tâche vous a été assignée :
            
            Tâche : %s
            Projet : %s
            Échéance : %s
            Priorité : %s
            Description : %s
            
            Assigné par : %s
            
            Connectez-vous à la plateforme pour plus de détails.
            
            ---
            Cet email a été envoyé automatiquement. Merci de ne pas y répondre.
            © DXC Technology - Plateforme de gestion de projets
            """,
                assignedTo.getPrenom(),
                task.getTitle(),
                task.getProject() != null ? task.getProject().getName() : "N/A",
                task.getEstimatedEndDate() != null ? task.getEstimatedEndDate().toString() : "Non définie",
                task.getPriority() != null ? task.getPriority().name() : "MOYENNE",
                task.getDescription() != null ? task.getDescription() : "Aucune description",
                assignedBy != null ? assignedBy.getFullName() : "Administrateur"
        );
        sendSimpleEmail(assignedTo.getEmail(), subject, content);
    }

    /**
     * Notification de validation de tâche
     */
    public void notifyTaskValidated(Task task, User member, String commentaire) {
        String subject = "✅ Tâche validée - " + task.getTitle();
        String content = String.format("""
            Bonjour %s,
            
            Félicitations ! Votre tâche a été validée par le chef de projet.
            
            Tâche : %s
            Projet : %s
            
            Commentaire : %s
            
            Bien continuer !
            
            ---
            Cet email a été envoyé automatiquement.
            © DXC Technology - Plateforme de gestion de projets
            """,
                member.getPrenom(),
                task.getTitle(),
                task.getProject() != null ? task.getProject().getName() : "N/A",
                commentaire != null ? commentaire : "Aucun commentaire"
        );
        sendSimpleEmail(member.getEmail(), subject, content);
    }

    /**
     * Notification de rejet de tâche
     */
    public void notifyTaskRejected(Task task, User member, String commentaire) {
        String subject = "❌ Tâche à reprendre - " + task.getTitle();
        String content = String.format("""
            Bonjour %s,
            
            Votre tâche a été rejetée par le chef de projet.
            
            Tâche : %s
            Projet : %s
            
            Motif du rejet : %s
            
            Veuillez la reprendre et la soumettre à nouveau une fois corrigée.
            
            ---
            Cet email a été envoyé automatiquement.
            © DXC Technology - Plateforme de gestion de projets
            """,
                member.getPrenom(),
                task.getTitle(),
                task.getProject() != null ? task.getProject().getName() : "N/A",
                commentaire != null ? commentaire : "Non spécifié"
        );
        sendSimpleEmail(member.getEmail(), subject, content);
    }

    /**
     * Notification de soumission de tâche pour validation (au chef de projet)
     */
    public void notifyTaskSubmittedForValidation(Task task, User member, User chefProjet) {
        String subject = "📋 Tâche à valider - " + task.getTitle();
        String content = String.format("""
            Bonjour Chef de projet,
            
            Le membre %s a soumis une tâche pour validation.
            
            Tâche : %s
            Projet : %s
            Assignée à : %s
            
            Connectez-vous à la plateforme pour valider ou rejeter cette tâche.
            
            ---
            Cet email a été envoyé automatiquement.
            © DXC Technology - Plateforme de gestion de projets
            """,
                member.getFullName(),
                task.getTitle(),
                task.getProject() != null ? task.getProject().getName() : "N/A",
                task.getAssignedTo() != null ? task.getAssignedTo().getFullName() : "Non assigné"
        );
        sendSimpleEmail(chefProjet.getEmail(), subject, content);
    }

    /**
     * Notification de création de tâche (confirmation au chef de projet)
     */
    public void notifyTaskCreated(Task task, User chefProjet) {
        String subject = "✅ Tâche créée avec succès - " + task.getTitle();
        String content = String.format("""
            Bonjour Chef de projet,
            
            La tâche suivante a été créée avec succès :
            
            Tâche : %s
            Projet : %s
            Assignée à : %s
            Échéance : %s
            Priorité : %s
            
            ---
            Cet email a été envoyé automatiquement.
            © DXC Technology - Plateforme de gestion de projets
            """,
                task.getTitle(),
                task.getProject() != null ? task.getProject().getName() : "N/A",
                task.getAssignedTo() != null ? task.getAssignedTo().getFullName() : "Non assigné",
                task.getEstimatedEndDate() != null ? task.getEstimatedEndDate().toString() : "Non définie",
                task.getPriority() != null ? task.getPriority().name() : "MOYENNE"
        );
        sendSimpleEmail(chefProjet.getEmail(), subject, content);
    }
    // ================= AJOUTEZ DANS EmailService.java =================

    /**
     * Notification de modification de tâche
     */
    public void notifyTaskUpdated(Task task, User updatedBy, User assignedTo) {
        String subject = "✏️ Tâche modifiée - " + task.getTitle();
        String content = String.format("""
        Bonjour %s,
        
        La tâche suivante a été modifiée par %s :
        
        Tâche : %s
        Projet : %s
        Nouvelle échéance : %s
        Nouvelle priorité : %s
        Nouvelle description : %s
        
        Connectez-vous à la plateforme pour voir les modifications.
        
        ---
        Cet email a été envoyé automatiquement. Merci de ne pas y répondre.
        © DXC Technology - Plateforme de gestion de projets
        """,
                assignedTo.getPrenom(),
                updatedBy.getFullName(),
                task.getTitle(),
                task.getProject() != null ? task.getProject().getName() : "N/A",
                task.getEstimatedEndDate() != null ? task.getEstimatedEndDate().toString() : "Non définie",
                task.getPriority() != null ? task.getPriority().name() : "MOYENNE",
                task.getDescription() != null ? task.getDescription() : "Aucune description"
        );
        sendSimpleEmail(assignedTo.getEmail(), subject, content);
    }

    /**
     * Notification de suppression de tâche
     */
    public void notifyTaskDeleted(Task task, User deletedBy, User assignedTo) {
        String subject = "🗑️ Tâche supprimée - " + task.getTitle();
        String content = String.format("""
        Bonjour %s,
        
        La tâche suivante a été supprimée par %s :
        
        Tâche : %s
        Projet : %s
        
        Cette tâche n'est plus disponible.
        
        ---
        Cet email a été envoyé automatiquement. Merci de ne pas y répondre.
        © DXC Technology - Plateforme de gestion de projets
        """,
                assignedTo.getPrenom(),
                deletedBy.getFullName(),
                task.getTitle(),
                task.getProject() != null ? task.getProject().getName() : "N/A"
        );
        sendSimpleEmail(assignedTo.getEmail(), subject, content);
    }
}
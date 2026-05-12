package com.dxc.dxc_platform.service;

import com.dxc.dxc_platform.entity.Project;
import com.dxc.dxc_platform.entity.Task;
import com.dxc.dxc_platform.entity.Team;
import com.dxc.dxc_platform.entity.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.time.LocalDate;

@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;

    public EmailService(JavaMailSender mailSender, TemplateEngine templateEngine) {
        this.mailSender = mailSender;
        this.templateEngine = templateEngine;
    }

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
        String commentaireText = (commentaire != null && !commentaire.trim().isEmpty())
                ? "Commentaire : " + commentaire
                : "";
        String content = String.format("""
            Bonjour %s,
            
            Félicitations ! Votre tâche a été validée par le chef de projet.
            
            Tâche : %s
            Projet : %s
            %s
            
            Bien continuer !
            
            ---
            Cet email a été envoyé automatiquement.
            © DXC Technology - Plateforme de gestion de projets
            """,
                member.getPrenom(),
                task.getTitle(),
                task.getProject() != null ? task.getProject().getName() : "N/A",
                commentaireText
        );
        sendSimpleEmail(member.getEmail(), subject, content);
    }

    /**
     * Notification de rejet de tâche
     */
    public void notifyTaskRejected(Task task, User member, String commentaire) {
        String subject = "❌ Tâche à reprendre - " + task.getTitle();
        String motifText = (commentaire != null && !commentaire.trim().isEmpty())
                ? "Motif du rejet : " + commentaire
                : "Motif du rejet : Non spécifié";
        String content = String.format("""
            Bonjour %s,
            
            Votre tâche a été rejetée par le chef de projet.
            
            Tâche : %s
            Projet : %s
            
            %s
            
            Veuillez la reprendre et la soumettre à nouveau une fois corrigée.
            
            ---
            Cet email a été envoyé automatiquement.
            © DXC Technology - Plateforme de gestion de projets
            """,
                member.getPrenom(),
                task.getTitle(),
                task.getProject() != null ? task.getProject().getName() : "N/A",
                motifText
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

    /**
     * Notification d'assignation à une équipe
     */
    public void notifyTeamAssigned(User member, Team team, User assignedBy) {
        String subject = "👥 Vous avez été ajouté à l'équipe " + team.getName();
        String content = String.format("""
            Bonjour %s,

            Vous avez été affecté(e) à l'équipe "%s".

            Chef de projet : %s
            Équipe : %s

            Connectez-vous à la plateforme pour consulter vos projets et tâches.

            ---
            Cet email a été envoyé automatiquement. Merci de ne pas y répondre.
            © DXC Technology - Plateforme de gestion de projets
            """,
                member.getPrenom(),
                team.getName(),
                assignedBy != null ? assignedBy.getFullName() : "Votre chef de projet",
                team.getName()
        );
        sendSimpleEmail(member.getEmail(), subject, content);
    }

    /**
     * Notification d'assignation d'un projet pour l'équipe
     */
    public void notifyTeamAssignedToProject(Team team, Project project, User assignedBy) {
        if (team.getMembers() == null || team.getMembers().isEmpty()) {
            return;
        }

        String subject = "🧩 Votre équipe a été affectée au projet : " + project.getName();

        for (User member : team.getMembers()) {
            String content = String.format("""
                Bonjour %s,

                Votre équipe "%s" a été affectée au projet suivant :

                Projet : %s
                Client : %s
                Statut : %s
                Description : %s
                Chef de projet : %s

                Connectez-vous à la plateforme pour consulter les tâches et les détails du projet.

                ---
                Cet email a été envoyé automatiquement. Merci de ne pas y répondre.
                © DXC Technology - Plateforme de gestion de projets
                """,
                    member.getPrenom(),
                    team.getName(),
                    project.getName(),
                    project.getClient() != null ? project.getClient() : "N/A",
                    project.getStatus() != null ? project.getStatus().name() : "N/A",
                    project.getDescription() != null ? project.getDescription() : "Aucune description",
                    assignedBy != null ? assignedBy.getFullName() : "Votre chef de projet"
            );
            sendSimpleEmail(member.getEmail(), subject, content);
        }
    }

    /**
     * Notification lorsqu'un chef de projet est affecté à un projet par le manager
     */
    public void notifyChefProjetAssigned(Project project, User chefProjet, User manager, String commentaire) {
        String subject = "👨‍💼 Vous êtes affecté(e) au projet : " + project.getName();
        String commentaireText = (commentaire != null && !commentaire.isBlank())
                ? "Commentaire du manager : " + commentaire
                : "";
        String content = String.format("""
            Bonjour %s,

            Vous avez été affecté(e) en tant que chef de projet pour le projet suivant :

            Projet : %s
            Client : %s
            Statut : %s
            Description : %s
            Manager : %s
            %s

            Connectez-vous à la plateforme pour consulter le projet et démarrer l'organisation.

            ---
            Cet email a été envoyé automatiquement. Merci de ne pas y répondre.
            © DXC Technology - Plateforme de gestion de projets
            """,
                chefProjet.getPrenom(),
                project.getName(),
                project.getClient() != null ? project.getClient() : "N/A",
                project.getStatus() != null ? project.getStatus().name() : "N/A",
                project.getDescription() != null ? project.getDescription() : "Aucune description",
                manager != null ? manager.getFullName() : "Votre manager",
                commentaireText
        );
        sendSimpleEmail(chefProjet.getEmail(), subject, content);
    }

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

    // ================= NOTIFICATIONS MOT DE PASSE =================

    /**
     * Notification de mot de passe temporaire (envoyée par l'admin)
     */
    public void notifyTemporaryPassword(User user, String tempPassword) {
        String subject = "Réinitialisation du mot de passe - DXC Platform";

        String content = String.format("""
            Bonjour %s,

            Votre mot de passe temporaire est :

            %s

            Email de connexion : %s

            Connectez-vous avec ce mot de passe temporaire, puis changez-le dans votre profil.

            ---
            Cet email a été envoyé automatiquement.
            DXC Platform
            """,
                user.getPrenom(),
                tempPassword,
                user.getEmail()
        );

        sendSimpleEmail(user.getEmail(), subject, content);
    }

    /**
     * Notification de confirmation après changement de mot de passe réussi par l'utilisateur
     */
    public void notifyPasswordChanged(User user) {
        String subject = "🔑 Votre mot de passe a été modifié - DXC Platform";

        String now = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"));

        String content = String.format("""
            Bonjour %s,

            Votre mot de passe a été modifié avec succès.

            ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
            📧 Compte  : %s
            ✅ Statut  : Mot de passe mis à jour
            ⏰ Date    : %s
            ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

            Si vous n'êtes pas à l'origine de cette modification, contactez immédiatement votre administrateur.

            ---
            Cet email a été envoyé automatiquement pour des raisons de sécurité.
            © DXC Technology - Plateforme de gestion de projets
            """,
                user.getPrenom(),
                user.getEmail(),
                now
        );

        sendSimpleEmail(user.getEmail(), subject, content);
        log.info("Notification de changement de mot de passe envoyée à {}", user.getEmail());
    }

    // ================= NOUVELLES NOTIFICATIONS ADMIN =================

    /**
     * Notification à un admin spécifique quand un compte est désactivé
     */
    public void notifyAdminAccountDisabled(User disabledUser, String adminEmail, String actionByEmail) {
        String subject = "🔒 Compte utilisateur désactivé - " + disabledUser.getEmail();

        String content = String.format("""
            Bonjour Admin,
            
            Le compte utilisateur suivant a été désactivé par %s :
            
            ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
            📧 Email : %s
            👤 Nom   : %s %s
            🎭 Rôles : %s
            🏷️ Statut : DÉSACTIVÉ
            ⏰ Date   : %s
            ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
            
            Pour réactiver ce compte, connectez-vous à la plateforme et utilisez la fonction "Activer".
            
            ---
            Cet email a été envoyé automatiquement.
            © DXC Technology - Plateforme de gestion de projets
            """,
                actionByEmail,
                disabledUser.getEmail(),
                disabledUser.getPrenom() != null ? disabledUser.getPrenom() : "",
                disabledUser.getNom() != null ? disabledUser.getNom() : "",
                disabledUser.getRoles().stream()
                        .map(role -> role.getNom())
                        .reduce((a, b) -> a + ", " + b)
                        .orElse("Aucun"),
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"))
        );

        sendSimpleEmail(adminEmail, subject, content);
    }

    /**
     * Notification à l'utilisateur que son compte est verrouillé avec nouveau mot de passe
     */
    public void notifyUserAccountLocked(User lockedUser, String tempPassword) {
        String subject = "🔐 Votre compte a été verrouillé - Nouveau mot de passe temporaire";

        String content = String.format("""
        Bonjour %s,
        
        Votre compte a été automatiquement verrouillé après 3 tentatives de connexion échouées.
        
        🔑 Votre nouveau mot de passe temporaire est : %s
        
        Pour vous connecter :
        1. Utilisez votre email habituel : %s
        2. Utilisez ce mot de passe temporaire
        3. Vous devrez changer votre mot de passe à la première connexion
        
        Si vous n'êtes pas à l'origine de ces tentatives, veuillez contacter immédiatement votre administrateur.
        
        ---
        Cet email a été envoyé automatiquement pour des raisons de sécurité.
        © DXC Technology - Plateforme de gestion de projets
        """,
                lockedUser.getPrenom(),
                tempPassword,
                lockedUser.getEmail()
        );

        sendSimpleEmail(lockedUser.getEmail(), subject, content);
    }

    /**
     * Notification à un admin spécifique quand un compte est réactivé
     */
    public void notifyAdminAccountEnabled(User enabledUser, String adminEmail, String actionByEmail) {
        String subject = "✅ Compte utilisateur réactivé - " + enabledUser.getEmail();

        String content = String.format("""
            Bonjour Admin,
            
            Le compte utilisateur suivant a été réactivé par %s :
            
            ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
            📧 Email : %s
            👤 Nom   : %s %s
            🎭 Rôles : %s
            🏷️ Statut : ACTIF
            ⏰ Date   : %s
            ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
            
            ---
            Cet email a été envoyé automatiquement.
            © DXC Technology - Plateforme de gestion de projets
            """,
                actionByEmail,
                enabledUser.getEmail(),
                enabledUser.getPrenom() != null ? enabledUser.getPrenom() : "",
                enabledUser.getNom() != null ? enabledUser.getNom() : "",
                enabledUser.getRoles().stream()
                        .map(role -> role.getNom())
                        .reduce((a, b) -> a + ", " + b)
                        .orElse("Aucun"),
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"))
        );

        sendSimpleEmail(adminEmail, subject, content);
    }

    /**
     * Notification à TOUS les admins quand un compte est désactivé
     */
    public void notifyAllAdminsAccountDisabled(User disabledUser, List<User> admins, String actionByEmail) {
        if (admins == null || admins.isEmpty()) {
            log.warn("Aucun admin trouvé pour la notification de désactivation");
            return;
        }

        for (User admin : admins) {
            notifyAdminAccountDisabled(disabledUser, admin.getEmail(), actionByEmail);
            log.info("Email de désactivation envoyé à l'admin: {}", admin.getEmail());
        }
    }

    /**
     * Notification à TOUS les admins quand un compte est réactivé
     */
    public void notifyAllAdminsAccountEnabled(User enabledUser, List<User> admins, String actionByEmail) {
        if (admins == null || admins.isEmpty()) {
            log.warn("Aucun admin trouvé pour la notification de réactivation");
            return;
        }

        for (User admin : admins) {
            notifyAdminAccountEnabled(enabledUser, admin.getEmail(), actionByEmail);
            log.info("Email de réactivation envoyé à l'admin: {}", admin.getEmail());
        }
    }

    /**
     * Notification quand quelqu'un reçoit un nouveau message
     */
    public void notifyNewMessageReceived(User sender, User receiver) {
        String subject = "💬 Nouveau message sur DXC Platform";

        String content = String.format("""
        Bonjour %s,
        
        %s %s vous a envoyé un message sur la plateforme DXC.
        
        ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
        📨 De : %s %s
        📅 Reçu le : %s
        🔔 Statut : Non lu
        ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
        
        Pour consulter votre message, connectez-vous à la plateforme et rendez-vous dans votre messagerie.
        
        ---
        Cet email a été envoyé automatiquement. Merci de ne pas y répondre.
        © DXC Technology - Plateforme de gestion de projets
        """,
                receiver.getPrenom(),
                sender.getPrenom(),
                sender.getNom(),
                sender.getPrenom(),
                sender.getNom(),
                java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"))
        );

        sendSimpleEmail(receiver.getEmail(), subject, content);
    }

    /**
     * Notification quand quelqu'un reçoit un fichier
     */
    public void notifyNewFileReceived(User sender, User receiver, String fileName) {
        String subject = "📎 Nouveau fichier reçu sur DXC Platform";

        String content = String.format("""
        Bonjour %s,
        
        %s %s vous a envoyé un fichier sur la plateforme DXC.
        
        ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
        📨 De : %s %s
        📎 Fichier : %s
        📅 Reçu le : %s
        🔔 Statut : Non lu
        ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
        
        Pour consulter ce fichier, connectez-vous à la plateforme et rendez-vous dans votre messagerie.
                
        ---
        Cet email a été envoyé automatiquement. Merci de ne pas y répondre.
        © DXC Technology - Plateforme de gestion de projets
        """,
                receiver.getPrenom(),
                sender.getPrenom(),
                sender.getNom(),
                sender.getPrenom(),
                sender.getNom(),
                fileName,
                java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"))
        );

        sendSimpleEmail(receiver.getEmail(), subject, content);
    }

    /**
     * Notification quand quelqu'un est invité à un événement du calendrier
     */
    public void notifyCalendarInvitation(String invitedUserEmail, String invitedUserName,
                                         String ownerName, String eventTitle,
                                         LocalDate eventDate, String startTime,
                                         String endTime, String description) {
        String subject = "📅 Invitation à un événement - " + eventTitle;

        String timeInfo = "";
        if (startTime != null && !startTime.isEmpty()) {
            timeInfo = " de " + startTime;
            if (endTime != null && !endTime.isEmpty()) {
                timeInfo += " à " + endTime;
            }
        }

        String content = String.format("""
        Bonjour %s,
        
        %s vous a invité à un événement sur la plateforme DXC.
        
        ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
        📅 Événement : %s
        📆 Date : %s%s
        📝 Description : %s
        👤 Organisé par : %s
        ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
        
        Pour consulter cet événement, connectez-vous à la plateforme et rendez-vous dans le calendrier partagé.
        
        ---
        Cet email a été envoyé automatiquement. Merci de ne pas y répondre.
        © DXC Technology - Plateforme de gestion de projets
        """,
                invitedUserName,
                ownerName,
                eventTitle,
                eventDate.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")),
                timeInfo,
                description != null && !description.isEmpty() ? description : "Aucune description",
                ownerName
        );

        sendSimpleEmail(invitedUserEmail, subject, content);
    }

    /**
     * Notification au manager quand il est assigné à un projet par le responsable de contrat
     */
    public void notifyManagerAssignedToProject(Project project, User manager, User responsableContrat) {
        String subject = "📁 Vous êtes assigné(e) au projet : " + project.getName();
        String content = String.format("""
        Bonjour %s,

        %s vous a assigné(e) en tant que manager au projet suivant :

        ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
        📁 Projet       : %s
        🏢 Client       : %s
        👤 Assigné par  : %s
        ⏰ Date         : %s
        ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

        Connectez-vous à la plateforme pour consulter le projet et commencer la gestion.

        ---
        Cet email a été envoyé automatiquement. Merci de ne pas y répondre.
        © DXC Technology - Plateforme de gestion de projets
        """,
                manager.getPrenom(),
                responsableContrat != null ? responsableContrat.getFullName() : "Le responsable de contrat",
                project.getName(),
                project.getClient() != null ? project.getClient() : "N/A",
                responsableContrat != null ? responsableContrat.getFullName() : "N/A",
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"))
        );
        sendSimpleEmail(manager.getEmail(), subject, content);
    }

    // ================= NOTIFICATIONS VALIDATION PROJET PAR MANAGER =================

    /**
     * Notification au RC (Responsable de Contrat) quand le manager VALIDE un projet.
     */
    public void notifyRcProjectValidatedByManager(Project project, User rc, User manager,
                                                  User chefProjet, String commentaire) {
        String subject = "✅ Projet validé par le manager - " + project.getName();

        String commentaireText = (commentaire != null && !commentaire.isBlank())
                ? "Commentaire du manager : " + commentaire
                : "Commentaire du manager : Aucun commentaire";

        String chefProjetInfo = (chefProjet != null)
                ? chefProjet.getFullName() + " (" + chefProjet.getEmail() + ")"
                : "Non encore assigné";

        String content = String.format("""
            Bonjour %s,

            Le manager a validé le projet suivant et a assigné un Chef de Projet :

            ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
            📁 Projet          : %s
            🏢 Client          : %s
            ✅ Statut          : VALIDÉ
            👨‍💼 Manager         : %s
            👤 Chef de Projet  : %s
            ⏰ Date            : %s
            ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

            💬 %s

            Connectez-vous à la plateforme pour consulter les détails du projet.

            ---
            Cet email a été envoyé automatiquement. Merci de ne pas y répondre.
            © DXC Technology - Plateforme de gestion de projets
            """,
                rc.getPrenom(),
                project.getName(),
                project.getClient() != null ? project.getClient() : "N/A",
                manager != null ? manager.getFullName() : "N/A",
                chefProjetInfo,
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")),
                commentaireText
        );

        sendSimpleEmail(rc.getEmail(), subject, content);
        log.info("Notification de validation de projet envoyée au RC : {}", rc.getEmail());
    }

    /**
     * Notification au RC (Responsable de Contrat) quand le manager REJETTE un projet.
     */
    public void notifyRcProjectRejectedByManager(Project project, User rc, User manager,
                                                 String commentaire) {
        String subject = "❌ Projet rejeté par le manager - " + project.getName();

        String motifText = (commentaire != null && !commentaire.isBlank())
                ? "Motif du rejet : " + commentaire
                : "Motif du rejet : Non spécifié";

        String content = String.format("""
            Bonjour %s,

            Le manager a rejeté le projet suivant. Veuillez en prendre connaissance et apporter les corrections nécessaires.

            ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
            📁 Projet   : %s
            🏢 Client   : %s
            ❌ Statut   : REJETÉ
            👨‍💼 Manager  : %s
            ⏰ Date     : %s
            ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

            💬 %s

            Connectez-vous à la plateforme pour modifier le projet et le soumettre à nouveau.

            ---
            Cet email a été envoyé automatiquement. Merci de ne pas y répondre.
            © DXC Technology - Plateforme de gestion de projets
            """,
                rc.getPrenom(),
                project.getName(),
                project.getClient() != null ? project.getClient() : "N/A",
                manager != null ? manager.getFullName() : "N/A",
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")),
                motifText
        );

        sendSimpleEmail(rc.getEmail(), subject, content);
        log.info("Notification de rejet de projet envoyée au RC : {}", rc.getEmail());
    }
}
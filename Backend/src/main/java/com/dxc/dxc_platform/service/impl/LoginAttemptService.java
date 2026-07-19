package com.dxc.dxc_platform.service.impl;

import com.dxc.dxc_platform.entity.User;
import com.dxc.dxc_platform.repository.UserRepository;
import com.dxc.dxc_platform.service.AuditService;
import com.dxc.dxc_platform.service.EmailService;
import com.dxc.dxc_platform.service.NotificationService;
import com.dxc.dxc_platform.shared.exception.NotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class LoginAttemptService {

    private static final Logger log = LoggerFactory.getLogger(LoginAttemptService.class);
    private static final int MAX_FAILED_ATTEMPTS = 3;

    private final UserRepository userRepository;
    private final AuditService auditService;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;
    private final NotificationService notificationService;

    public LoginAttemptService(UserRepository userRepository,
                               AuditService auditService,
                               EmailService emailService,
                               PasswordEncoder passwordEncoder,
                               NotificationService notificationService) {
        this.userRepository = userRepository;
        this.auditService = auditService;
        this.emailService = emailService;
        this.passwordEncoder = passwordEncoder;
        this.notificationService = notificationService;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public int registerFailedAttempt(String email, String ipAddress) {
        User user = userRepository.findByEmailAndDeletedFalse(email)
                .orElseThrow(() -> new NotFoundException(
                        "USER_NOT_FOUND", "Utilisateur introuvable"));

        int attempts = user.getFailedAttempts() + 1;
        System.out.println(">>> TENTATIVE " + attempts + " pour " + email);

        userRepository.incrementFailedAttempts(email);

        if (attempts == MAX_FAILED_ATTEMPTS) {
            // Verrouiller le compte
            user.setLocked(true);
            user.setMustChangePassword(true);
            userRepository.save(user);

            // Envoyer notifications aux admins
            notifyAdminsAccountLocked(user, ipAddress);

            // Audit
            auditService.log("ACCOUNT_LOCKED", "USER", user.getId(),
                    "Compte verrouillé après " + attempts + " tentatives échouées. L'admin doit réinitialiser le mot de passe.",
                    email, ipAddress);
        }

        return MAX_FAILED_ATTEMPTS - attempts;
    }

    private void notifyAdminsAccountLocked(User lockedUser, String ipAddress) {
        try {
            List<User> admins = userRepository.findAllAdmins();

            if (admins == null || admins.isEmpty()) {
                log.warn("Aucun admin trouvé pour la notification");
                return;
            }

            // 1. Envoyer les notifications WebSocket en temps réel
            String title = "🔐 Compte verrouillé";
            String content = String.format("Le compte de %s %s (%s) a été verrouillé après 3 tentatives de connexion échouées.",
                    lockedUser.getPrenom(), lockedUser.getNom(), lockedUser.getEmail());

            Map<String, Object> metadata = new HashMap<>();
            metadata.put("userId", lockedUser.getId());
            metadata.put("ipAddress", ipAddress);
            metadata.put("lockedAt", LocalDateTime.now().toString());

            for (User admin : admins) {
                notificationService.createNotification(
                        admin,
                        title,
                        content,
                        "ACCOUNT_LOCKED",
                        null,
                        "/admin/users/" + lockedUser.getId(),
                        metadata
                );
                log.info("Notification WebSocket envoyée à l'admin: {}", admin.getEmail());
            }

            // 2. Envoyer les emails aux admins
            String subject = "ALERTE SECURITE - Compte verrouillé - " + lockedUser.getEmail();
            String emailContent = String.format("""
            ALERTE SECURITE - Compte verrouillé - %s
            
            Notification automatique de la plateforme DXC.
            
            Le compte suivant a été automatiquement verrouillé après 3 tentatives de connexion échouées :
            
            Email : %s
            Nom   : %s %s
            Statut : VERROUILLÉ
            Date   : %s
            
            Action requise :
            Pour déverrouiller ce compte, veuillez :
            1. Vous connecter à l'interface d'administration
            2. Rechercher cet utilisateur
            3. Utiliser la fonction "Réinitialiser le mot de passe"
            4. Envoyer le nouveau mot de passe temporaire à l'utilisateur
            
            ---
            Cet email a été envoyé automatiquement pour des raisons de sécurité.
            © DXC Technology - Plateforme de gestion de projets
            """,
                    lockedUser.getEmail(),
                    lockedUser.getEmail(),
                    lockedUser.getPrenom() != null ? lockedUser.getPrenom() : "",
                    lockedUser.getNom() != null ? lockedUser.getNom() : "",
                    LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"))
            );

            for (User admin : admins) {
                emailService.sendSimpleEmail(admin.getEmail(), subject, emailContent);
                log.info("Email de verrouillage envoyé à l'admin: {}", admin.getEmail());
            }

        } catch (Exception e) {
            log.error("Erreur envoi notification aux admins: {}", e.getMessage(), e);
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void resetAttempts(String email) {
        userRepository.resetFailedAttempts(email);
        log.info("resetAttempts() terminé pour {}", email);
    }
}
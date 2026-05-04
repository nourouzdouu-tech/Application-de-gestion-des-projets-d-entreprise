package com.dxc.dxc_platform.service.impl;

import com.dxc.dxc_platform.entity.User;
import com.dxc.dxc_platform.repository.UserRepository;
import com.dxc.dxc_platform.service.AuditService;
import com.dxc.dxc_platform.service.EmailService;
import com.dxc.dxc_platform.shared.exception.NotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class LoginAttemptService {

    private static final int MAX_FAILED_ATTEMPTS = 3;

    private final UserRepository userRepository;
    private final AuditService auditService;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;

    public LoginAttemptService(UserRepository userRepository,
                               AuditService auditService,
                               EmailService emailService,
                               PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.auditService = auditService;
        this.emailService = emailService;
        this.passwordEncoder = passwordEncoder;
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
            user.setMustChangePassword(true); // Forcer changement de mot de passe
            userRepository.save(user);

            // ✅ Envoyer email aux admins (SANS le mot de passe)
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

            if (admins.isEmpty()) {
                return;
            }

            String subject = "🔐 ALERTE SECURITE - Compte verrouillé - " + lockedUser.getEmail();

            String content = String.format("""
            ⚠️ ALERTE DE SÉCURITÉ ⚠️
            
            Bonjour Admin,
            
            Le compte suivant a été automatiquement verrouillé après 3 tentatives de connexion échouées :
            
            ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
            📧 Email : %s
            👤 Nom   : %s %s
            🌐 IP source : %s
            🔒 Statut : VERROUILLÉ
            ⏰ Date   : %s
            ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
            
            🔑 Action requise :
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
                    lockedUser.getPrenom() != null ? lockedUser.getPrenom() : "",
                    lockedUser.getNom() != null ? lockedUser.getNom() : "",
                    ipAddress,
                    java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"))
            );

            for (User admin : admins) {
                emailService.sendSimpleEmail(admin.getEmail(), subject, content);
                System.out.println("Email de verrouillage envoyé à l'admin: " + admin.getEmail());
            }

        } catch (Exception e) {
            System.err.println("Erreur envoi email aux admins: " + e.getMessage());
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void resetAttempts(String email) {
        userRepository.resetFailedAttempts(email);
        System.out.println(">>> resetAttempts() TERMINÉ pour " + email);
    }
}
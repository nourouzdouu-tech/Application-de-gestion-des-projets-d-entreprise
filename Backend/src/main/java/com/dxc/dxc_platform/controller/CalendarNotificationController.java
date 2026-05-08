package com.dxc.dxc_platform.controller;

import com.dxc.dxc_platform.dto.CalendarInvitationDto;
import com.dxc.dxc_platform.entity.User;
import com.dxc.dxc_platform.repository.UserRepository;
import com.dxc.dxc_platform.service.EmailService;
import com.dxc.dxc_platform.shared.exception.NotFoundException;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/calendar")
public class CalendarNotificationController {

    private final EmailService emailService;
    private final UserRepository userRepository;

    public CalendarNotificationController(EmailService emailService, UserRepository userRepository) {
        this.emailService = emailService;
        this.userRepository = userRepository;
    }

    /**
     * Envoyer des notifications d'invitation aux utilisateurs invités à un événement
     */
    @PostMapping("/send-invitations")
    public ResponseEntity<?> sendInvitations(@Valid @RequestBody CalendarInvitationDto invitation) {

        Map<String, Object> response = new HashMap<>();
        int emailsSent = 0;
        int usersNotFound = 0;

        for (Long userId : invitation.getInvitedUserIds()) {
            try {
                User invitedUser = userRepository.findByIdAndDeletedFalse(userId)
                        .orElseThrow(() -> new NotFoundException("USER_NOT_FOUND",
                                "Utilisateur non trouvé: " + userId));

                emailService.notifyCalendarInvitation(
                        invitedUser.getEmail(),
                        invitedUser.getPrenom(),
                        invitation.getOwnerName(),
                        invitation.getTitle(),
                        invitation.getDate(),
                        invitation.getStartTime(),
                        invitation.getEndTime(),
                        invitation.getDescription()
                );
                emailsSent++;

            } catch (NotFoundException e) {
                usersNotFound++;
                System.err.println("Utilisateur non trouvé: " + userId);
            } catch (Exception e) {
                System.err.println("Erreur envoi email à " + userId + ": " + e.getMessage());
            }
        }

        response.put("success", true);
        response.put("emailsSent", emailsSent);
        response.put("usersNotFound", usersNotFound);
        response.put("message", emailsSent + " invitation(s) envoyée(s)");

        return ResponseEntity.ok(response);
    }
}
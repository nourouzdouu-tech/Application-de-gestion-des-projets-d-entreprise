// com/dxc/dxc_platform/controller/NotificationController.java
package com.dxc.dxc_platform.controller;

import com.dxc.dxc_platform.dto.NotificationDto;
import com.dxc.dxc_platform.entity.User;
import com.dxc.dxc_platform.repository.UserRepository;
import com.dxc.dxc_platform.service.NotificationService;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
@CrossOrigin(origins = "http://localhost:4200")
public class NotificationController {

    private final NotificationService notificationService;
    private final UserRepository userRepository;

    public NotificationController(NotificationService notificationService,
                                  UserRepository userRepository) {
        this.notificationService = notificationService;
        this.userRepository = userRepository;
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Page<NotificationDto>> getNotifications(
            Authentication authentication,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        User currentUser = getUserFromAuthentication(authentication);
        Page<NotificationDto> notifications = notificationService.getUserNotifications(currentUser, page, size);
        return ResponseEntity.ok(notifications);
    }

    @GetMapping("/unread")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<NotificationDto>> getUnreadNotifications(Authentication authentication) {
        User currentUser = getUserFromAuthentication(authentication);
        List<NotificationDto> notifications = notificationService.getUnreadNotifications(currentUser);
        return ResponseEntity.ok(notifications);
    }

    @GetMapping("/unread/count")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Long>> getUnreadCount(Authentication authentication) {
        User currentUser = getUserFromAuthentication(authentication);
        long count = notificationService.getUnreadCountForUser(currentUser);

        Map<String, Long> response = new HashMap<>();
        response.put("count", count);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}/read")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> markAsRead(@PathVariable Long id, Authentication authentication) {
        User currentUser = getUserFromAuthentication(authentication);
        notificationService.markAsRead(currentUser, id);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/read-all")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> markAllAsRead(Authentication authentication) {
        User currentUser = getUserFromAuthentication(authentication);
        notificationService.markAllAsRead(currentUser);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/cleanup")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Integer>> cleanupOldNotifications(
            Authentication authentication,
            @RequestParam(defaultValue = "30") int daysOld) {

        User currentUser = getUserFromAuthentication(authentication);
        int deletedCount = notificationService.cleanupOldNotifications(currentUser, daysOld);

        Map<String, Integer> response = new HashMap<>();
        response.put("deletedCount", deletedCount);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/admin/broadcast")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> broadcastToAllAdmins(
            Authentication authentication,
            @RequestBody Map<String, String> request) {

        User currentUser = getUserFromAuthentication(authentication);
        String title = request.get("title");
        String content = request.get("content");

        if (title == null || title.trim().isEmpty()) {
            title = "📢 Annonce importante";
        }

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("broadcastBy", currentUser.getEmail());
        metadata.put("broadcastAt", System.currentTimeMillis());

        notificationService.notifyAllAdmins(
                title,
                content,
                "BROADCAST",
                currentUser,
                "/dashboard",
                metadata
        );

        return ResponseEntity.ok(Map.of(
                "message", "Notification broadcastée à tous les admins"
        ));
    }

    private User getUserFromAuthentication(Authentication authentication) {
        String email = authentication.getName();
        return userRepository.findByEmailAndDeletedFalse(email)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé: " + email));
    }
    // Dans NotificationController.java - AJOUTEZ CETTE METHODE
    @PostMapping("/test-chef")
    @PreAuthorize("hasAnyRole('CHEF_PROJET', 'ADMIN')")
    public ResponseEntity<?> testChefNotification(Authentication authentication) {
        User currentUser = getUserFromAuthentication(authentication);

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("test", true);
        metadata.put("timestamp", System.currentTimeMillis());
        metadata.put("type", "chef_test");

        NotificationDto notification = notificationService.createNotification(
                currentUser,
                "🧪 TEST Chef Projet",
                "Ceci est une notification de test pour le chef de projet. Si vous voyez ce message, le système fonctionne parfaitement !",
                "TEST_CHEF",
                currentUser,
                "/chef-projet/dashboard",
                metadata
        );

        return ResponseEntity.ok(Map.of(
                "message", "Notification de test envoyée au chef de projet",
                "notification", notification
        ));
    }
}
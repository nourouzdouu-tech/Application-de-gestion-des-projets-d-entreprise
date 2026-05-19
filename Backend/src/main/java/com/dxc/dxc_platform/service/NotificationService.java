// com/dxc/dxc_platform/service/NotificationService.java
package com.dxc.dxc_platform.service;

import com.dxc.dxc_platform.dto.NotificationDto;
import com.dxc.dxc_platform.entity.Notification;
import com.dxc.dxc_platform.entity.User;
import com.dxc.dxc_platform.repository.NotificationRepository;
import com.dxc.dxc_platform.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final ObjectMapper objectMapper;

    public NotificationService(NotificationRepository notificationRepository,
                               UserRepository userRepository,
                               SimpMessagingTemplate messagingTemplate,
                               ObjectMapper objectMapper) {
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
        this.messagingTemplate = messagingTemplate;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public NotificationDto createNotification(User user, String title, String content,
                                              String type, User createdBy, String actionUrl,
                                              Map<String, Object> metadata) {
        String metadataJson = null;
        if (metadata != null && !metadata.isEmpty()) {
            try {
                metadataJson = objectMapper.writeValueAsString(metadata);
            } catch (Exception e) {
                log.error("Erreur lors de la sérialisation des métadonnées", e);
            }
        }

        Notification notification = new Notification(title, content, type, user, createdBy, actionUrl, metadataJson);
        Notification saved = notificationRepository.save(notification);
        NotificationDto dto = convertToDto(saved);

        sendRealTimeNotification(user.getId(), dto);

        log.info("Notification créée pour l'utilisateur {}: {}", user.getEmail(), title);
        return dto;
    }

    @Transactional
    public void notifyAllAdmins(String title, String content, String type,
                                User createdBy, String actionUrl, Map<String, Object> metadata) {
        List<User> admins = userRepository.findAllAdmins();

        if (admins == null || admins.isEmpty()) {
            log.warn("Aucun admin trouvé pour la notification");
            return;
        }

        for (User admin : admins) {
            createNotification(admin, title, content, type, createdBy, actionUrl, metadata);
        }

        log.info("Notification envoyée à {} admins: {}", admins.size(), title);
    }

    private void sendRealTimeNotification(Long userId, NotificationDto notification) {
        try {
            // Envoyer la notification
            Map<String, Object> wsMessage = new HashMap<>();
            wsMessage.put("type", "new_notification");
            wsMessage.put("notification", notification);

            messagingTemplate.convertAndSendToUser(
                    userId.toString(),
                    "/queue/notifications",
                    wsMessage
            );

            // Envoyer le compteur mis à jour
            userRepository.findById(userId).ifPresent(user -> {
                long unreadCount = getUnreadCountForUser(user);
                Map<String, Object> countMessage = new HashMap<>();
                countMessage.put("type", "unread_count");
                countMessage.put("count", unreadCount);
                messagingTemplate.convertAndSendToUser(
                        userId.toString(),
                        "/queue/notifications",
                        countMessage
                );
            });
        } catch (Exception e) {
            log.error("Erreur lors de l'envoi WebSocket de la notification", e);
        }
    }

    public Page<NotificationDto> getUserNotifications(User user, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return notificationRepository.findByUserOrderByCreatedAtDesc(user, pageable)
                .map(this::convertToDto);
    }

    public List<NotificationDto> getUnreadNotifications(User user) {
        List<Notification> notifications = notificationRepository.findByUserAndReadFalseOrderByCreatedAtDesc(user);
        return notifications.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public void markAsRead(User user, Long notificationId) {
        notificationRepository.markAsRead(user, notificationId);

        long unreadCount = getUnreadCountForUser(user);
        Map<String, Object> countMessage = new HashMap<>();
        countMessage.put("type", "unread_count");
        countMessage.put("count", unreadCount);
        messagingTemplate.convertAndSendToUser(
                user.getId().toString(),
                "/queue/notifications",
                countMessage
        );
    }

    @Transactional
    public void markAllAsRead(User user) {
        notificationRepository.markAllAsRead(user);

        Map<String, Object> countMessage = new HashMap<>();
        countMessage.put("type", "unread_count");
        countMessage.put("count", 0L);
        messagingTemplate.convertAndSendToUser(
                user.getId().toString(),
                "/queue/notifications",
                countMessage
        );
    }

    public long getUnreadCountForUser(User user) {
        if (user == null) return 0;
        return notificationRepository.countUnread(user);
    }

    @Transactional
    public int cleanupOldNotifications(User user, int daysOld) {
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(daysOld);
        return notificationRepository.deleteOldReadNotifications(user, cutoffDate);
    }

    private NotificationDto convertToDto(Notification notification) {
        NotificationDto dto = new NotificationDto();
        dto.setId(notification.getId());
        dto.setTitle(notification.getTitle());
        dto.setContent(notification.getContent());
        dto.setType(notification.getType());
        dto.setRead(notification.isRead());
        dto.setCreatedAt(notification.getCreatedAt());
        dto.setActionUrl(notification.getActionUrl());
        dto.setMetadata(notification.getMetadata());

        if (notification.getUser() != null) {
            dto.setUserId(notification.getUser().getId());
            dto.setUserName(notification.getUser().getPrenom() + " " + notification.getUser().getNom());
        }

        if (notification.getCreatedBy() != null) {
            dto.setCreatedById(notification.getCreatedBy().getId());
            dto.setCreatedByName(notification.getCreatedBy().getPrenom() + " " + notification.getCreatedBy().getNom());
        }

        return dto;
    }
    // Dans NotificationService.java
    // Dans NotificationService.java - AJOUTEZ CETTE METHODE
    @Transactional
    public void notifyChefProjet(User chefProjet, String title, String content,
                                 String type, User createdBy, String actionUrl,
                                 Map<String, Object> metadata) {
        createNotification(chefProjet, title, content, type, createdBy, actionUrl, metadata);
        log.info("Notification envoyée au chef de projet {}: {}", chefProjet.getEmail(), title);
    }

    @Transactional
    public void notifyAllChefsProjet(List<User> chefsProjet, String title, String content,
                                     String type, User createdBy, String actionUrl,
                                     Map<String, Object> metadata) {
        if (chefsProjet == null || chefsProjet.isEmpty()) {
            log.warn("Aucun chef de projet trouvé pour la notification");
            return;
        }

        for (User chef : chefsProjet) {
            createNotification(chef, title, content, type, createdBy, actionUrl, metadata);
        }

        log.info("Notification envoyée à {} chefs de projet: {}", chefsProjet.size(), title);
    }
    // Dans NotificationService.java - AJOUTEZ CETTE METHODE
    @Transactional
    public void notifyMembre(User membre, String title, String content,
                             String type, User createdBy, String actionUrl,
                             Map<String, Object> metadata) {
        createNotification(membre, title, content, type, createdBy, actionUrl, metadata);
        log.info("Notification envoyée au membre {}: {}", membre.getEmail(), title);
    }
    // Dans NotificationService.java - AJOUTEZ
    @Transactional
    public void notifyManager(User manager, String title, String content,
                              String type, User createdBy, String actionUrl,
                              Map<String, Object> metadata) {
        createNotification(manager, title, content, type, createdBy, actionUrl, metadata);
        log.info("Notification envoyée au manager {}: {}", manager.getEmail(), title);
    }
    // Dans NotificationService.java - AJOUTEZ
    @Transactional
    public void notifyResponsableContrat(User rc, String title, String content,
                                         String type, User createdBy, String actionUrl,
                                         Map<String, Object> metadata) {
        createNotification(rc, title, content, type, createdBy, actionUrl, metadata);
        log.info("Notification envoyée au Responsable de Contrat {}: {}", rc.getEmail(), title);
    }

}
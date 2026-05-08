package com.dxc.dxc_platform.controller;

import com.dxc.dxc_platform.dto.WebSocketMessageDto;
import com.dxc.dxc_platform.entity.Message;
import com.dxc.dxc_platform.entity.User;
import com.dxc.dxc_platform.repository.UserRepository;
import com.dxc.dxc_platform.service.MessageService;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Controller
public class MessageWebSocketController {

    private final SimpMessagingTemplate messagingTemplate;
    private final MessageService messageService;
    private final UserRepository userRepository;

    // Stocker les utilisateurs en ligne
    private final Set<Long> onlineUserIds = ConcurrentHashMap.newKeySet();

    public MessageWebSocketController(SimpMessagingTemplate messagingTemplate,
                                      MessageService messageService,
                                      UserRepository userRepository) {
        this.messagingTemplate = messagingTemplate;
        this.messageService = messageService;
        this.userRepository = userRepository;
    }

    @MessageMapping("/chat.connect")
    public void handleConnect(@Payload WebSocketMessageDto connectMessage, Principal principal) {
        System.out.println("🔌 CONNECT reçu - Principal: " + (principal != null ? principal.getName() : "null"));

        if (principal == null) return;

        String email = principal.getName();
        User user = userRepository.findByEmailAndDeletedFalse(email)
                .orElse(null);

        if (user != null && !onlineUserIds.contains(user.getId())) {
            onlineUserIds.add(user.getId());

            System.out.println("✅ Utilisateur connecté: " + user.getEmail() + " (ID: " + user.getId() + ") - Total: " + onlineUserIds.size());

            // Diffuser le statut "en ligne" à tous les utilisateurs connectés
            broadcastUserStatus(user.getId(), true, user.getPrenom() + " " + user.getNom());
        }
    }

    @MessageMapping("/chat.disconnect")
    public void handleDisconnect(@Payload WebSocketMessageDto disconnectMessage, Principal principal) {
        System.out.println("🔌 DISCONNECT reçu - Principal: " + (principal != null ? principal.getName() : "null"));

        if (principal == null) return;

        String email = principal.getName();
        User user = userRepository.findByEmailAndDeletedFalse(email)
                .orElse(null);

        if (user != null && onlineUserIds.contains(user.getId())) {
            onlineUserIds.remove(user.getId());

            System.out.println("❌ Utilisateur déconnecté: " + user.getEmail() + " (ID: " + user.getId() + ") - Total: " + onlineUserIds.size());

            // Diffuser le statut "déconnecté" à tous les utilisateurs
            broadcastUserStatus(user.getId(), false, user.getPrenom() + " " + user.getNom());
        }
    }

    @MessageMapping("/chat.status")
    public void checkUserStatus(@Payload WebSocketMessageDto statusMessage, Principal principal) {
        System.out.println("📊 checkUserStatus reçu - receiverId: " + statusMessage.getReceiverId());

        if (principal == null) return;

        Long userIdToCheck = statusMessage.getReceiverId();
        boolean isOnline = onlineUserIds.contains(userIdToCheck);

        System.out.println("  → userIdToCheck: " + userIdToCheck + ", isOnline: " + isOnline);

        WebSocketMessageDto response = new WebSocketMessageDto();
        response.setType("user_status");
        response.setUserId(userIdToCheck);
        response.setOnline(isOnline);

        // Optionnel: ajouter le nom de l'utilisateur
        userRepository.findById(userIdToCheck).ifPresent(user -> {
            response.setSenderName(user.getPrenom() + " " + user.getNom());
        });

        String currentUserEmail = principal.getName();
        messagingTemplate.convertAndSendToUser(currentUserEmail, "/queue/messages", response);
        System.out.println("  → Réponse envoyée à: " + currentUserEmail);
    }

    @MessageMapping("/chat.send")
    public void sendMessage(@Payload WebSocketMessageDto chatMessage, Principal principal) {
        if (principal == null) {
            throw new RuntimeException("Utilisateur WebSocket non authentifié");
        }

        String senderEmail = principal.getName();

        User sender = userRepository.findByEmailAndDeletedFalse(senderEmail)
                .orElseThrow(() -> new RuntimeException("Expéditeur non trouvé"));

        User receiver = userRepository.findById(chatMessage.getReceiverId())
                .orElseThrow(() -> new RuntimeException("Destinataire non trouvé"));

        Message saved = messageService.saveWebSocketMessage(
                sender,
                receiver,
                chatMessage.getContent(),
                chatMessage.getClientTempId(),
                chatMessage.getReplyToMessageId()
        );

        WebSocketMessageDto response = toMessageDto(saved);
        response.setClientTempId(chatMessage.getClientTempId());
        response.setReplyToMessageId(chatMessage.getReplyToMessageId());

        messagingTemplate.convertAndSendToUser(sender.getEmail(), "/queue/messages", response);
        messagingTemplate.convertAndSendToUser(receiver.getEmail(), "/queue/messages", response);
    }

    @MessageMapping("/chat.reaction")
    public void sendReaction(@Payload WebSocketMessageDto reactionDto, Principal principal) {
        if (principal == null) {
            throw new RuntimeException("Utilisateur WebSocket non authentifié");
        }

        if (reactionDto.getMessageId() == null) {
            throw new RuntimeException("messageId est obligatoire pour une réaction");
        }

        if (reactionDto.getEmoji() == null || reactionDto.getEmoji().isBlank()) {
            throw new RuntimeException("emoji est obligatoire pour une réaction");
        }

        String senderEmail = principal.getName();

        User sender = userRepository.findByEmailAndDeletedFalse(senderEmail)
                .orElseThrow(() -> new RuntimeException("Expéditeur non trouvé"));

        User receiver = userRepository.findById(reactionDto.getReceiverId())
                .orElseThrow(() -> new RuntimeException("Destinataire non trouvé"));

        messageService.toggleReaction(
                reactionDto.getMessageId(),
                reactionDto.getEmoji().trim(),
                reactionDto.isAdd(),
                sender
        );

        WebSocketMessageDto response = new WebSocketMessageDto();
        response.setType("reaction");
        response.setMessageId(reactionDto.getMessageId());
        response.setEmoji(reactionDto.getEmoji().trim());
        response.setAdd(reactionDto.isAdd());
        response.setSenderId(sender.getId());
        response.setSenderName(sender.getPrenom() + " " + sender.getNom());
        response.setReceiverId(receiver.getId());
        response.setReceiverName(receiver.getPrenom() + " " + receiver.getNom());
        response.setSentAt(LocalDateTime.now());

        messagingTemplate.convertAndSendToUser(sender.getEmail(), "/queue/messages", response);
        messagingTemplate.convertAndSendToUser(receiver.getEmail(), "/queue/messages", response);
    }

    @MessageMapping("/chat.ping")
    public void handlePing(@Payload WebSocketMessageDto pingMessage, Principal principal) {
        // Ne rien faire, ça maintient juste la connexion active
    }

    private void broadcastUserStatus(Long userId, boolean online, String userName) {
        WebSocketMessageDto statusMessage = new WebSocketMessageDto();
        statusMessage.setType("user_status");
        statusMessage.setUserId(userId);
        statusMessage.setOnline(online);
        statusMessage.setSenderName(userName);
        statusMessage.setSentAt(LocalDateTime.now());

        System.out.println("📡 Broadcast statut: userId=" + userId + ", online=" + online + ", userName=" + userName);
        System.out.println("  → Nombre d'utilisateurs en ligne: " + onlineUserIds.size());

        // Envoyer à tous les utilisateurs connectés
        for (Long targetUserId : onlineUserIds) {
            try {
                User targetUser = userRepository.findById(targetUserId).orElse(null);
                if (targetUser != null) {
                    messagingTemplate.convertAndSendToUser(targetUser.getEmail(), "/queue/messages", statusMessage);
                    System.out.println("  → Envoyé à: " + targetUser.getEmail());
                }
            } catch (Exception e) {
                System.err.println("Erreur envoi statut à " + targetUserId + ": " + e.getMessage());
            }
        }
    }

    private WebSocketMessageDto toMessageDto(Message msg) {
        WebSocketMessageDto dto = new WebSocketMessageDto();
        dto.setType("message");
        dto.setId(msg.getId());
        dto.setContent(msg.getContent());
        dto.setSenderId(msg.getSender().getId());
        dto.setSenderName(msg.getSender().getPrenom() + " " + msg.getSender().getNom());
        dto.setReceiverId(msg.getReceiver().getId());
        dto.setReceiverName(msg.getReceiver().getPrenom() + " " + msg.getReceiver().getNom());
        dto.setSentAt(msg.getSentAt());
        dto.setRead(msg.isRead());
        dto.setClientTempId(msg.getClientTempId());
        dto.setReplyToMessageId(msg.getReplyToMessage() != null ? msg.getReplyToMessage().getId() : null);
        return dto;
    }

    // Méthode utilitaire pour obtenir les utilisateurs en ligne (optionnel)
    public Set<Long> getOnlineUserIds() {
        return onlineUserIds;
    }

    public boolean isUserOnline(Long userId) {
        return onlineUserIds.contains(userId);
    }
}
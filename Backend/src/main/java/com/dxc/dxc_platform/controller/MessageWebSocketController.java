package com.dxc.dxc_platform.controller;

import com.dxc.dxc_platform.dto.WebSocketMessageDto;
import com.dxc.dxc_platform.entity.Message;
import com.dxc.dxc_platform.entity.User;
import com.dxc.dxc_platform.repository.UserRepository;
import com.dxc.dxc_platform.service.MessageService;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;

import java.security.Principal;

@Controller
public class MessageWebSocketController {

    private final SimpMessagingTemplate messagingTemplate;
    private final MessageService messageService;
    private final UserRepository userRepository;

    public MessageWebSocketController(SimpMessagingTemplate messagingTemplate,
                                      MessageService messageService,
                                      UserRepository userRepository) {
        this.messagingTemplate = messagingTemplate;
        this.messageService = messageService;
        this.userRepository = userRepository;
    }

    @MessageMapping("/chat.send")
    public void sendMessage(@Payload WebSocketMessageDto chatMessage, Principal principal) {
        // Récupérer l'expéditeur à partir du token (email)
        String senderEmail = principal.getName();
        User sender = userRepository.findByEmailAndDeletedFalse(senderEmail)
                .orElseThrow(() -> new RuntimeException("Expéditeur non trouvé"));
        User receiver = userRepository.findById(chatMessage.getReceiverId())
                .orElseThrow(() -> new RuntimeException("Destinataire non trouvé"));

        Message saved = messageService.saveWebSocketMessage(sender, receiver, chatMessage.getContent());
        WebSocketMessageDto response = toDto(saved);

        // Envoyer aux deux participants (via leur email)
        messagingTemplate.convertAndSendToUser(sender.getEmail(), "/queue/messages", response);
        messagingTemplate.convertAndSendToUser(receiver.getEmail(), "/queue/messages", response);
    }
    private WebSocketMessageDto toDto(Message msg) {
        WebSocketMessageDto dto = new WebSocketMessageDto();
        dto.setId(msg.getId());
        dto.setContent(msg.getContent());
        dto.setSenderId(msg.getSender().getId());
        dto.setSenderName(msg.getSender().getPrenom() + " " + msg.getSender().getNom());
        dto.setReceiverId(msg.getReceiver().getId());
        dto.setReceiverName(msg.getReceiver().getPrenom() + " " + msg.getReceiver().getNom());
        dto.setSentAt(msg.getSentAt());
        dto.setRead(msg.isRead());
        return dto;
    }
}
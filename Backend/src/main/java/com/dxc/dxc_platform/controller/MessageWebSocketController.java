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
        String senderEmail = principal.getName();

        User sender = userRepository.findByEmailAndDeletedFalse(senderEmail)
                .orElseThrow(() -> new RuntimeException("Expéditeur non trouvé"));

        User receiver = userRepository.findById(reactionDto.getReceiverId())
                .orElseThrow(() -> new RuntimeException("Destinataire non trouvé"));

        WebSocketMessageDto response = new WebSocketMessageDto();
        response.setType("reaction");
        response.setMessageId(reactionDto.getMessageId());
        response.setEmoji(reactionDto.getEmoji());
        response.setAdd(reactionDto.isAdd());
        response.setSenderId(sender.getId());
        response.setSenderName(sender.getPrenom() + " " + sender.getNom());
        response.setReceiverId(receiver.getId());
        response.setReceiverName(receiver.getPrenom() + " " + receiver.getNom());
        response.setSentAt(LocalDateTime.now());

        messagingTemplate.convertAndSendToUser(sender.getEmail(), "/queue/messages", response);
        messagingTemplate.convertAndSendToUser(receiver.getEmail(), "/queue/messages", response);
    }

    @MessageMapping("/chat.file")
    public void sendFile(@Payload WebSocketMessageDto fileDto, Principal principal) {
        String senderEmail = principal.getName();

        User sender = userRepository.findByEmailAndDeletedFalse(senderEmail)
                .orElseThrow(() -> new RuntimeException("Expéditeur non trouvé"));

        User receiver = userRepository.findById(fileDto.getReceiverId())
                .orElseThrow(() -> new RuntimeException("Destinataire non trouvé"));

        Message saved = messageService.saveWebSocketFileMessage(
                sender,
                receiver,
                fileDto.getClientTempId(),
                fileDto.getReplyToMessageId()
        );

        WebSocketMessageDto response = new WebSocketMessageDto();
        response.setType("file");
        response.setId(saved.getId());
        response.setMessageId(saved.getId());
        response.setClientTempId(fileDto.getClientTempId());
        response.setReplyToMessageId(fileDto.getReplyToMessageId());
        response.setFileName(fileDto.getFileName());
        response.setFileType(fileDto.getFileType());
        response.setFileSize(fileDto.getFileSize());
        response.setFileData(fileDto.getFileData());
        response.setSenderId(sender.getId());
        response.setSenderName(sender.getPrenom() + " " + sender.getNom());
        response.setReceiverId(receiver.getId());
        response.setReceiverName(receiver.getPrenom() + " " + receiver.getNom());
        response.setSentAt(saved.getSentAt());
        response.setRead(saved.isRead());

        messagingTemplate.convertAndSendToUser(sender.getEmail(), "/queue/messages", response);
        messagingTemplate.convertAndSendToUser(receiver.getEmail(), "/queue/messages", response);
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
}
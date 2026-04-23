package com.dxc.dxc_platform.service.impl;

import com.dxc.dxc_platform.dto.ConversationDto;
import com.dxc.dxc_platform.dto.MessageDto;
import com.dxc.dxc_platform.dto.MessageReactionDto;
import com.dxc.dxc_platform.entity.Conversation;
import com.dxc.dxc_platform.entity.Message;
import com.dxc.dxc_platform.entity.User;
import com.dxc.dxc_platform.repository.ConversationRepository;
import com.dxc.dxc_platform.repository.MessageRepository;
import com.dxc.dxc_platform.repository.UserRepository;
import com.dxc.dxc_platform.service.FileStorageService;
import com.dxc.dxc_platform.service.MessageService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class MessageServiceImpl implements MessageService {

    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    private final UserRepository userRepository;
    private final FileStorageService fileStorageService;
    private final ObjectMapper objectMapper;

    @Value("${app.base-url}")
    private String baseUrl;

    public MessageServiceImpl(ConversationRepository conversationRepository,
                              MessageRepository messageRepository,
                              UserRepository userRepository,
                              FileStorageService fileStorageService,
                              ObjectMapper objectMapper) {
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
        this.userRepository = userRepository;
        this.fileStorageService = fileStorageService;
        this.objectMapper = objectMapper;
    }

    private User getCurrentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmailAndDeletedFalse(email)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));
    }

    @Override
    @Transactional
    public ConversationDto getOrCreateConversation(Long otherUserId) {
        Long currentUserId = getCurrentUser().getId();

        Conversation conv = conversationRepository.findByUser1IdAndUser2Id(currentUserId, otherUserId)
                .orElseGet(() -> conversationRepository.findByUser2IdAndUser1Id(currentUserId, otherUserId)
                        .orElse(null));

        if (conv == null) {
            User u1 = userRepository.findById(currentUserId).orElseThrow();
            User u2 = userRepository.findById(otherUserId).orElseThrow();

            conv = new Conversation();
            conv.setUser1(u1);
            conv.setUser2(u2);
            conv = conversationRepository.save(conv);
        }

        return toConversationDto(conv, currentUserId);
    }

    @Override
    @Transactional
    public MessageDto sendMessage(Long receiverId, String content, String clientTempId, Long replyToMessageId) {
        User sender = getCurrentUser();
        User receiver = userRepository.findById(receiverId)
                .orElseThrow(() -> new RuntimeException("Destinataire non trouvé"));

        Message msg = buildAndSaveMessage(sender, receiver, content, clientTempId, replyToMessageId);
        return toMessageDto(msg);
    }

    @Override
    @Transactional
    public MessageDto sendFileMessage(Long receiverId,
                                      MultipartFile file,
                                      String clientTempId,
                                      Long replyToMessageId) {
        User sender = getCurrentUser();
        User receiver = userRepository.findById(receiverId)
                .orElseThrow(() -> new RuntimeException("Destinataire non trouvé"));

        Conversation conv = findOrCreateConversation(sender, receiver);

        Message replyTo = null;
        if (replyToMessageId != null) {
            replyTo = messageRepository.findById(replyToMessageId).orElse(null);
        }

        String relativeUrl = fileStorageService.storeFile(file);

        Message msg = new Message();
        msg.setContent("");
        msg.setSender(sender);
        msg.setReceiver(receiver);
        msg.setConversation(conv);
        msg.setSentAt(LocalDateTime.now());
        msg.setRead(false);
        msg.setClientTempId(clientTempId);
        msg.setReplyToMessage(replyTo);
        msg.setFileName(file.getOriginalFilename());
        msg.setFileType(file.getContentType());
        msg.setFileSize(file.getSize());
        msg.setFileUrl(baseUrl + relativeUrl);

        msg = messageRepository.save(msg);

        return toMessageDto(msg);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ConversationDto> getUserConversations() {
        Long userId = getCurrentUser().getId();

        List<Conversation> conversations = conversationRepository.findByUser1IdOrUser2Id(userId, userId);

        return conversations.stream()
                .map(c -> toConversationDto(c, userId))
                .sorted(Comparator.comparing(
                        ConversationDto::getLastMessageTime,
                        Comparator.nullsLast(Comparator.reverseOrder())
                ))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public List<MessageDto> getConversationMessages(Long conversationId) {
        Long userId = getCurrentUser().getId();

        Conversation conv = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new RuntimeException("Conversation non trouvée"));

        if (!conv.getUser1().getId().equals(userId) && !conv.getUser2().getId().equals(userId)) {
            throw new RuntimeException("Accès non autorisé");
        }

        List<Message> messages = messageRepository.findByConversationIdOrderBySentAtAscIdAsc(conversationId);

        List<Message> unreadReceivedMessages = messages.stream()
                .filter(m -> m.getReceiver().getId().equals(userId) && !m.isRead())
                .peek(m -> m.setRead(true))
                .collect(Collectors.toList());

        if (!unreadReceivedMessages.isEmpty()) {
            messageRepository.saveAll(unreadReceivedMessages);
        }

        return messages.stream()
                .map(this::toMessageDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public Message saveWebSocketMessage(User sender, User receiver, String content, String clientTempId, Long replyToMessageId) {
        return buildAndSaveMessage(sender, receiver, content, clientTempId, replyToMessageId);
    }

    @Override
    @Transactional
    public void toggleReaction(Long messageId, String emoji, boolean isAdd, User user) {
        System.out.println("=== toggleReaction DEBUT === messageId=" + messageId + " emoji=" + emoji + " isAdd=" + isAdd + " user=" + user.getId());

        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new RuntimeException("Message non trouvé : " + messageId));

        System.out.println("=== Message trouvé: " + message.getId() + " reactionsJson actuel: " + message.getReactionsJson());

        if (emoji == null || emoji.isBlank()) {
            throw new RuntimeException("Emoji invalide");
        }

        if (!message.getSender().getId().equals(user.getId()) &&
                !message.getReceiver().getId().equals(user.getId())) {
            throw new RuntimeException("Accès non autorisé à ce message");
        }

        try {
            List<MessageReactionDto> reactions = readReactions(message);
            System.out.println("=== Réactions lues: " + reactions.size());

            if (isAdd) {
                boolean exists = reactions.stream().anyMatch(r ->
                        r.getUserId() != null &&
                                r.getUserId().equals(user.getId()) &&
                                emoji.equals(r.getEmoji())
                );

                if (!exists) {
                    MessageReactionDto dto = new MessageReactionDto();
                    dto.setEmoji(emoji);
                    dto.setUserId(user.getId());
                    dto.setUserName((user.getPrenom() + " " + user.getNom()).trim());
                    reactions.add(dto);
                    System.out.println("=== Réaction AJOUTÉE. Total: " + reactions.size());
                } else {
                    System.out.println("=== Réaction déjà existante, pas d'ajout");
                }
            } else {
                int before = reactions.size();
                reactions.removeIf(r ->
                        r.getUserId() != null &&
                                r.getUserId().equals(user.getId()) &&
                                emoji.equals(r.getEmoji())
                );
                System.out.println("=== Réaction SUPPRIMÉE. Avant: " + before + " Après: " + reactions.size());
            }

            String newJson = objectMapper.writeValueAsString(reactions);
            System.out.println("=== Nouveau reactionsJson: " + newJson);

            message.setReactionsJson(newJson);
            Message saved = messageRepository.save(message);
            messageRepository.flush();

            System.out.println("=== Message sauvegardé id=" + saved.getId() + " reactionsJson=" + saved.getReactionsJson());

        } catch (Exception e) {
            System.err.println("=== ERREUR sauvegarde réaction: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Erreur sauvegarde réaction", e);
        }
    }

    private Conversation findOrCreateConversation(User sender, User receiver) {
        Conversation conv = conversationRepository.findByUser1IdAndUser2Id(sender.getId(), receiver.getId())
                .orElseGet(() -> conversationRepository.findByUser2IdAndUser1Id(sender.getId(), receiver.getId())
                        .orElse(null));

        if (conv == null) {
            conv = new Conversation();
            conv.setUser1(sender);
            conv.setUser2(receiver);
            conv = conversationRepository.save(conv);
        }

        return conv;
    }

    private Message buildAndSaveMessage(User sender,
                                        User receiver,
                                        String content,
                                        String clientTempId,
                                        Long replyToMessageId) {
        Conversation conv = findOrCreateConversation(sender, receiver);

        Message replyTo = null;
        if (replyToMessageId != null) {
            replyTo = messageRepository.findById(replyToMessageId).orElse(null);
        }

        Message msg = new Message();
        msg.setContent(content == null ? "" : content.trim());
        msg.setSender(sender);
        msg.setReceiver(receiver);
        msg.setConversation(conv);
        msg.setSentAt(LocalDateTime.now());
        msg.setRead(false);
        msg.setClientTempId(clientTempId);
        msg.setReplyToMessage(replyTo);

        return messageRepository.save(msg);
    }

    private ConversationDto toConversationDto(Conversation conv, Long currentUserId) {
        ConversationDto dto = new ConversationDto();
        dto.setId(conv.getId());

        boolean isUser1 = conv.getUser1().getId().equals(currentUserId);
        User other = isUser1 ? conv.getUser2() : conv.getUser1();

        dto.setOtherParticipantId(other.getId());
        dto.setOtherParticipantName((other.getPrenom() + " " + other.getNom()).trim());

        Message lastMessage = messageRepository
                .findTopByConversationIdOrderBySentAtDescIdDesc(conv.getId())
                .orElse(null);

        if (lastMessage != null) {
            String preview = lastMessage.getContent() == null ? "" : lastMessage.getContent().trim();
            if (lastMessage.getFileUrl() != null && !lastMessage.getFileUrl().isBlank()) {
                dto.setLastMessage("📎 " + lastMessage.getFileName());
            } else {
                dto.setLastMessage(preview.isEmpty() ? "[Pièce jointe]" : preview);
            }
            dto.setLastMessageTime(lastMessage.getSentAt());
        } else {
            dto.setLastMessage("");
            dto.setLastMessageTime(conv.getCreatedAt());
        }

        long unread = messageRepository.countByConversationIdAndReceiverIdAndReadFalse(
                conv.getId(),
                currentUserId
        );
        dto.setUnreadCount((int) unread);

        return dto;
    }

    private MessageDto toMessageDto(Message msg) {
        MessageDto dto = new MessageDto();
        dto.setId(msg.getId());
        dto.setContent(msg.getContent());
        dto.setSenderId(msg.getSender().getId());
        dto.setSenderName((msg.getSender().getPrenom() + " " + msg.getSender().getNom()).trim());
        dto.setReceiverId(msg.getReceiver().getId());
        dto.setReceiverName((msg.getReceiver().getPrenom() + " " + msg.getReceiver().getNom()).trim());
        dto.setSentAt(msg.getSentAt());
        dto.setRead(msg.isRead());
        dto.setClientTempId(msg.getClientTempId());
        dto.setReplyToMessageId(msg.getReplyToMessage() != null ? msg.getReplyToMessage().getId() : null);

        dto.setFileName(msg.getFileName());
        dto.setFileType(msg.getFileType());
        dto.setFileSize(msg.getFileSize());
        dto.setFileUrl(msg.getFileUrl());

        dto.setDownloadUrl(
                msg.getFileUrl() != null
                        ? baseUrl + "/api/files/download?path=" + msg.getFileUrl().replace(baseUrl, "")
                        : null
        );

        dto.setReactions(readReactions(msg));

        return dto;
    }

    private List<MessageReactionDto> readReactions(Message msg) {
        try {
            if (msg.getReactionsJson() == null || msg.getReactionsJson().isBlank()) {
                return new ArrayList<>();
            }
            // ✅ Log pour vérifier ce qui est stocké en base
            System.out.println("=== reactionsJson for msg " + msg.getId() + " : " + msg.getReactionsJson());

            return objectMapper.readValue(
                    msg.getReactionsJson(),
                    new TypeReference<List<MessageReactionDto>>() {}
            );
        } catch (Exception e) {
            System.err.println("Erreur parsing reactionsJson: " + e.getMessage());
            return new ArrayList<>();
        }
    }
}
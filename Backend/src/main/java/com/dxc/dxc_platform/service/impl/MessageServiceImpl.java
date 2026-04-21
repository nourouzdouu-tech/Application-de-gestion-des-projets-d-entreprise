package com.dxc.dxc_platform.service.impl;

import com.dxc.dxc_platform.dto.ConversationDto;
import com.dxc.dxc_platform.dto.MessageDto;
import com.dxc.dxc_platform.entity.Conversation;
import com.dxc.dxc_platform.entity.Message;
import com.dxc.dxc_platform.entity.User;
import com.dxc.dxc_platform.repository.ConversationRepository;
import com.dxc.dxc_platform.repository.MessageRepository;
import com.dxc.dxc_platform.repository.UserRepository;
import com.dxc.dxc_platform.service.MessageService;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class MessageServiceImpl implements MessageService {

    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    private final UserRepository userRepository;

    public MessageServiceImpl(ConversationRepository conversationRepository,
                              MessageRepository messageRepository,
                              UserRepository userRepository) {
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
        this.userRepository = userRepository;
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
    public Message saveWebSocketFileMessage(User sender, User receiver, String clientTempId, Long replyToMessageId) {
        return buildAndSaveMessage(sender, receiver, "", clientTempId, replyToMessageId);
    }

    private Message buildAndSaveMessage(User sender,
                                        User receiver,
                                        String content,
                                        String clientTempId,
                                        Long replyToMessageId) {
        Conversation conv = conversationRepository.findByUser1IdAndUser2Id(sender.getId(), receiver.getId())
                .orElseGet(() -> conversationRepository.findByUser2IdAndUser1Id(sender.getId(), receiver.getId())
                        .orElse(null));

        if (conv == null) {
            conv = new Conversation();
            conv.setUser1(sender);
            conv.setUser2(receiver);
            conv = conversationRepository.save(conv);
        }

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
        dto.setOtherParticipantName(other.getPrenom() + " " + other.getNom());

        Message lastMessage = messageRepository
                .findTopByConversationIdOrderBySentAtDescIdDesc(conv.getId())
                .orElse(null);

        if (lastMessage != null) {
            String preview = lastMessage.getContent() == null ? "" : lastMessage.getContent().trim();
            dto.setLastMessage(preview.isEmpty() ? "[Pièce jointe]" : preview);
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
package com.dxc.dxc_platform.service;

import com.dxc.dxc_platform.dto.ConversationDto;
import com.dxc.dxc_platform.dto.MessageDto;
import com.dxc.dxc_platform.dto.WebSocketMessageDto;
import com.dxc.dxc_platform.entity.Message;
import com.dxc.dxc_platform.entity.User;

import java.util.List;

public interface MessageService {
    ConversationDto getOrCreateConversation(Long otherUserId);
    MessageDto sendMessage(Long receiverId, String content);
    List<ConversationDto> getUserConversations();
    List<MessageDto> getConversationMessages(Long conversationId);
    Message saveWebSocketMessage(User sender, User receiver, String content);
}
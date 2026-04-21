package com.dxc.dxc_platform.dto;

import java.time.LocalDateTime;

public class ConversationDto {
    private Long id;
    private Long otherParticipantId;
    private String otherParticipantName;
    private String lastMessage;
    private LocalDateTime lastMessageTime;
    private int unreadCount;

    public ConversationDto() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getOtherParticipantId() {
        return otherParticipantId;
    }

    public void setOtherParticipantId(Long otherParticipantId) {
        this.otherParticipantId = otherParticipantId;
    }

    public String getOtherParticipantName() {
        return otherParticipantName;
    }

    public void setOtherParticipantName(String otherParticipantName) {
        this.otherParticipantName = otherParticipantName;
    }

    public String getLastMessage() {
        return lastMessage;
    }

    public void setLastMessage(String lastMessage) {
        this.lastMessage = lastMessage;
    }

    public LocalDateTime getLastMessageTime() {
        return lastMessageTime;
    }

    public void setLastMessageTime(LocalDateTime lastMessageTime) {
        this.lastMessageTime = lastMessageTime;
    }

    public int getUnreadCount() {
        return unreadCount;
    }

    public void setUnreadCount(int unreadCount) {
        this.unreadCount = unreadCount;
    }
}
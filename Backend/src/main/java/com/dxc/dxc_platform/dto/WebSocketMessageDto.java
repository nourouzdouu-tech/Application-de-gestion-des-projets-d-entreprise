package com.dxc.dxc_platform.dto;

import java.time.LocalDateTime;
import java.util.List;

public class WebSocketMessageDto {
    private String type;

    private Long id;
    private String clientTempId;
    private String content;

    private Long senderId;
    private String senderName;

    private Long receiverId;
    private String receiverName;

    private LocalDateTime sentAt;
    private boolean read;

    private Long replyToMessageId;

    private Long messageId;
    private String emoji;
    private boolean isAdd;

    private String fileName;
    private String fileType;
    private Long fileSize;
    private String fileData;

    // Statut en ligne/déconnecté
    private boolean online;
    private List<Long> onlineUserIds;
    private int onlineCount;
    private Long userId;

    public WebSocketMessageDto() {}

    // Getters et Setters pour le statut
    public boolean isOnline() { return online; }
    public void setOnline(boolean online) { this.online = online; }

    public List<Long> getOnlineUserIds() { return onlineUserIds; }
    public void setOnlineUserIds(List<Long> onlineUserIds) { this.onlineUserIds = onlineUserIds; }

    public int getOnlineCount() { return onlineCount; }
    public void setOnlineCount(int onlineCount) { this.onlineCount = onlineCount; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    // Getters et Setters existants
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getClientTempId() { return clientTempId; }
    public void setClientTempId(String clientTempId) { this.clientTempId = clientTempId; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public Long getSenderId() { return senderId; }
    public void setSenderId(Long senderId) { this.senderId = senderId; }

    public String getSenderName() { return senderName; }
    public void setSenderName(String senderName) { this.senderName = senderName; }

    public Long getReceiverId() { return receiverId; }
    public void setReceiverId(Long receiverId) { this.receiverId = receiverId; }

    public String getReceiverName() { return receiverName; }
    public void setReceiverName(String receiverName) { this.receiverName = receiverName; }

    public LocalDateTime getSentAt() { return sentAt; }
    public void setSentAt(LocalDateTime sentAt) { this.sentAt = sentAt; }

    public boolean isRead() { return read; }
    public void setRead(boolean read) { this.read = read; }

    public Long getReplyToMessageId() { return replyToMessageId; }
    public void setReplyToMessageId(Long replyToMessageId) { this.replyToMessageId = replyToMessageId; }

    public Long getMessageId() { return messageId; }
    public void setMessageId(Long messageId) { this.messageId = messageId; }

    public String getEmoji() { return emoji; }
    public void setEmoji(String emoji) { this.emoji = emoji; }

    public boolean isAdd() { return isAdd; }
    public void setAdd(boolean add) { isAdd = add; }

    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }

    public String getFileType() { return fileType; }
    public void setFileType(String fileType) { this.fileType = fileType; }

    public Long getFileSize() { return fileSize; }
    public void setFileSize(Long fileSize) { this.fileSize = fileSize; }

    public String getFileData() { return fileData; }
    public void setFileData(String fileData) { this.fileData = fileData; }
}
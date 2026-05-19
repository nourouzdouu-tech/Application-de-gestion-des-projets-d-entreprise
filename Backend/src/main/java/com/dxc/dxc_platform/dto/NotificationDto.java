// com/dxc/dxc_platform/dto/NotificationDto.java
package com.dxc.dxc_platform.dto;

import java.time.LocalDateTime;

public class NotificationDto {
    private Long id;
    private String title;
    private String content;
    private String type;
    private Long userId;
    private String userName;
    private Long createdById;
    private String createdByName;
    private boolean read;
    private LocalDateTime createdAt;
    private String metadata;
    private String actionUrl;

    public NotificationDto() {}

    // Getters
    public Long getId() { return id; }
    public String getTitle() { return title; }
    public String getContent() { return content; }
    public String getType() { return type; }
    public Long getUserId() { return userId; }
    public String getUserName() { return userName; }
    public Long getCreatedById() { return createdById; }
    public String getCreatedByName() { return createdByName; }
    public boolean isRead() { return read; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public String getMetadata() { return metadata; }
    public String getActionUrl() { return actionUrl; }

    // Setters
    public void setId(Long id) { this.id = id; }
    public void setTitle(String title) { this.title = title; }
    public void setContent(String content) { this.content = content; }
    public void setType(String type) { this.type = type; }
    public void setUserId(Long userId) { this.userId = userId; }
    public void setUserName(String userName) { this.userName = userName; }
    public void setCreatedById(Long createdById) { this.createdById = createdById; }
    public void setCreatedByName(String createdByName) { this.createdByName = createdByName; }
    public void setRead(boolean read) { this.read = read; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public void setMetadata(String metadata) { this.metadata = metadata; }
    public void setActionUrl(String actionUrl) { this.actionUrl = actionUrl; }
}
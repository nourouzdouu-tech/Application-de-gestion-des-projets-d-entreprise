// com/dxc/dxc_platform/entity/Notification.java
package com.dxc.dxc_platform.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "notifications")
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(nullable = false)
    private String type;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne
    @JoinColumn(name = "created_by_id")
    private User createdBy;

    @Column(nullable = false)
    private boolean read = false;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "metadata", columnDefinition = "TEXT")
    private String metadata;

    @Column(name = "action_url")
    private String actionUrl;

    public Notification() {
        this.createdAt = LocalDateTime.now();
    }

    public Notification(String title, String content, String type, User user, User createdBy, String actionUrl, String metadata) {
        this.title = title;
        this.content = content;
        this.type = type;
        this.user = user;
        this.createdBy = createdBy;
        this.actionUrl = actionUrl;
        this.metadata = metadata;
        this.read = false;
        this.createdAt = LocalDateTime.now();
    }

    // Getters
    public Long getId() { return id; }
    public String getTitle() { return title; }
    public String getContent() { return content; }
    public String getType() { return type; }
    public User getUser() { return user; }
    public User getCreatedBy() { return createdBy; }
    public boolean isRead() { return read; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public String getMetadata() { return metadata; }
    public String getActionUrl() { return actionUrl; }

    // Setters
    public void setId(Long id) { this.id = id; }
    public void setTitle(String title) { this.title = title; }
    public void setContent(String content) { this.content = content; }
    public void setType(String type) { this.type = type; }
    public void setUser(User user) { this.user = user; }
    public void setCreatedBy(User createdBy) { this.createdBy = createdBy; }
    public void setRead(boolean read) { this.read = read; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public void setMetadata(String metadata) { this.metadata = metadata; }
    public void setActionUrl(String actionUrl) { this.actionUrl = actionUrl; }
}
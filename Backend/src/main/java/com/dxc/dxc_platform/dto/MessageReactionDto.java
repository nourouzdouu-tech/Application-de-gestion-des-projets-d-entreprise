package com.dxc.dxc_platform.dto;

public class MessageReactionDto {
    private String emoji;
    private Long userId;
    private String userName;

    public MessageReactionDto() {
    }

    public String getEmoji() {
        return emoji;
    }

    public void setEmoji(String emoji) {
        this.emoji = emoji;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }
}
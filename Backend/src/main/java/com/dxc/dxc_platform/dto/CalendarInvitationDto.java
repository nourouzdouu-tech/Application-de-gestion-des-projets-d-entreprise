package com.dxc.dxc_platform.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.List;

public class CalendarInvitationDto {

    @NotBlank
    private String title;

    private String description;

    @NotNull
    private LocalDate date;

    private String startTime;
    private String endTime;

    private Long projectId;
    private String projectName;

    @NotNull
    private Long ownerId;
    private String ownerName;

    @NotNull
    private List<Long> invitedUserIds;

    // Getters et Setters
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }

    public String getStartTime() { return startTime; }
    public void setStartTime(String startTime) { this.startTime = startTime; }

    public String getEndTime() { return endTime; }
    public void setEndTime(String endTime) { this.endTime = endTime; }

    public Long getProjectId() { return projectId; }
    public void setProjectId(Long projectId) { this.projectId = projectId; }

    public String getProjectName() { return projectName; }
    public void setProjectName(String projectName) { this.projectName = projectName; }

    public Long getOwnerId() { return ownerId; }
    public void setOwnerId(Long ownerId) { this.ownerId = ownerId; }

    public String getOwnerName() { return ownerName; }
    public void setOwnerName(String ownerName) { this.ownerName = ownerName; }

    public List<Long> getInvitedUserIds() { return invitedUserIds; }
    public void setInvitedUserIds(List<Long> invitedUserIds) { this.invitedUserIds = invitedUserIds; }
}
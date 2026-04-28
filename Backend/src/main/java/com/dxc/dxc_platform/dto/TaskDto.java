package com.dxc.dxc_platform.dto;

import com.dxc.dxc_platform.enums.Priority;
import com.dxc.dxc_platform.enums.Status;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class TaskDto {

    private Long id;
    private String title;
    private String description;
    private Status status;
    private LocalDateTime createdAt;

    // Nouveau modèle
    private LocalDate startDate;
    private Integer criticite;
    private Integer dureeEstimee;
    private LocalDate estimatedEndDate;

    private Long assignedToId;
    private String assignedToName;
    private Long projectId;
    private String projectName;
    private boolean deleted;
    private Priority priority;

    // ✅ Nouveau
    private Boolean rejected;
    private String rejectionComment;

    public TaskDto() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }

    public Integer getCriticite() { return criticite; }
    public void setCriticite(Integer criticite) { this.criticite = criticite; }

    public Integer getDureeEstimee() { return dureeEstimee; }
    public void setDureeEstimee(Integer dureeEstimee) { this.dureeEstimee = dureeEstimee; }

    public LocalDate getEstimatedEndDate() { return estimatedEndDate; }
    public void setEstimatedEndDate(LocalDate estimatedEndDate) { this.estimatedEndDate = estimatedEndDate; }

    public Long getAssignedToId() { return assignedToId; }
    public void setAssignedToId(Long assignedToId) { this.assignedToId = assignedToId; }

    public String getAssignedToName() { return assignedToName; }
    public void setAssignedToName(String assignedToName) { this.assignedToName = assignedToName; }

    public Long getProjectId() { return projectId; }
    public void setProjectId(Long projectId) { this.projectId = projectId; }

    public String getProjectName() { return projectName; }
    public void setProjectName(String projectName) { this.projectName = projectName; }

    public boolean isDeleted() { return deleted; }
    public void setDeleted(boolean deleted) { this.deleted = deleted; }

    public Priority getPriority() { return priority; }
    public void setPriority(Priority priority) { this.priority = priority; }

    public Boolean getRejected() { return rejected; }
    public void setRejected(Boolean rejected) { this.rejected = rejected; }

    public String getRejectionComment() { return rejectionComment; }
    public void setRejectionComment(String rejectionComment) { this.rejectionComment = rejectionComment; }
}
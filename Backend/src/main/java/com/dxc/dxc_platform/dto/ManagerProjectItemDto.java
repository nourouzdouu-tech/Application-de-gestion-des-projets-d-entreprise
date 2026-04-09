package com.dxc.dxc_platform.dto;

import com.dxc.dxc_platform.enums.ProjectStatus;

import java.time.LocalDateTime;

public class ManagerProjectItemDto {

    private Long id;
    private String projectName;
    private ProjectStatus status;
    private String client;
    private String managerName;

    private Long chefProjetId;
    private String chefProjetName;

    private String managerComment;
    private LocalDateTime createdAt;
    private LocalDateTime reviewedAt;

    public ManagerProjectItemDto() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getProjectName() {
        return projectName;
    }

    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }

    public ProjectStatus getStatus() {
        return status;
    }

    public void setStatus(ProjectStatus status) {
        this.status = status;
    }

    public String getClient() {
        return client;
    }

    public void setClient(String client) {
        this.client = client;
    }

    public String getManagerName() {
        return managerName;
    }

    public void setManagerName(String managerName) {
        this.managerName = managerName;
    }

    public Long getChefProjetId() {
        return chefProjetId;
    }

    public void setChefProjetId(Long chefProjetId) {
        this.chefProjetId = chefProjetId;
    }

    public String getChefProjetName() {
        return chefProjetName;
    }

    public void setChefProjetName(String chefProjetName) {
        this.chefProjetName = chefProjetName;
    }

    public String getManagerComment() {
        return managerComment;
    }

    public void setManagerComment(String managerComment) {
        this.managerComment = managerComment;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getReviewedAt() {
        return reviewedAt;
    }

    public void setReviewedAt(LocalDateTime reviewedAt) {
        this.reviewedAt = reviewedAt;
    }
}
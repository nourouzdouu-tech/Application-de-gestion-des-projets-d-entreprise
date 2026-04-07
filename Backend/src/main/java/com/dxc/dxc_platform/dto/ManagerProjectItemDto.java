package com.dxc.dxc_platform.dto;

import com.dxc.dxc_platform.enums.ProjectStatus;

import java.time.LocalDateTime;
//c pour afficher les projets
public class ManagerProjectItemDto {

    private Long id;
    private String projectName;
    private ProjectStatus status;
    private String client;
    private String managerName;
    private LocalDateTime createdAt;

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

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
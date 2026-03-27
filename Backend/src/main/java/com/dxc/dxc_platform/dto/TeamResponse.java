package com.dxc.dxc_platform.dto;

import java.time.LocalDateTime;
import java.util.List;

public class TeamResponse {

    private Long id;
    private String name;
    private String description;
    private Long projectManagerId;
    private String projectManagerName;
    private List<MemberResponse> members;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public TeamResponse() {
    }

    public TeamResponse(Long id, String name, String description, Long projectManagerId,
                        String projectManagerName, List<MemberResponse> members,
                        LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.projectManagerId = projectManagerId;
        this.projectManagerName = projectManagerName;
        this.members = members;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public Long getProjectManagerId() {
        return projectManagerId;
    }

    public String getProjectManagerName() {
        return projectManagerName;
    }

    public List<MemberResponse> getMembers() {
        return members;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setProjectManagerId(Long projectManagerId) {
        this.projectManagerId = projectManagerId;
    }

    public void setProjectManagerName(String projectManagerName) {
        this.projectManagerName = projectManagerName;
    }

    public void setMembers(List<MemberResponse> members) {
        this.members = members;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
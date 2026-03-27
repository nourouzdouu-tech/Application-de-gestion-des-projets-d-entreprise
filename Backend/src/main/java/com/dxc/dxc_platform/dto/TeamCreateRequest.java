package com.dxc.dxc_platform.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class TeamCreateRequest {

    @NotBlank(message = "Le nom de l'équipe est obligatoire")
    @Size(max = 100, message = "Le nom de l'équipe ne doit pas dépasser 100 caractères")
    private String name;

    @Size(max = 255, message = "La description ne doit pas dépasser 255 caractères")
    private String description;

    private Long projectManagerId;

    public TeamCreateRequest() {
    }

    public TeamCreateRequest(String name, String description, Long projectManagerId) {
        this.name = name;
        this.description = description;
        this.projectManagerId = projectManagerId;
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

    public void setName(String name) {
        this.name = name;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setProjectManagerId(Long projectManagerId) {
        this.projectManagerId = projectManagerId;
    }
}
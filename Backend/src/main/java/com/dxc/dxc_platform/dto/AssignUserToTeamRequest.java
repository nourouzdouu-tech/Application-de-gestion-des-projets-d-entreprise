package com.dxc.dxc_platform.dto;

import jakarta.validation.constraints.NotNull;

public class AssignUserToTeamRequest {

    @NotNull(message = "L'identifiant de l'utilisateur est obligatoire")
    private Long userId;

    public AssignUserToTeamRequest() {
    }

    public AssignUserToTeamRequest(Long userId) {
        this.userId = userId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }
}
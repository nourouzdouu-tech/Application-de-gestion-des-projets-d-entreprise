package com.dxc.dxc_platform.dto;

import jakarta.validation.constraints.NotBlank;

public class PermissionDto {

    public record CreatePermissionRequest(
            @NotBlank String nom,
            String description
    ) {}

    public record PermissionResponse(
            Long id,
            String nom,
            String description
    ) {}

    public record UpdatePermissionRequest(
            @NotBlank(message = "Le nom est obligatoire")
            String nom,
            String description
    ) {}
}
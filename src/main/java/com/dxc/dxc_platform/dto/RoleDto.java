package com.dxc.dxc_platform.dto;

import jakarta.validation.constraints.NotBlank;

import java.util.Set;

public class RoleDto {

    public record CreateRequest(
            @NotBlank String nom,
            String description,
            Set<String> permissionCodes
    ) {}

    public record UpdateRequest(
            @NotBlank String nom,
            String description
    ) {}

    public record UpdatePermissionsRequest(
            Set<String> permissionCodes
    ) {}

    public record Response(
            Long id,
            String nom,
            String description,
            boolean active,
            Set<PermissionDto.Summary> permissions
    ) {}

    public record Summary(
            Long id,
            String nom
    ) {}
}
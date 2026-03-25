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
            String nom,
            String description,
            Boolean active,
            Set<Long> permissionIds
    ) {}

    public record UpdatePermissionsRequest(
            Set<String> permissionCodes
    ) {}

    public record Response(
            Long id,
            String nom,
            String description,
            boolean active,
            Set<PermissionDto.Summary> permissions,
            Integer usersCount
    ) {}

    public record Summary(
            Long id,
            String nom
    ) {}
}
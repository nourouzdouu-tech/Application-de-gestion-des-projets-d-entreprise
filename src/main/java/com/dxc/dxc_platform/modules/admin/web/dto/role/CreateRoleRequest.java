package com.dxc.dxc_platform.modules.admin.web.dto.role;

import jakarta.validation.constraints.NotBlank;

import java.util.Set;

public record CreateRoleRequest(
        @NotBlank String nom,
        String description,
        Set<String> permissionCodes
) {}
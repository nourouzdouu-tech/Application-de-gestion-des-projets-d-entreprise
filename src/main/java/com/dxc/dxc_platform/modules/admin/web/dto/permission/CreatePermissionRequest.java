package com.dxc.dxc_platform.modules.admin.web.dto.permission;

import jakarta.validation.constraints.NotBlank;

public record CreatePermissionRequest(
        @NotBlank String nom,
        String description
) {}
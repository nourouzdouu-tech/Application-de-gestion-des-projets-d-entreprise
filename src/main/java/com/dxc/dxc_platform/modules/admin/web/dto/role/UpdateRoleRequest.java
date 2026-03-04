package com.dxc.dxc_platform.modules.admin.web.dto.role;

import jakarta.validation.constraints.NotBlank;

public record UpdateRoleRequest(
        @NotBlank String nom,
        String description
) {}
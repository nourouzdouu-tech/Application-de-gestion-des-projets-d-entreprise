package com.dxc.dxc_platform.modules.admin.web.dto.permission;

import jakarta.validation.constraints.NotBlank;

public record UpdatePermissionRequest(

        @NotBlank(message = "Le nom est obligatoire")
        String nom,

        String description
) {}
package com.dxc.dxc_platform.modules.admin.web.dto.permission;

public record PermissionResponse(
        Long id,
        String nom,
        String description
) {}
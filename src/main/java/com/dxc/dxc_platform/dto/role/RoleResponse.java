package com.dxc.dxc_platform.dto.role;

import java.util.Set;

public record RoleResponse(
        Long id,
        String nom,
        String description,
        boolean active,
        Set<String> permissions
) {}
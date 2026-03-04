package com.dxc.dxc_platform.modules.admin.web.dto.user;

import com.dxc.dxc_platform.modules.admin.domain.enums.Genre;

import java.util.Set;

public record UserResponse(
        Long id,
        String email,
        String prenom,
        String nom,
        Genre genre,
        boolean enabled,
        int failedAttempts,
        boolean mustChangePassword,
        Set<String> roles
) {}
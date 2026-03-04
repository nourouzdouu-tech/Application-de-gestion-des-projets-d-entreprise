package com.dxc.dxc_platform.modules.auth.web.dto;

import java.util.Set;

public record AuthResponse(
        String accessToken,
        String tokenType,
        String email,
        String prenom,
        String nom,
        Set<String> roles,
        String redirectTo,
        boolean mustChangePassword   // true → l'utilisateur doit changer son mot de passe
) {}
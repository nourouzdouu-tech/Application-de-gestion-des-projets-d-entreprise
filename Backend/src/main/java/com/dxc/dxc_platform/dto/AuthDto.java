package com.dxc.dxc_platform.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.Set;

public class AuthDto {

    public record LoginRequest(
            @NotBlank @Email String email,
            @NotBlank String password
    ) {}

    public record ChangePasswordRequest(
            @NotBlank(message = "L'ancien mot de passe est obligatoire")
            String oldPassword,

            @NotBlank(message = "Le nouveau mot de passe est obligatoire")
            @Size(min = 6, message = "Le mot de passe doit contenir au moins 6 caractères")
            String newPassword,

            @NotBlank(message = "La confirmation est obligatoire")
            String confirmPassword
    ) {}

    public record Response(
            String accessToken,
            String tokenType,
            Long id,                     // ← AJOUTER
            String email,
            String prenom,
            String nom,
            Set<String> roles,
            String redirectTo,
            boolean mustChangePassword
    ) {}
    public record UpdateProfileRequest(
            String nom,
            String prenom,
            String email
    ) {}
}
package com.dxc.dxc_platform.dto;

import com.dxc.dxc_platform.enums.Genre;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Set;

public class UserDto {

    public record CreateRequest(
            @NotBlank String prenom,
            @NotBlank String nom,
            @Email @NotBlank String email,
            @NotNull Genre genre,
            @NotEmpty Set<String> roleCodes,
            @NotBlank String password,
            @NotNull Long profileId
    ) {}

    public record UpdateRequest(
            @NotBlank String prenom,
            @NotBlank String nom,
            @Email @NotBlank String email,
            @NotNull Genre genre,
            @NotEmpty Set<String> roleCodes,
            @NotNull Long profileId
    ) {}

    public record ResetPasswordRequest(
            Long userId,
            String tempPassword
    ) {}

    public record ResetPasswordResponse(
            Long userId,
            String tempPassword,
            boolean mustChangePassword,
            LocalDateTime expiresAt,      // ✅ Date d'expiration
            long expiresInMinutes
    ) {}

    // ✅ NOUVEAU : utilisé par l'endpoint PATCH /{id}/change-password
    public record ChangePasswordRequest(
            @NotBlank String newPassword
    ) {}

    public record Response(
            Long id,
            String email,
            String prenom,
            String nom,
            Genre genre,
            int failedAttempts,
            boolean locked,
            boolean mustChangePassword,
            Set<RoleDto.Summary> roles,
            Long profileId,
            String profileLibelle,
            BigDecimal tjm
    ) {}

    public record Summary(
            Long id,
            String email,
            String prenom,
            String nom
    ) {}
}
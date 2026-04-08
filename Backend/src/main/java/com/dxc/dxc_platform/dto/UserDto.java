package com.dxc.dxc_platform.dto;

import com.dxc.dxc_platform.enums.Genre;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.Set;

public class UserDto {

    public record CreateRequest(
            @NotBlank String prenom,
            @NotBlank String nom,
            @Email @NotBlank String email,
            @NotNull Genre genre,
            @NotBlank String roleCode,
            @NotBlank String password,
            @NotNull Long profileId
    ) {}

    public record UpdateRequest(
            @NotBlank String prenom,
            @NotBlank String nom,
            @Email @NotBlank String email,
            @NotNull Genre genre,
            @NotBlank String roleCode,
            @NotNull Long profileId
    ) {}

    public record ResetPasswordRequest(
            String tempPassword
    ) {}

    public record ResetPasswordResponse(
            Long userId,
            String tempPassword,
            boolean mustChangePassword
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
            String profileLibelle
    ) {}

    public record Summary(
            Long id,
            String email,
            String prenom,
            String nom
    ) {}
}
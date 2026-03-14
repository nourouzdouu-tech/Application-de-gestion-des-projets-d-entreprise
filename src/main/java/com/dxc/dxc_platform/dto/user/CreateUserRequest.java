package com.dxc.dxc_platform.dto.user;

import com.dxc.dxc_platform.enums.Genre;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateUserRequest(
        @NotBlank String prenom,
        @NotBlank String nom,
        @Email @NotBlank String email,
        @NotNull Genre genre,
        @NotBlank String roleCode,
        @NotBlank String password
) {}
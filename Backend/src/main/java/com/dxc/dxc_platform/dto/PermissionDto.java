package com.dxc.dxc_platform.dto;

import jakarta.validation.constraints.NotBlank;

public class PermissionDto {

    public record CreateRequest(
            @NotBlank String nom,
            String description
    ) {}

    public record UpdateRequest(
            @NotBlank String nom,
            String description
    ) {}

    public record Response(
            Long id,
            String nom,
            String description
    ) {}

    public record Summary(
            Long id,
            String nom
    ) {}
}
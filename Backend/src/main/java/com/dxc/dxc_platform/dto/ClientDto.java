package com.dxc.dxc_platform.dto;

import jakarta.validation.constraints.NotBlank;
import java.util.List;

public class ClientDto {

    public record RepresentantDto(
            Long id,
            String nom,
            String email,
            String telephone
    ) {}

    public record Response(
            Long id,
            String nom,
            int representantsCount,
            List<RepresentantDto> representants
    ) {}

    public record CreateRequest(
            @NotBlank String nom,
            List<CreateRepresentantRequest> representants
    ) {}

    public record CreateRepresentantRequest(
            @NotBlank String nom,
            @NotBlank String email,
            @NotBlank String telephone
    ) {}

    public record UpdateRequest(
            @NotBlank String nom,
            List<UpdateRepresentantRequest> representants
    ) {}

    public record UpdateRepresentantRequest(
            Long id,
            String nom,
            String email,
            String telephone
    ) {}
}
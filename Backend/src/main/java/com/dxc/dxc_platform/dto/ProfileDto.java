package com.dxc.dxc_platform.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class ProfileDto {

    private Long id;

    @NotBlank(message = "Le libellé du profil est obligatoire")
    @Size(max = 100, message = "Le libellé du profil ne doit pas dépasser 100 caractères")
    private String libelle;

    @NotNull(message = "Le TJM est obligatoire")
    @DecimalMin(value = "0.0", inclusive = false, message = "Le TJM doit être supérieur à 0")
    private BigDecimal tjm;

    private Boolean deleted;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public ProfileDto() {
    }

    public Long getId() {
        return id;
    }

    public String getLibelle() {
        return libelle;
    }

    public BigDecimal getTjm() {
        return tjm;
    }

    public Boolean getDeleted() {
        return deleted;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setLibelle(String libelle) {
        this.libelle = libelle;
    }

    public void setTjm(BigDecimal tjm) {
        this.tjm = tjm;
    }

    public void setDeleted(Boolean deleted) {
        this.deleted = deleted;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
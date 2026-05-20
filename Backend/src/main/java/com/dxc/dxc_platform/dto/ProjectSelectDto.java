package com.dxc.dxc_platform.dto.reporting;

// ProjectSelectDto.java
public class ProjectSelectDto {
    private Long id;
    private String nom;

    public ProjectSelectDto() {}
    public ProjectSelectDto(Long id, String nom) {
        this.id = id;
        this.nom = nom;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }
}
package com.dxc.dxc_platform.modules.admin.domain.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "permissions")
public class Permission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 60, nullable = false, unique = true)
    private String nom;

    @Column(length = 255)
    private String description;

    // Constructeur vide obligatoire pour JPA
    public Permission() {}

    public Permission(String nom, String description) {
        this.nom = nom;
        this.description = description;
    }

    // Getters & Setters

    public Long getId() {
        return id;
    }

    public String getNom() {
        return nom;
    }

    public void setNom(String nom) {
        this.nom = nom;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
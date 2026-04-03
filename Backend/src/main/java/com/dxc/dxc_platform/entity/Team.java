package com.dxc.dxc_platform.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "teams")
public class Team {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;  // Retirer unique = true car un chef peut avoir plusieurs équipes

    @Column(length = 255)
    private String description;

    // Changement: Un chef de projet peut gérer plusieurs équipes
    @ManyToOne  // Au lieu de @OneToOne
    @JoinColumn(name = "project_manager_id")  // Retirer unique = true
    private User projectManager;

    @OneToMany(mappedBy = "team")
    private List<User> members = new ArrayList<>();

    @Column(nullable = false)
    private boolean deleted = false;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    // Ajouter une contrainte d'unicité composée pour le nom par manager
    // Cette contrainte sera gérée au niveau applicatif

    public Team() {
    }

    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // Getters et setters
    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public User getProjectManager() {
        return projectManager;
    }

    public List<User> getMembers() {
        return members;
    }

    public boolean isDeleted() {
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

    public void setName(String name) {
        this.name = name;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setProjectManager(User projectManager) {
        this.projectManager = projectManager;
    }

    public void setMembers(List<User> members) {
        this.members = members;
    }

    public void setDeleted(boolean deleted) {
        this.deleted = deleted;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
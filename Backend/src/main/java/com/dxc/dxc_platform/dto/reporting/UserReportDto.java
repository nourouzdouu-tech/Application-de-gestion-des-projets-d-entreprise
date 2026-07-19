package com.dxc.dxc_platform.dto.reporting;

import java.util.List;

public class UserReportDto {
    private Long id;
    private String email;
    private String nom;
    private String prenom;
    private String role;
    private boolean active;
    private boolean locked;
    private int failedAttempts;
    private int passwordResetCount;
    private String profileLibelle;

    public UserReportDto() {}

    public UserReportDto(Long id, String email, String nom, String prenom, String role,
                         boolean active, boolean locked, int failedAttempts,
                         int passwordResetCount, String profileLibelle) {
        this.id = id;
        this.email = email;
        this.nom = nom;
        this.prenom = prenom;
        this.role = role;
        this.active = active;
        this.locked = locked;
        this.failedAttempts = failedAttempts;
        this.passwordResetCount = passwordResetCount;
        this.profileLibelle = profileLibelle;
    }

    // Getters & Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }

    public String getPrenom() { return prenom; }
    public void setPrenom(String prenom) { this.prenom = prenom; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public boolean isLocked() { return locked; }
    public void setLocked(boolean locked) { this.locked = locked; }

    public int getFailedAttempts() { return failedAttempts; }
    public void setFailedAttempts(int failedAttempts) { this.failedAttempts = failedAttempts; }

    public int getPasswordResetCount() { return passwordResetCount; }
    public void setPasswordResetCount(int passwordResetCount) { this.passwordResetCount = passwordResetCount; }

    public String getProfileLibelle() { return profileLibelle; }
    public void setProfileLibelle(String profileLibelle) { this.profileLibelle = profileLibelle; }
}
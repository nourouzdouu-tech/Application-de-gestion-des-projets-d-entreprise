package com.dxc.dxc_platform.dto;

import com.dxc.dxc_platform.enums.ProjectStatus;
import java.time.LocalDate;

public class ProjectReportDto {
    private Long id;
    private String nom;
    private String description;
    private ProjectStatus status;
    private LocalDate dateDebut;
    private LocalDate dateFinEstimee;
    private LocalDate dateFinReelle;
    private String chefProjetNom;
    private String chefProjetPrenom;
    private String teamNom;
    private Long teamId;
    private boolean overdue;
    private long joursRetard;

    public ProjectReportDto() {}

    public ProjectReportDto(Long id, String nom, String description, ProjectStatus status,
                            LocalDate dateDebut, LocalDate dateFinEstimee, LocalDate dateFinReelle,
                            String chefProjetNom, String chefProjetPrenom,
                            String teamNom, Long teamId) {
        this.id = id;
        this.nom = nom;
        this.description = description;
        this.status = status;
        this.dateDebut = dateDebut;
        this.dateFinEstimee = dateFinEstimee;
        this.dateFinReelle = dateFinReelle;
        this.chefProjetNom = chefProjetNom;
        this.chefProjetPrenom = chefProjetPrenom;
        this.teamNom = teamNom;
        this.teamId = teamId;

        LocalDate today = LocalDate.now();
        if (dateFinEstimee != null && today.isAfter(dateFinEstimee)
                && status != ProjectStatus.CLOTURE) {
            this.overdue = true;
            this.joursRetard = java.time.temporal.ChronoUnit.DAYS.between(dateFinEstimee, today);
        }
    }

    // Getters & Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public ProjectStatus getStatus() { return status; }
    public void setStatus(ProjectStatus status) { this.status = status; }

    public LocalDate getDateDebut() { return dateDebut; }
    public void setDateDebut(LocalDate dateDebut) { this.dateDebut = dateDebut; }

    public LocalDate getDateFinEstimee() { return dateFinEstimee; }
    public void setDateFinEstimee(LocalDate dateFinEstimee) { this.dateFinEstimee = dateFinEstimee; }

    public LocalDate getDateFinReelle() { return dateFinReelle; }
    public void setDateFinReelle(LocalDate dateFinReelle) { this.dateFinReelle = dateFinReelle; }

    public String getChefProjetNom() { return chefProjetNom; }
    public void setChefProjetNom(String chefProjetNom) { this.chefProjetNom = chefProjetNom; }

    public String getChefProjetPrenom() { return chefProjetPrenom; }
    public void setChefProjetPrenom(String chefProjetPrenom) { this.chefProjetPrenom = chefProjetPrenom; }

    public String getTeamNom() { return teamNom; }
    public void setTeamNom(String teamNom) { this.teamNom = teamNom; }

    public Long getTeamId() { return teamId; }
    public void setTeamId(Long teamId) { this.teamId = teamId; }

    public boolean isOverdue() { return overdue; }
    public void setOverdue(boolean overdue) { this.overdue = overdue; }

    public long getJoursRetard() { return joursRetard; }
    public void setJoursRetard(long joursRetard) { this.joursRetard = joursRetard; }
}
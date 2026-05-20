package com.dxc.dxc_platform.dto.reporting;

import com.dxc.dxc_platform.enums.Priority;
import com.dxc.dxc_platform.enums.Status;
import java.time.LocalDate;

public class TaskReportDto {
    private Long id;
    private String titre;
    private String description;
    private Status status;
    private Priority priority;
    private LocalDate dateEcheance;
    private long joursRetard;
    private String assigneNom;
    private String assignePrenom;
    private Long projectId;
    private String projectNom;

    public TaskReportDto() {}

    public TaskReportDto(Long id, String titre, String description, Status status, Priority priority,
                         LocalDate dateEcheance, String assigneNom, String assignePrenom,
                         Long projectId, String projectNom) {
        this.id = id;
        this.titre = titre;
        this.description = description;
        this.status = status;
        this.priority = priority;
        this.dateEcheance = dateEcheance;
        this.assigneNom = assigneNom;
        this.assignePrenom = assignePrenom;
        this.projectId = projectId;
        this.projectNom = projectNom;

        if (dateEcheance != null) {
            LocalDate today = LocalDate.now();
            if (today.isAfter(dateEcheance)) {
                this.joursRetard = java.time.temporal.ChronoUnit.DAYS.between(dateEcheance, today);
            }
        }
    }

    // Getters & Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTitre() { return titre; }
    public void setTitre(String titre) { this.titre = titre; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }

    public Priority getPriority() { return priority; }
    public void setPriority(Priority priority) { this.priority = priority; }

    public LocalDate getDateEcheance() { return dateEcheance; }
    public void setDateEcheance(LocalDate dateEcheance) { this.dateEcheance = dateEcheance; }

    public long getJoursRetard() { return joursRetard; }
    public void setJoursRetard(long joursRetard) { this.joursRetard = joursRetard; }

    public String getAssigneNom() { return assigneNom; }
    public void setAssigneNom(String assigneNom) { this.assigneNom = assigneNom; }

    public String getAssignePrenom() { return assignePrenom; }
    public void setAssignePrenom(String assignePrenom) { this.assignePrenom = assignePrenom; }

    public Long getProjectId() { return projectId; }
    public void setProjectId(Long projectId) { this.projectId = projectId; }

    public String getProjectNom() { return projectNom; }
    public void setProjectNom(String projectNom) { this.projectNom = projectNom; }
}
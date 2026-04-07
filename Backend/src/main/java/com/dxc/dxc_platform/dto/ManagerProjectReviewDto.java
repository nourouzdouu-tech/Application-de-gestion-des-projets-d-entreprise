package com.dxc.dxc_platform.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
//Form asighantion
public class ManagerProjectReviewDto {

    @NotNull(message = "L'id du projet est obligatoire")
    private Long projectId;

    @NotNull(message = "L'id du chef de projet est obligatoire")
    private Long chefProjetId;

    @NotBlank(message = "Le commentaire est obligatoire")
    private String commentaire;

    @NotNull(message = "La décision est obligatoire")
    private Decision decision;

    public enum Decision {
        VALIDER,
        REJETER
    }

    public ManagerProjectReviewDto() {
    }

    public Long getProjectId() {
        return projectId;
    }

    public void setProjectId(Long projectId) {
        this.projectId = projectId;
    }

    public Long getChefProjetId() {
        return chefProjetId;
    }

    public void setChefProjetId(Long chefProjetId) {
        this.chefProjetId = chefProjetId;
    }

    public String getCommentaire() {
        return commentaire;
    }

    public void setCommentaire(String commentaire) {
        this.commentaire = commentaire;
    }

    public Decision getDecision() {
        return decision;
    }

    public void setDecision(Decision decision) {
        this.decision = decision;
    }
}
package com.dxc.dxc_platform.service;

import com.dxc.dxc_platform.entity.Project;
import com.dxc.dxc_platform.enums.ProjectStatus;
import com.dxc.dxc_platform.repository.ProjectRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class RiskScheduler {

    private final PredictionService predictionService;
    private final ProjectRepository projectRepository;

    public RiskScheduler(PredictionService predictionService,
                         ProjectRepository projectRepository) {
        this.predictionService = predictionService;
        this.projectRepository = projectRepository;
    }

    @Scheduled(fixedRate = 6 * 60 * 60 * 1000) // toutes les 6 heures
    public void refreshAllRisks() {
        List<Project> activeProjects =
                projectRepository.findByStatus(ProjectStatus.EN_COURS);
        for (Project project : activeProjects) {
            try {
                predictionService.predictForProject(project.getId());
            } catch (Exception e) {
                System.err.println("Erreur prédiction projet "
                        + project.getId() + " : " + e.getMessage());
            }
        }
    }
}
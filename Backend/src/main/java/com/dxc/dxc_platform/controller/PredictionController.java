package com.dxc.dxc_platform.controller;

import com.dxc.dxc_platform.dto.RiskPredictionResult;
import com.dxc.dxc_platform.dto.TeamRecommendationResult;
import com.dxc.dxc_platform.service.PredictionService;
import com.dxc.dxc_platform.service.TeamRecommendationService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/projects")
public class PredictionController {

    private final PredictionService predictionService;
    private final TeamRecommendationService teamRecommendationService;

    // ✅ Constructeur simple — pas de @Lazy ici
    public PredictionController(PredictionService predictionService,
                                TeamRecommendationService teamRecommendationService) {
        this.predictionService = predictionService;
        this.teamRecommendationService = teamRecommendationService;
    }

    @GetMapping("/{projectId}/risk")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<RiskPredictionResult> getProjectRisk(
            @PathVariable Long projectId) {
        return ResponseEntity.ok(predictionService.predictForProject(projectId));
    }

    @PostMapping("/{projectId}/risk/refresh")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<RiskPredictionResult> forceRefreshRisk(
            @PathVariable Long projectId) {
        return ResponseEntity.ok(predictionService.forceRefreshForProject(projectId));
    }

    @GetMapping("/risks")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<RiskPredictionResult>> getAllRisks() {
        return ResponseEntity.ok(predictionService.predictAllActiveProjects());
    }

    @GetMapping("/{projectId}/team-recommendation")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<TeamRecommendationResult> getTeamRecommendation(
            @PathVariable Long projectId) {
        return ResponseEntity.ok(
                teamRecommendationService.recommendForDelayedProject(projectId));
    }
}
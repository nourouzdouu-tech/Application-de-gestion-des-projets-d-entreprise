package com.dxc.dxc_platform.controller;

import com.dxc.dxc_platform.dto.*;
import com.dxc.dxc_platform.enums.ProjectStatus;
import com.dxc.dxc_platform.service.PredictionService;
import com.dxc.dxc_platform.service.ProjectService;
import com.dxc.dxc_platform.service.TeamRecommendationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import com.dxc.dxc_platform.dto.ManagerSelectDto;
import com.dxc.dxc_platform.dto.TeamRecommendationResult;
import com.dxc.dxc_platform.service.UserAdminService;
import com.dxc.dxc_platform.dto.RiskPredictionResult;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/projects")
public class ProjectController {

    private final ProjectService projectService;
    private final UserAdminService userAdminService;
    private final PredictionService predictionService;
    private final TeamRecommendationService teamRecommendationService;

    public ProjectController(ProjectService projectService,
                             UserAdminService userAdminService,
                             PredictionService predictionService,
                             TeamRecommendationService teamRecommendationService) {
        this.projectService = projectService;
        this.userAdminService = userAdminService;
        this.predictionService = predictionService;
        this.teamRecommendationService = teamRecommendationService;
    }

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ProjectDto> createProject(@Valid @RequestBody ProjectDto request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(projectService.createProject(request));
    }

    @PutMapping("/{projectId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ProjectDto> updateProject(@PathVariable Long projectId,
                                                    @Valid @RequestBody ProjectDto request) {
        return ResponseEntity.ok(projectService.updateProject(projectId, request));
    }

    @GetMapping("/{projectId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ProjectDto> getProjectById(@PathVariable Long projectId) {
        return ResponseEntity.ok(projectService.getProjectById(projectId));
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<ProjectDto>> getAllProjects(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) ProjectStatus status) {
        return ResponseEntity.ok(projectService.getAllProjects(query, status));
    }

    @GetMapping("/my-projects")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<ProjectDto>> getMyProjects(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) ProjectStatus status) {
        return ResponseEntity.ok(projectService.getMyProjects(query, status));
    }

    @PatchMapping("/{projectId}/deleted")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ProjectDto> setDeletedStatus(@PathVariable Long projectId,
                                                       @RequestParam boolean deleted) {
        return ResponseEntity.ok(projectService.setDeletedStatus(projectId, deleted));
    }
    @GetMapping("/managers/select")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<ManagerSelectDto>> getManagersForSelect() {
        return ResponseEntity.ok(userAdminService.getManagersForSelect());
    }

    @GetMapping("/manager")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<ManagerProjectItemDto>> getManagerProjects() {
        return ResponseEntity.ok(projectService.getManagerProjects());
    }

    @GetMapping("/manager/chefs-projet/select")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<UserDto.Summary>> getChefsProjetForSelect() {
        return ResponseEntity.ok(projectService.getChefsProjetForSelect());
    }

    @PostMapping("/manager/review")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ProjectDto> reviewProjectByManager(
            @Valid @RequestBody ManagerProjectReviewDto request) {
        return ResponseEntity.ok(projectService.reviewProjectByManager(request));
    }
    @PatchMapping("/{projectId}/assign-team")
    @PreAuthorize("hasRole('CHEF_PROJET')")
    public ResponseEntity<ProjectDto> assignTeamToProject(@PathVariable Long projectId, @RequestParam Long teamId) {
        return ResponseEntity.ok(projectService.assignTeamToProject(projectId, teamId));
    }
    @GetMapping("/my-assigned-projects")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<ProjectDto>> getMyAssignedProjects(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) ProjectStatus status) {
        return ResponseEntity.ok(projectService.getMyAssignedProjects(query, status));
    }
    @PatchMapping("/{id}/status")
    public ResponseEntity<?> updateProjectStatus(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {

        ProjectStatus status = ProjectStatus.valueOf(body.get("status"));
        projectService.updateProjectStatus(id, status);
        return ResponseEntity.ok().build();
    }

}

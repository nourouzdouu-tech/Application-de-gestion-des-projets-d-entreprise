package com.dxc.dxc_platform.controller;

import com.dxc.dxc_platform.dto.ManagerSelectDto;
import com.dxc.dxc_platform.dto.ProjectDto;
import com.dxc.dxc_platform.enums.ProjectStatus;
import com.dxc.dxc_platform.service.ProjectService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import com.dxc.dxc_platform.dto.ManagerSelectDto;
import com.dxc.dxc_platform.service.UserAdminService;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.List;

@RestController
@RequestMapping("/api/projects")
public class ProjectController {

    private final ProjectService projectService;
    private final UserAdminService userAdminService;
    public ProjectController(ProjectService projectService, UserAdminService userAdminService) {
        this.projectService = projectService;
        this.userAdminService = userAdminService;
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
}

package com.dxc.dxc_platform.controller;

import com.dxc.dxc_platform.dto.AssignUserToTeamRequest;
import com.dxc.dxc_platform.dto.TeamDto;
import com.dxc.dxc_platform.dto.UserSearchResult;
import com.dxc.dxc_platform.service.TeamService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/teams")
public class TeamController {

    private final TeamService teamService;

    public TeamController(TeamService teamService) {
        this.teamService = teamService;
    }

    @PostMapping
    @PreAuthorize("hasRole('CHEF_PROJET')")
    public ResponseEntity<TeamDto> createTeam(@Valid @RequestBody TeamDto request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(teamService.createTeam(request));
    }

    @PutMapping("/{teamId}")
    @PreAuthorize("hasRole('CHEF_PROJET')")
    public ResponseEntity<TeamDto> updateTeam(@PathVariable Long teamId,
                                              @Valid @RequestBody TeamDto request) {
        return ResponseEntity.ok(teamService.updateTeam(teamId, request));
    }

    @PostMapping("/{teamId}/members")
    @PreAuthorize("hasRole('CHEF_PROJET')")
    public ResponseEntity<TeamDto> assignUserToTeam(@PathVariable Long teamId,
                                                    @Valid @RequestBody AssignUserToTeamRequest request) {
        return ResponseEntity.ok(teamService.assignUserToTeam(teamId, request.getUserId()));
    }

    @PatchMapping("/{teamId}/deleted")
    @PreAuthorize("hasRole('CHEF_PROJET')")
    public ResponseEntity<TeamDto> setDeletedStatus(@PathVariable Long teamId,
                                                    @RequestParam boolean deleted) {
        return ResponseEntity.ok(teamService.setDeletedStatus(teamId, deleted));
    }

    @GetMapping("/{teamId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<TeamDto> getTeamById(@PathVariable Long teamId) {
        return ResponseEntity.ok(teamService.getTeamById(teamId));
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<TeamDto>> getAllTeams() {
        return ResponseEntity.ok(teamService.getAllTeams());
    }

    @GetMapping("/my-teams")
    @PreAuthorize("hasRole('CHEF_PROJET')")
    public ResponseEntity<List<TeamDto>> getMyTeams() {
        return ResponseEntity.ok(teamService.getMyTeams());
    }

    // Endpoint conservé pour compatibilité
    @GetMapping("/my-team")
    @PreAuthorize("hasRole('CHEF_PROJET')")
    public ResponseEntity<TeamDto> getMyTeam() {
        return ResponseEntity.ok(teamService.getMyTeam());
    }

    @GetMapping("/users/search")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<UserSearchResult>> searchAvailableUsers(
            @RequestParam String query) {
        List<UserSearchResult> results = teamService.searchAvailableUsers(query);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(results);
    }

    @DeleteMapping("/{teamId}/members/{userId}")
    @PreAuthorize("hasRole('CHEF_PROJET')")
    public ResponseEntity<TeamDto> removeUserFromTeam(@PathVariable Long teamId,
                                                      @PathVariable Long userId) {
        return ResponseEntity.ok(teamService.removeUserFromTeam(teamId, userId));
    }
}
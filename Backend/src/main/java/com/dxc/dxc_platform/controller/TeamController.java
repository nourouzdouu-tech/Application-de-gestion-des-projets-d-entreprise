package com.dxc.dxc_platform.controller;

import com.dxc.dxc_platform.dto.AssignUserToTeamRequest;
import com.dxc.dxc_platform.dto.TeamCreateRequest;
import com.dxc.dxc_platform.dto.TeamResponse;
import com.dxc.dxc_platform.service.TeamService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
    public ResponseEntity<TeamResponse> createTeam(@Valid @RequestBody TeamCreateRequest request) {
        TeamResponse response = teamService.createTeam(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/{teamId}/members")
    public ResponseEntity<TeamResponse> assignUserToTeam(@PathVariable Long teamId,
                                                         @Valid @RequestBody AssignUserToTeamRequest request) {
        TeamResponse response = teamService.assignUserToTeam(teamId, request.getUserId());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{teamId}")
    public ResponseEntity<TeamResponse> getTeamById(@PathVariable Long teamId) {
        return ResponseEntity.ok(teamService.getTeamById(teamId));
    }

    @GetMapping
    public ResponseEntity<List<TeamResponse>> getAllTeams() {
        return ResponseEntity.ok(teamService.getAllTeams());
    }
}
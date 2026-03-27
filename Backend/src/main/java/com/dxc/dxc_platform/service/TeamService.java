package com.dxc.dxc_platform.service;

import com.dxc.dxc_platform.dto.TeamCreateRequest;
import com.dxc.dxc_platform.dto.TeamResponse;

import java.util.List;

public interface TeamService {

    TeamResponse createTeam(TeamCreateRequest request);

    TeamResponse assignUserToTeam(Long teamId, Long userId);

    TeamResponse getTeamById(Long teamId);

    List<TeamResponse> getAllTeams();
}
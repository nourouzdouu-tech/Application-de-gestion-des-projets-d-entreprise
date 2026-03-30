package com.dxc.dxc_platform.service;

import com.dxc.dxc_platform.dto.TeamDto;
import com.dxc.dxc_platform.dto.UserSearchResult;
import java.util.List;

public interface TeamService {

    TeamDto createTeam(TeamDto request);

    TeamDto updateTeam(Long teamId, TeamDto request);

    TeamDto assignUserToTeam(Long teamId, Long userId);

    TeamDto getTeamById(Long teamId);

    List<TeamDto> getAllTeams();

    TeamDto setDeletedStatus(Long teamId, boolean deleted);
    TeamDto getMyTeam();
    List<UserSearchResult> searchAvailableUsers(String query);
    TeamDto removeUserFromTeam(Long teamId, Long userId);
}
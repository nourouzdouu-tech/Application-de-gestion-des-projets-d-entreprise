package com.dxc.dxc_platform.service.impl;

import com.dxc.dxc_platform.dto.TeamCreateRequest;
import com.dxc.dxc_platform.dto.TeamResponse;
import com.dxc.dxc_platform.entity.Team;
import com.dxc.dxc_platform.entity.User;
import com.dxc.dxc_platform.mapper.TeamMapper;
import com.dxc.dxc_platform.repository.TeamRepository;
import com.dxc.dxc_platform.repository.UserRepository;
import com.dxc.dxc_platform.service.TeamService;
import com.dxc.dxc_platform.shared.exception.BusinessException;
import com.dxc.dxc_platform.shared.exception.ConflictException;
import com.dxc.dxc_platform.shared.exception.NotFoundException;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

import static com.dxc.dxc_platform.shared.exception.ErrorCodes.TEAM_ALREADY_EXISTS;
import static com.dxc.dxc_platform.shared.exception.ErrorCodes.TEAM_NOT_FOUND;
import static com.dxc.dxc_platform.shared.exception.ErrorCodes.USER_ALREADY_IN_TEAM;
import static com.dxc.dxc_platform.shared.exception.ErrorCodes.USER_DELETED;
import static com.dxc.dxc_platform.shared.exception.ErrorCodes.USER_LOCKED;
import static com.dxc.dxc_platform.shared.exception.ErrorCodes.USER_NOT_FOUND;
import static com.dxc.dxc_platform.shared.exception.ErrorCodes.USER_NO_ROLE;

@Service
@Transactional
public class TeamServiceImpl implements TeamService {

    private final TeamRepository teamRepository;
    private final UserRepository userRepository;
    private final TeamMapper teamMapper;

    public TeamServiceImpl(TeamRepository teamRepository,
                           UserRepository userRepository,
                           TeamMapper teamMapper) {
        this.teamRepository = teamRepository;
        this.userRepository = userRepository;
        this.teamMapper = teamMapper;
    }

    @Override
    public TeamResponse createTeam(TeamCreateRequest request) {
        String teamName = request.getName().trim();

        if (teamRepository.existsByNameIgnoreCase(teamName)) {
            throw new ConflictException(
                    TEAM_ALREADY_EXISTS,
                    "Une équipe avec ce nom existe déjà"
            );
        }

        Team team = new Team();
        team.setName(teamName);
        team.setDescription(request.getDescription());

        if (request.getProjectManagerId() != null) {
            User projectManager = userRepository.findById(request.getProjectManagerId())
                    .orElseThrow(() -> new NotFoundException(
                            USER_NOT_FOUND,
                            "Chef de projet introuvable"
                    ));

            validateUserCanBeAssigned(projectManager);

            team.setProjectManager(projectManager);
            projectManager.setTeam(team);
        }

        Team savedTeam = teamRepository.save(team);

        return teamMapper.toResponse(savedTeam);
    }

    @Override
    public TeamResponse assignUserToTeam(Long teamId, Long userId) {
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new NotFoundException(
                        TEAM_NOT_FOUND,
                        "Équipe introuvable"
                ));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException(
                        USER_NOT_FOUND,
                        "Utilisateur introuvable"
                ));

        validateUserCanBeAssigned(user);

        user.setTeam(team);
        userRepository.save(user);

        return teamMapper.toResponse(team);
    }

    @Override
    public TeamResponse getTeamById(Long teamId) {
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new NotFoundException(
                        TEAM_NOT_FOUND,
                        "Équipe introuvable"
                ));

        return teamMapper.toResponse(team);
    }

    @Override
    public List<TeamResponse> getAllTeams() {
        return teamRepository.findAll()
                .stream()
                .map(teamMapper::toResponse)
                .collect(Collectors.toList());
    }

    private void validateUserCanBeAssigned(User user) {
        if (user.isDeleted()) {
            throw new BusinessException(
                    USER_DELETED,
                    "Un utilisateur supprimé ne peut pas être affecté à une équipe"
            );
        }

        if (user.isLocked()) {
            throw new BusinessException(
                    USER_LOCKED,
                    "Un utilisateur verrouillé ne peut pas être affecté à une équipe"
            );
        }

        if (user.getTeam() != null) {
            throw new ConflictException(
                    USER_ALREADY_IN_TEAM,
                    "Cet utilisateur appartient déjà à une équipe"
            );
        }

        if (user.getRoles() == null || user.getRoles().isEmpty()) {
            throw new BusinessException(
                    USER_NO_ROLE,
                    "Un utilisateur doit avoir au moins un rôle"
            );
        }
    }
}
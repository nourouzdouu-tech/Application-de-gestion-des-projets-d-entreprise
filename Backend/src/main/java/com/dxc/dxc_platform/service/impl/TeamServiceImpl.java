package com.dxc.dxc_platform.service.impl;

import com.dxc.dxc_platform.dto.TeamDto;
import com.dxc.dxc_platform.entity.Role;
import com.dxc.dxc_platform.entity.Team;
import com.dxc.dxc_platform.entity.User;
import com.dxc.dxc_platform.mapper.TeamMapper;
import com.dxc.dxc_platform.repository.TeamRepository;
import com.dxc.dxc_platform.repository.UserRepository;
import com.dxc.dxc_platform.service.TeamService;
import com.dxc.dxc_platform.shared.exception.BusinessException;
import com.dxc.dxc_platform.shared.exception.ConflictException;
import com.dxc.dxc_platform.shared.exception.ForbiddenException;
import com.dxc.dxc_platform.shared.exception.NotFoundException;
import jakarta.transaction.Transactional;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import com.dxc.dxc_platform.dto.UserSearchResult;

import java.util.List;
import java.util.stream.Collectors;

import static com.dxc.dxc_platform.shared.exception.ErrorCodes.*;

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
    public TeamDto createTeam(TeamDto request) {
        String teamName = request.getName().trim();

        if (teamRepository.existsByNameIgnoreCaseAndDeletedFalse(teamName)) {
            throw new ConflictException(
                    TEAM_ALREADY_EXISTS,
                    "Une équipe avec ce nom existe déjà"
            );
        }

        User currentUser = getAuthenticatedUser();

        validateProjectManagerRole(currentUser);
        validateUserCanBeAssigned(currentUser);

        Team team = new Team();
        team.setName(teamName);
        team.setDescription(request.getDescription());
        team.setProjectManager(currentUser);
        team.setDeleted(false);

        currentUser.setTeam(team);

        Team savedTeam = teamRepository.save(team);

        return teamMapper.toDto(savedTeam);
    }

    @Override
    public TeamDto updateTeam(Long teamId, TeamDto request) {
        Team team = findActiveTeamById(teamId);
        User currentUser = getAuthenticatedUser();

        validateCurrentUserCanManageTeam(team, currentUser);

        String newName = request.getName().trim();

        teamRepository.findByNameIgnoreCaseAndDeletedFalse(newName)
                .ifPresent(existingTeam -> {
                    if (!existingTeam.getId().equals(team.getId())) {
                        throw new ConflictException(
                                TEAM_ALREADY_EXISTS,
                                "Une équipe avec ce nom existe déjà"
                        );
                    }
                });

        team.setName(newName);
        team.setDescription(request.getDescription());

        Team updatedTeam = teamRepository.save(team);
        return teamMapper.toDto(updatedTeam);
    }

    @Override
    public TeamDto getTeamById(Long teamId) {
        Team team = findActiveTeamById(teamId);
        return teamMapper.toDto(team);
    }

    @Override
    public List<TeamDto> getAllTeams() {
        return teamRepository.findAllByDeletedFalse()
                .stream()
                .map(teamMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public TeamDto setDeletedStatus(Long teamId, boolean deleted) {
        Team team = findTeamByIdForDeleteRestore(teamId, deleted);
        User currentUser = getAuthenticatedUser();

        validateCurrentUserCanManageTeam(team, currentUser);

        team.setDeleted(deleted);

        Team savedTeam = teamRepository.save(team);
        return teamMapper.toDto(savedTeam);
    }

    @Override
    public TeamDto getMyTeam() {
        User currentUser = getAuthenticatedUser();

        Team team = teamRepository.findByProjectManagerIdAndDeletedFalse(currentUser.getId())
                .orElseThrow(() -> new NotFoundException(
                        TEAM_NOT_FOUND,
                        "Aucune équipe associée au chef de projet connecté"
                ));

        return teamMapper.toDto(team);
    }

    @Override
    public List<UserSearchResult> searchAvailableUsers(String query) {
        User currentUser = getAuthenticatedUser();

        List<User> users = userRepository.searchAvailableUsers(query, currentUser.getId());

        return users.stream()
                .map(u -> new UserSearchResult(
                        u.getId(),
                        (u.getPrenom() != null ? u.getPrenom() : "") + " " + (u.getNom() != null ? u.getNom() : ""),
                        u.getEmail(),
                        u.getRoles().stream().map(Role::getNom).collect(Collectors.joining(", ")),
                        u.getTeam() != null,  // alreadyInTeam
                        u.getTeam() != null ? u.getTeam().getName() : null  // teamName
                ))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public TeamDto removeUserFromTeam(Long teamId, Long userId) {
        Team team = findActiveTeamById(teamId);
        User currentUser = getAuthenticatedUser();

        validateCurrentUserCanManageTeam(team, currentUser);

        User userToRemove = userRepository.findByIdAndDeletedFalse(userId)
                .orElseThrow(() -> new NotFoundException(
                        USER_NOT_FOUND,
                        "Utilisateur introuvable"
                ));

        // EMPÊCHER la suppression du chef de projet
        if (team.getProjectManager() != null && team.getProjectManager().getId().equals(userId)) {
            throw new ForbiddenException(
                    FORBIDDEN,  // String "FORBIDDEN"
                    "Vous ne pouvez pas retirer le chef de projet de sa propre équipe"
            );
        }

        if (userToRemove.getTeam() == null || !userToRemove.getTeam().getId().equals(team.getId())) {
            throw new BusinessException(
                    USER_NOT_IN_TEAM,
                    "Cet utilisateur n'appartient pas à cette équipe"
            );
        }

        userToRemove.setTeam(null);
        userRepository.save(userToRemove);

        return teamMapper.toDto(team);
    }

    @Override
    public TeamDto assignUserToTeam(Long teamId, Long userId) {
        Team team = findActiveTeamById(teamId);
        User currentUser = getAuthenticatedUser();

        validateCurrentUserCanManageTeam(team, currentUser);

        User user = userRepository.findByIdAndDeletedFalse(userId)
                .orElseThrow(() -> new NotFoundException(
                        USER_NOT_FOUND,
                        "Utilisateur introuvable"
                ));

        // ✅ Vérification 1: Empêcher l'assignation du chef de projet de CETTE équipe
        if (team.getProjectManager() != null && team.getProjectManager().getId().equals(userId)) {
            throw new ForbiddenException(
                    FORBIDDEN,
                    "Le chef de projet de cette équipe ne peut pas être assigné comme membre"
            );
        }

        // ✅ Vérification 2: Empêcher l'assignation d'un utilisateur qui a le rôle CHEF_PROJET
        boolean isChefProjet = user.getRoles().stream()
                .map(Role::getNom)
                .anyMatch(role -> role.equalsIgnoreCase("CHEF_PROJET"));

        if (isChefProjet) {
            throw new ForbiddenException(
                    FORBIDDEN,
                    "⛔ Impossible d'assigner '" + user.getPrenom() + " " + user.getNom() + "' car c'est un CHEF DE PROJET !"
            );
        }

        // ✅ Vérification 3: Vérifier si l'utilisateur a déjà une équipe
        if (user.getTeam() != null) {
            String teamName = user.getTeam().getName();
            throw new ConflictException(
                    USER_ALREADY_IN_TEAM,
                    "⚠️ " + user.getPrenom() + " " + user.getNom() + " est déjà membre de l'équipe : " + teamName
            );
        }

        // ✅ Vérification 4: Vérifier s'il y a déjà un MANAGER dans l'équipe
        boolean isManager = user.getRoles().stream()
                .map(Role::getNom)
                .anyMatch(role -> role.equalsIgnoreCase("MANAGER"));

        if (isManager) {
            boolean alreadyHasManager = team.getMembers().stream()
                    .filter(member -> !member.getId().equals(team.getProjectManager().getId())) // Exclure le chef de projet
                    .anyMatch(member -> {
                        return member.getRoles().stream()
                                .map(Role::getNom)
                                .anyMatch(role -> role.equalsIgnoreCase("MANAGER"));
                    });

            if (alreadyHasManager) {
                throw new ConflictException(
                        TEAM_MEMBER_ALREADY_EXISTS,
                        "⚠️ Cette équipe a déjà un MANAGER ! Un seul manager est autorisé par équipe."
                );
            }
        }

        validateUserCanBeAssigned(user);

        user.setTeam(team);
        userRepository.save(user);

        return teamMapper.toDto(team);
    }
    // ─── Private Methods ───────────────────────────────────────────────────────

    private Team findActiveTeamById(Long teamId) {
        return teamRepository.findByIdAndDeletedFalse(teamId)
                .orElseThrow(() -> new NotFoundException(
                        TEAM_NOT_FOUND,
                        "Équipe introuvable"
                ));
    }

    private Team findTeamByIdForDeleteRestore(Long teamId, boolean deleted) {
        if (deleted) {
            return teamRepository.findByIdAndDeletedFalse(teamId)
                    .orElseThrow(() -> new NotFoundException(
                            TEAM_NOT_FOUND,
                            "Équipe introuvable"
                    ));
        }

        return teamRepository.findById(teamId)
                .orElseThrow(() -> new NotFoundException(
                        TEAM_NOT_FOUND,
                        "Équipe introuvable"
                ));
    }

    private User getAuthenticatedUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || authentication.getName() == null) {
            throw new BusinessException(
                    UNAUTHORIZED,
                    "Utilisateur non authentifié"
            );
        }

        String email = authentication.getName();

        return userRepository.findByEmailAndDeletedFalse(email)
                .orElseThrow(() -> new NotFoundException(
                        USER_NOT_FOUND,
                        "Utilisateur connecté introuvable"
                ));
    }

    private void validateProjectManagerRole(User user) {
        boolean isProjectManager = user.getRoles().stream()
                .map(Role::getNom)
                .anyMatch(role -> role.equalsIgnoreCase("CHEF_PROJET"));

        if (!isProjectManager) {
            throw new ForbiddenException(
                    FORBIDDEN,  // String "FORBIDDEN"
                    "Seul un chef de projet peut créer une équipe"
            );
        }
    }

    private void validateCurrentUserCanManageTeam(Team team, User currentUser) {
        if (team.getProjectManager() == null || currentUser.getId() == null) {
            throw new ForbiddenException(
                    FORBIDDEN,  // String "FORBIDDEN"
                    "Accès refusé"
            );
        }

        boolean sameProjectManager = team.getProjectManager().getId().equals(currentUser.getId());

        if (!sameProjectManager) {
            throw new ForbiddenException(
                    FORBIDDEN,  // String "FORBIDDEN"
                    "Seul le chef de projet de cette équipe peut la gérer"
            );
        }
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
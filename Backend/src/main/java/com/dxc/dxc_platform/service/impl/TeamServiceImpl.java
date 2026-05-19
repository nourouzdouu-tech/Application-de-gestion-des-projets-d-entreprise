package com.dxc.dxc_platform.service.impl;

import com.dxc.dxc_platform.dto.TeamDto;
import com.dxc.dxc_platform.dto.UserSearchResult;
import com.dxc.dxc_platform.entity.Role;
import com.dxc.dxc_platform.entity.Team;
import com.dxc.dxc_platform.entity.User;
import com.dxc.dxc_platform.mapper.TeamMapper;
import com.dxc.dxc_platform.repository.TeamRepository;
import com.dxc.dxc_platform.repository.UserRepository;
import com.dxc.dxc_platform.service.AuditService;
import com.dxc.dxc_platform.service.EmailService;
import com.dxc.dxc_platform.service.TeamService;
import com.dxc.dxc_platform.shared.exception.BusinessException;
import com.dxc.dxc_platform.shared.exception.ConflictException;
import com.dxc.dxc_platform.shared.exception.ForbiddenException;
import com.dxc.dxc_platform.shared.exception.NotFoundException;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

import static com.dxc.dxc_platform.shared.exception.ErrorCodes.*;

@Service
@Transactional
public class TeamServiceImpl implements TeamService {

    private static final Logger log = LoggerFactory.getLogger(TeamServiceImpl.class);

    private final TeamRepository teamRepository;
    private final UserRepository userRepository;
    private final TeamMapper teamMapper;
    private final AuditService auditService;
    private final EmailService emailService;

    public TeamServiceImpl(TeamRepository teamRepository,
                           UserRepository userRepository,
                           TeamMapper teamMapper,
                           AuditService auditService,
                           EmailService emailService) {
        this.teamRepository = teamRepository;
        this.userRepository = userRepository;
        this.teamMapper = teamMapper;
        this.auditService = auditService;
        this.emailService = emailService;
    }

    private String getCurrentUserEmail() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null ? auth.getName() : "system";
    }

    @Override
    public TeamDto createTeam(TeamDto request) {
        String teamName = request.getName().trim();
        User currentUser = getAuthenticatedUser();

        validateProjectManagerRole(currentUser);

        if (teamRepository.existsByNameForManager(teamName, currentUser.getId())) {
            throw new ConflictException(
                    TEAM_ALREADY_EXISTS,
                    "Vous avez déjà une équipe avec le nom '" + teamName + "'"
            );
        }

        Team team = new Team();
        team.setName(teamName);
        team.setDescription(request.getDescription());
        team.setProjectManager(currentUser);
        team.setDeleted(false);

        Team savedTeam = teamRepository.save(team);

        auditService.log("CREATE_TEAM", "TEAM", savedTeam.getId(),
                "Création de l'équipe " + savedTeam.getName(),
                getCurrentUserEmail(), null);

        return teamMapper.toDto(savedTeam);
    }

    @Override
    public TeamDto updateTeam(Long teamId, TeamDto request) {
        Team team = findActiveTeamById(teamId);
        User currentUser = getAuthenticatedUser();

        validateCurrentUserCanManageTeam(team, currentUser);
        String oldName = team.getName();
        String newName = request.getName().trim();

        List<Team> managerTeams = teamRepository.findByProjectManagerIdAndDeletedFalse(currentUser.getId());
        boolean nameExistsForAnotherTeam = managerTeams.stream()
                .anyMatch(t -> t.getName().equalsIgnoreCase(newName) && !t.getId().equals(team.getId()));

        if (nameExistsForAnotherTeam) {
            throw new ConflictException(
                    TEAM_ALREADY_EXISTS,
                    "Vous avez déjà une autre équipe avec le nom '" + newName + "'"
            );
        }

        team.setName(newName);
        team.setDescription(request.getDescription());

        Team updatedTeam = teamRepository.save(team);
        auditService.log("UPDATE_TEAM", "TEAM", teamId,
                "Modification de l'équipe " + oldName + " → " + newName,
                getCurrentUserEmail(), null);
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

        if (deleted) {
            for (User member : team.getMembers()) {
                member.setTeam(null);
                userRepository.save(member);
            }
        }

        Team savedTeam = teamRepository.save(team);
        String action = deleted ? "DELETE_TEAM" : "RESTORE_TEAM";
        auditService.log(action, "TEAM", teamId,
                (deleted ? "Suppression" : "Restauration") + " de l'équipe " + team.getName(),
                getCurrentUserEmail(), null);
        return teamMapper.toDto(savedTeam);
    }

    @Override
    public TeamDto getMyTeam() {
        User currentUser = getAuthenticatedUser();

        List<Team> managerTeams = teamRepository.findByProjectManagerIdAndDeletedFalse(currentUser.getId());

        if (managerTeams.isEmpty()) {
            throw new NotFoundException(
                    TEAM_NOT_FOUND,
                    "Vous ne gérez aucune équipe"
            );
        }

        return teamMapper.toDto(managerTeams.get(0));
    }

    @Override
    public List<TeamDto> getMyTeams() {
        User currentUser = getAuthenticatedUser();

        List<Team> managerTeams = teamRepository.findByProjectManagerIdAndDeletedFalse(currentUser.getId());

        return managerTeams.stream()
                .map(teamMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<UserSearchResult> searchAvailableUsers(String query) {
        User currentUser = getAuthenticatedUser();

        List<User> users = userRepository.searchAvailableUsers(query, currentUser.getId());

        return users.stream()
                .filter(u -> {
                    boolean isChefProjet = u.getRoles().stream()
                            .map(Role::getNom)
                            .anyMatch(role -> role.equalsIgnoreCase("CHEF_PROJET"));
                    return !isChefProjet;
                })
                .map(u -> new UserSearchResult(
                        u.getId(),
                        (u.getPrenom() != null ? u.getPrenom() : "") + " " + (u.getNom() != null ? u.getNom() : ""),
                        u.getEmail(),
                        u.getRoles().stream().map(Role::getNom).collect(Collectors.joining(", ")),
                        u.getTeam() != null,
                        u.getTeam() != null ? u.getTeam().getName() : null
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

        if (team.getProjectManager() != null && team.getProjectManager().getId().equals(userId)) {
            throw new ForbiddenException(
                    FORBIDDEN,
                    "Le chef de projet ne peut pas être retiré de sa propre équipe"
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
        auditService.log("REMOVE_MEMBER_FROM_TEAM", "TEAM", teamId,
                "Retrait de " + userToRemove.getEmail() + " de l'équipe " + team.getName(),
                getCurrentUserEmail(), null);

        return teamMapper.toDto(team);
    }

    @Override
    @Transactional
    public TeamDto assignUserToTeam(Long teamId, Long userId) {
        Team team = findActiveTeamById(teamId);
        User currentUser = getAuthenticatedUser();

        validateCurrentUserCanManageTeam(team, currentUser);

        User user = userRepository.findByIdAndDeletedFalse(userId)
                .orElseThrow(() -> new NotFoundException(
                        USER_NOT_FOUND,
                        "Utilisateur introuvable"
                ));

        if (team.getProjectManager() != null && team.getProjectManager().getId().equals(userId)) {
            throw new ForbiddenException(
                    FORBIDDEN,
                    "Le chef de projet de cette équipe ne peut pas être assigné comme membre"
            );
        }

        boolean isChefProjet = user.getRoles().stream()
                .map(Role::getNom)
                .anyMatch(role -> role.equalsIgnoreCase("CHEF_PROJET"));

        if (isChefProjet) {
            throw new ForbiddenException(
                    FORBIDDEN,
                    "⛔ Impossible d'assigner '" + user.getPrenom() + " " + user.getNom() +
                            "' car c'est un CHEF DE PROJET !"
            );
        }

        if (user.getTeam() != null) {
            String teamName = user.getTeam().getName();
            throw new ConflictException(
                    USER_ALREADY_IN_TEAM,
                    "⚠️ " + user.getPrenom() + " " + user.getNom() +
                            " est déjà membre de l'équipe : " + teamName
            );
        }

        boolean isManager = user.getRoles().stream()
                .map(Role::getNom)
                .anyMatch(role -> role.equalsIgnoreCase("MANAGER"));

        if (isManager) {
            boolean alreadyHasManager = team.getMembers().stream()
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

        try {
            // ✅ VERSION AVEC WEBSOCKET
            emailService.notifyTeamAssignedWithWS(user, team, currentUser);
        } catch (Exception e) {
            log.error("Erreur lors de l'envoi de la notification d'assignation à l'équipe : {}", e.getMessage());
        }

        auditService.log("ASSIGN_MEMBER_TO_TEAM", "TEAM", teamId,
                "Assignation de " + user.getEmail() + " à l'équipe " + team.getName(),
                getCurrentUserEmail(), null);

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
                    FORBIDDEN,
                    "Seul un chef de projet peut créer une équipe"
            );
        }
    }

    private void validateCurrentUserCanManageTeam(Team team, User currentUser) {
        if (team.getProjectManager() == null || currentUser.getId() == null) {
            throw new ForbiddenException(
                    FORBIDDEN,
                    "Accès refusé"
            );
        }

        if (!team.getProjectManager().getId().equals(currentUser.getId())) {
            throw new ForbiddenException(
                    FORBIDDEN,
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

        if (user.getRoles() == null || user.getRoles().isEmpty()) {
            throw new BusinessException(
                    USER_NO_ROLE,
                    "Un utilisateur doit avoir au moins un rôle"
            );
        }
    }
}
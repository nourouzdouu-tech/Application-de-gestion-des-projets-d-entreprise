package com.dxc.dxc_platform.service.impl;

import com.dxc.dxc_platform.dto.ProjectDto;
import com.dxc.dxc_platform.entity.Project;
import com.dxc.dxc_platform.entity.Role;
import com.dxc.dxc_platform.entity.Team;
import com.dxc.dxc_platform.entity.User;
import com.dxc.dxc_platform.enums.ProjectStatus;
import com.dxc.dxc_platform.mapper.ProjectMapper;
import com.dxc.dxc_platform.repository.ProjectRepository;
import com.dxc.dxc_platform.repository.TeamRepository;
import com.dxc.dxc_platform.repository.UserRepository;
import com.dxc.dxc_platform.service.ProjectService;
import com.dxc.dxc_platform.shared.exception.BusinessException;
import com.dxc.dxc_platform.shared.exception.ConflictException;
import com.dxc.dxc_platform.shared.exception.ForbiddenException;
import com.dxc.dxc_platform.shared.exception.NotFoundException;
import jakarta.transaction.Transactional;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class ProjectServiceImpl implements ProjectService {

    private final ProjectRepository projectRepository;
    private final TeamRepository teamRepository;
    private final UserRepository userRepository;
    private final ProjectMapper projectMapper;

    public ProjectServiceImpl(ProjectRepository projectRepository,
                              TeamRepository teamRepository,
                              UserRepository userRepository,
                              ProjectMapper projectMapper) {
        this.projectRepository = projectRepository;
        this.teamRepository = teamRepository;
        this.userRepository = userRepository;
        this.projectMapper = projectMapper;
    }

    @Override
    public ProjectDto createProject(ProjectDto request) {
        String projectName = request.getName().trim();

        if (projectRepository.existsByNameIgnoreCaseAndDeletedFalse(projectName)) {
            throw new ConflictException("PROJECT_ALREADY_EXISTS", "Un projet avec ce nom existe déjà");
        }

        User currentUser = getAuthenticatedUser();
        validateProjectCrudAccess(currentUser);
        validateProjectDates(request);

        Project project = new Project();
        project.setName(projectName);
        project.setDescription(request.getDescription());
        project.setClient(request.getClient().trim());
        project.setProgressPercentage(request.getProgressPercentage());
        project.setStatus(ProjectStatus.PRE_VALIDE);
        project.setRiskLevel(request.getRiskLevel());
        project.setStartDate(request.getStartDate());
        project.setEndDate(request.getEndDate());
        project.setDeleted(false);

        // AJOUT MANAGER
        if (request.getManagerId() != null) {
            User manager = findValidManagerById(request.getManagerId());
            project.setManager(manager);
        }

        if (isChefProjet(currentUser)) {
            List<Team> teams = teamRepository.findByProjectManagerIdAndDeletedFalse(currentUser.getId());

            if (teams.isEmpty()) {
                throw new NotFoundException(
                        "TEAM_NOT_FOUND",
                        "Aucune équipe associée au chef de projet connecté"
                );
            }

            Team team = teams.get(0);
            project.setTeam(team);
        } else if (isResponsableContrat(currentUser)) {
            project.setTeam(null);
        }

        Project savedProject = projectRepository.save(project);
        return projectMapper.toDto(savedProject);
    }

    @Override
    public ProjectDto updateProject(Long projectId, ProjectDto request) {
        Project project = findActiveProjectById(projectId);
        User currentUser = getAuthenticatedUser();

        validateCurrentUserCanManageProject(project, currentUser);
        validateProjectDates(request);

        String newName = request.getName().trim();

        if (!project.getName().equalsIgnoreCase(newName)
                && projectRepository.existsByNameIgnoreCaseAndDeletedFalse(newName)) {
            throw new ConflictException("PROJECT_ALREADY_EXISTS", "Un projet avec ce nom existe déjà");
        }

        project.setName(newName);
        project.setDescription(request.getDescription());
        project.setClient(request.getClient().trim());
        project.setProgressPercentage(request.getProgressPercentage());
        project.setRiskLevel(request.getRiskLevel());
        project.setStartDate(request.getStartDate());
        project.setEndDate(request.getEndDate());

        // AJOUT / MODIFICATION MANAGER
        if (request.getManagerId() != null) {
            User manager = findValidManagerById(request.getManagerId());
            project.setManager(manager);
        } else {
            project.setManager(null);
        }

        if (isResponsableContrat(currentUser) && request.getTeamId() != null) {
            Team team = teamRepository.findByIdAndDeletedFalse(request.getTeamId())
                    .orElseThrow(() -> new NotFoundException("TEAM_NOT_FOUND", "Équipe introuvable"));
            project.setTeam(team);
        }

        Project updatedProject = projectRepository.save(project);
        return projectMapper.toDto(updatedProject);
    }

    @Override
    public ProjectDto getProjectById(Long projectId) {
        Project project = findActiveProjectById(projectId);
        return projectMapper.toDto(project);
    }

    @Override
    public List<ProjectDto> getAllProjects(String query, ProjectStatus status) {
        User currentUser = getAuthenticatedUser();

        if (!isResponsableContrat(currentUser)) {
            throw new ForbiddenException("FORBIDDEN", "Accès réservé au responsable de contrat");
        }

        List<Project> projects = loadProjects(query, status, false);
        return projects.stream()
                .map(projectMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<ProjectDto> getMyProjects(String query, ProjectStatus status) {
        User currentUser = getAuthenticatedUser();

        if (!isChefProjet(currentUser)) {
            throw new ForbiddenException("FORBIDDEN", "Accès réservé au chef de projet");
        }

        List<Project> projects = loadProjects(query, status, true);
        return projects.stream()
                .map(projectMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public ProjectDto setDeletedStatus(Long projectId, boolean deleted) {
        Project project = deleted
                ? findActiveProjectById(projectId)
                : projectRepository.findById(projectId)
                .orElseThrow(() -> new NotFoundException("PROJECT_NOT_FOUND", "Projet introuvable"));

        User currentUser = getAuthenticatedUser();
        validateCurrentUserCanManageProject(project, currentUser);

        project.setDeleted(deleted);

        Project savedProject = projectRepository.save(project);
        return projectMapper.toDto(savedProject);
    }

    private List<Project> loadProjects(String query, ProjectStatus status, boolean onlyMine) {
        User currentUser = getAuthenticatedUser();

        List<Project> base;
        if (onlyMine) {
            base = status == null
                    ? projectRepository.findAllByTeamProjectManagerIdAndDeletedFalse(currentUser.getId())
                    : projectRepository.findAllByTeamProjectManagerIdAndDeletedFalseAndStatus(currentUser.getId(), status);
        } else {
            base = status == null
                    ? projectRepository.findAllByDeletedFalse()
                    : projectRepository.findAllByDeletedFalseAndStatus(status);
        }

        if (query == null || query.isBlank()) {
            return base;
        }

        String normalized = query.trim().toLowerCase();

        return base.stream()
                .filter(project ->
                        project.getName().toLowerCase().contains(normalized)
                                || project.getClient().toLowerCase().contains(normalized))
                .collect(Collectors.toList());
    }

    private Project findActiveProjectById(Long projectId) {
        return projectRepository.findByIdAndDeletedFalse(projectId)
                .orElseThrow(() -> new NotFoundException("PROJECT_NOT_FOUND", "Projet introuvable"));
    }

    private User getAuthenticatedUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || authentication.getName() == null) {
            throw new BusinessException("UNAUTHORIZED", "Utilisateur non authentifié");
        }

        return userRepository.findByEmailAndDeletedFalse(authentication.getName())
                .orElseThrow(() -> new NotFoundException("USER_NOT_FOUND", "Utilisateur connecté introuvable"));
    }

    private void validateProjectCrudAccess(User user) {
        boolean allowed = user.getRoles().stream()
                .map(Role::getNom)
                .anyMatch(role ->
                        role.equalsIgnoreCase("CHEF_PROJET")
                                || role.equalsIgnoreCase("RESPONSABLE_CONTRAT")
                );

        if (!allowed) {
            throw new ForbiddenException(
                    "FORBIDDEN",
                    "Seul un chef de projet ou un responsable de contrat peut gérer un projet"
            );
        }
    }

    private boolean isChefProjet(User user) {
        return user.getRoles().stream()
                .map(Role::getNom)
                .anyMatch(role -> role.equalsIgnoreCase("CHEF_PROJET"));
    }

    private boolean isResponsableContrat(User user) {
        return user.getRoles().stream()
                .map(Role::getNom)
                .anyMatch(role -> role.equalsIgnoreCase("RESPONSABLE_CONTRAT"));
    }

    private void validateCurrentUserCanManageProject(Project project, User currentUser) {
        if (isResponsableContrat(currentUser)) {
            return;
        }

        if (!isChefProjet(currentUser)) {
            throw new ForbiddenException("FORBIDDEN", "Accès refusé");
        }

        if (project.getTeam() == null
                || project.getTeam().getProjectManager() == null
                || currentUser.getId() == null) {
            throw new ForbiddenException("FORBIDDEN", "Accès refusé");
        }

        boolean sameProjectManager = project.getTeam().getProjectManager().getId().equals(currentUser.getId());

        if (!sameProjectManager) {
            throw new ForbiddenException(
                    "FORBIDDEN",
                    "Seul le chef de projet propriétaire peut gérer ce projet"
            );
        }
    }

    private void validateProjectDates(ProjectDto request) {
        if (request.getStartDate().isAfter(request.getEndDate())) {
            throw new BusinessException(
                    "INVALID_PROJECT_DATES",
                    "La date de début doit être antérieure ou égale à la date de fin"
            );
        }
    }

    private User findValidManagerById(Long managerId) {
        User manager = userRepository.findByIdAndDeletedFalse(managerId)
                .orElseThrow(() -> new NotFoundException("MANAGER_NOT_FOUND", "Manager introuvable"));

        boolean isManager = manager.getRoles().stream()
                .map(Role::getNom)
                .anyMatch(role -> role.equalsIgnoreCase("MANAGER"));

        if (!isManager) {
            throw new BusinessException("INVALID_MANAGER", "L'utilisateur sélectionné n'est pas un manager");
        }

        return manager;
    }
}
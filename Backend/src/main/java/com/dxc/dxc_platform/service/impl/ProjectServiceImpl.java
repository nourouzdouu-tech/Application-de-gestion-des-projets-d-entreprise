package com.dxc.dxc_platform.service.impl;

import com.dxc.dxc_platform.dto.ManagerProjectItemDto;
import com.dxc.dxc_platform.dto.ManagerProjectReviewDto;
import com.dxc.dxc_platform.dto.ProjectDto;
import com.dxc.dxc_platform.dto.UserDto;
import com.dxc.dxc_platform.entity.Project;
import com.dxc.dxc_platform.entity.Role;
import com.dxc.dxc_platform.entity.Team;
import com.dxc.dxc_platform.entity.User;
import com.dxc.dxc_platform.enums.ProjectStatus;
import com.dxc.dxc_platform.mapper.ManagerProjectMapper;
import com.dxc.dxc_platform.mapper.ProjectMapper;
import com.dxc.dxc_platform.mapper.UserMapper;
import com.dxc.dxc_platform.repository.ProjectRepository;
import com.dxc.dxc_platform.repository.TeamRepository;
import com.dxc.dxc_platform.repository.UserRepository;
import com.dxc.dxc_platform.repository.TaskRepository;
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
    private final ManagerProjectMapper managerProjectMapper;
    private final UserMapper userMapper;
    private final TaskRepository taskRepository;

    public ProjectServiceImpl(ProjectRepository projectRepository,
                              TeamRepository teamRepository,
                              UserRepository userRepository,
                              ProjectMapper projectMapper,
                              ManagerProjectMapper managerProjectMapper,
                              UserMapper userMapper,
    TaskRepository taskRepository) {
        this.projectRepository = projectRepository;
        this.teamRepository = teamRepository;
        this.userRepository = userRepository;
        this.projectMapper = projectMapper;
        this.managerProjectMapper = managerProjectMapper;
        this.userMapper = userMapper;
        this.taskRepository = taskRepository;
    }

    @Override
    public ProjectDto createProject(ProjectDto request) {
        // Nettoyage du nom du projet pour éviter les espaces inutiles
        String projectName = request.getName().trim();

        // Vérification de l'unicité du nom du projet parmi les projets non supprimés
        if (projectRepository.existsByNameIgnoreCaseAndDeletedFalse(projectName)) {
            throw new ConflictException("PROJECT_ALREADY_EXISTS", "Un projet avec ce nom existe déjà");
        }

        // Récupération de l'utilisateur connecté
        User currentUser = getAuthenticatedUser();

        // Seul un responsable de contrat ou un chef de projet peut créer/modifier un projet
        validateProjectCrudAccess(currentUser);

        // Vérification métier sur les dates
        validateProjectDates(request);

        Project project = new Project();
        project.setName(projectName);
        project.setDescription(request.getDescription());
        project.setClient(request.getClient().trim());
        project.setProgressPercentage(request.getProgressPercentage());

        // Correction importante :
        // un projet créé par le responsable de contrat doit être en attente de revue manager
        project.setStatus(ProjectStatus.EN_VALIDATION);

        project.setRiskLevel(request.getRiskLevel());
        project.setStartDate(request.getStartDate());
        project.setEndDate(request.getEndDate());
        project.setDeleted(false);

        // Si un manager a été sélectionné au moment de la création,
        // on le rattache au projet
        if (request.getManagerId() != null) {
            User manager = findValidManagerById(request.getManagerId());
            project.setManager(manager);
        }

        // Si le créateur est un chef de projet, on rattache automatiquement son équipe
        // Ce bloc fait partie de ta logique existante
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
            // Si c'est le responsable de contrat qui crée, on peut laisser team à null
            // jusqu'à l'assignation du chef de projet par le manager
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

        if (request.getManagerId() != null) {
            User manager = findValidManagerById(request.getManagerId());
            project.setManager(manager);
        }

        // ✅ Vérifier si l'équipe change et si des tâches existent
        Long newTeamId = request.getTeamId();
        if (newTeamId != null) {
            Long currentTeamId = project.getTeam() != null ? project.getTeam().getId() : null;
            if (!newTeamId.equals(currentTeamId)) {
                long taskCount = taskRepository.countByProjectIdAndDeletedFalse(projectId);
                if (taskCount > 0) {
                    throw new BusinessException(
                            "TASKS_ALREADY_EXIST",
                            "Impossible de changer d'équipe car des tâches existent déjà pour ce projet."
                    );
                }
                Team newTeam = teamRepository.findByIdAndDeletedFalse(newTeamId)
                        .orElseThrow(() -> new NotFoundException("TEAM_NOT_FOUND", "Équipe introuvable"));
                project.setTeam(newTeam);
            }
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

        // Dans ton code actuel, cette liste est réservée au responsable de contrat
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

        List<Project> projects = projectRepository.findAllByChefProjetIdAndDeletedFalse(currentUser.getId());

        // Si un filtre de statut est demandé, on filtre en mémoire
        if (status != null) {
            projects = projects.stream()
                    .filter(project -> project.getStatus() == status)
                    .collect(Collectors.toList());
        } else {
            // Sans filtre, on garde uniquement les projets utiles au chef de projet
            projects = projects.stream()
                    .filter(project ->
                            project.getStatus() == ProjectStatus.PRE_VALIDE
                                    || project.getStatus() == ProjectStatus.EN_COURS)
                    .collect(Collectors.toList());
        }

        // Filtre textuel
        if (query != null && !query.isBlank()) {
            String normalized = query.trim().toLowerCase();
            projects = projects.stream()
                    .filter(p ->
                            p.getName().toLowerCase().contains(normalized)
                                    || p.getClient().toLowerCase().contains(normalized))
                    .collect(Collectors.toList());
        }

        return projects.stream()
                .map(projectMapper::toDto)
                .collect(Collectors.toList());
    }
    @Override
    public ProjectDto setDeletedStatus(Long projectId, boolean deleted) {
        // Si on supprime, il faut trouver un projet actif
        // Si on restaure, on cherche le projet même s'il est supprimé
        Project project = deleted
                ? findActiveProjectById(projectId)
                : projectRepository.findById(projectId)
                .orElseThrow(() -> new NotFoundException("PROJECT_NOT_FOUND", "Projet introuvable"));

        User currentUser = getAuthenticatedUser();

        // Vérifie que l'utilisateur connecté peut agir sur ce projet
        validateCurrentUserCanManageProject(project, currentUser);

        project.setDeleted(deleted);

        Project savedProject = projectRepository.save(project);
        return projectMapper.toDto(savedProject);
    }

    @Override
    public List<ManagerProjectItemDto> getManagerProjects() {
        User currentUser = getAuthenticatedUser();

        if (!isManager(currentUser)) {
            throw new ForbiddenException("FORBIDDEN", "Accès réservé au manager");
        }

        return projectRepository.findAllByDeletedFalseAndManagerIdAndStatusIn(
                        currentUser.getId(),
                        List.of(ProjectStatus.EN_VALIDATION, ProjectStatus.PRE_VALIDE, ProjectStatus.REJETE)
                ).stream()
                .map(managerProjectMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<UserDto.Summary> getChefsProjetForSelect() {
        User currentUser = getAuthenticatedUser();

        // Seul un manager peut charger la liste des chefs de projet
        if (!isManager(currentUser)) {
            throw new ForbiddenException("FORBIDDEN", "Accès réservé au manager");
        }

        return userRepository.findAllActiveChefsProjet()
                .stream()
                .map(userMapper::toSummary)
                .collect(Collectors.toList());
    }

    @Override
    public ProjectDto reviewProjectByManager(ManagerProjectReviewDto request) {
        User currentUser = getAuthenticatedUser();

        if (!isManager(currentUser)) {
            throw new ForbiddenException("FORBIDDEN", "Accès réservé au manager");
        }

        Project project = findActiveProjectById(request.getProjectId());

        if (project.getManager() == null || !project.getManager().getId().equals(currentUser.getId())) {
            throw new ForbiddenException("FORBIDDEN", "Ce projet n'est pas affecté au manager connecté");
        }

        if (project.getStatus() != ProjectStatus.EN_VALIDATION) {
            throw new BusinessException(
                    "INVALID_PROJECT_STATUS",
                    "Seuls les projets en cours de validation peuvent être traités"
            );
        }

        if (request.getCommentaire() == null || request.getCommentaire().isBlank()) {
            throw new BusinessException("COMMENT_REQUIRED", "Le commentaire est obligatoire");
        }

        project.setManagerComment(request.getCommentaire().trim());
        project.setReviewedAt(java.time.LocalDateTime.now());

        if (request.getDecision() == ManagerProjectReviewDto.Decision.VALIDER) {
            if (request.getChefProjetId() == null) {
                throw new BusinessException(
                        "CHEF_PROJET_REQUIRED",
                        "Le chef de projet est obligatoire pour valider l'assignation"
                );
            }

            User chefProjet = findValidChefProjetById(request.getChefProjetId());
            assignChefProjetToProject(project, chefProjet);
            project.setStatus(ProjectStatus.PRE_VALIDE);

        } else if (request.getDecision() == ManagerProjectReviewDto.Decision.REJETER) {
            project.setStatus(ProjectStatus.REJETE);
        }

        Project saved = projectRepository.save(project);
        return projectMapper.toDto(saved);
    }
    @Override
    @Transactional
    public ProjectDto assignTeamToProject(Long projectId, Long teamId) {
        Project project = findActiveProjectById(projectId);
        User currentUser = getAuthenticatedUser();

        if (!isChefProjet(currentUser)) {
            throw new ForbiddenException("FORBIDDEN", "Seul un chef de projet peut assigner une équipe");
        }

        if (project.getChefProjet() == null || !project.getChefProjet().getId().equals(currentUser.getId())) {
            throw new ForbiddenException("FORBIDDEN", "Vous n'êtes pas le chef de projet assigné à ce projet");
        }

        // ✅ Vérifier si des tâches existent déjà pour ce projet
        long taskCount = taskRepository.countByProjectIdAndDeletedFalse(projectId);
        if (taskCount > 0) {
            throw new BusinessException(
                    "TASKS_ALREADY_EXIST",
                    "Impossible de modifier l'équipe car des tâches ont déjà été créées pour ce projet."
            );
        }

        Team team = teamRepository.findByIdAndDeletedFalse(teamId)
                .orElseThrow(() -> new NotFoundException("TEAM_NOT_FOUND", "Équipe introuvable"));

        if (team.getProjectManager() == null || !team.getProjectManager().getId().equals(currentUser.getId())) {
            throw new ForbiddenException("FORBIDDEN", "Vous ne pouvez affecter que votre propre équipe à ce projet");
        }

        project.setTeam(team);
        project.setStatus(ProjectStatus.EN_COURS);

        Project saved = projectRepository.save(project);
        return projectMapper.toDto(saved);
    }

    private List<Project> loadProjects(String query, ProjectStatus status, boolean onlyMine) {
        User currentUser = getAuthenticatedUser();

        List<Project> base;

        // onlyMine = true :
        // on charge uniquement les projets du chef de projet connecté
        // onlyMine = false :
        // on charge tous les projets visibles par le responsable de contrat
        if (onlyMine) {
            base = status == null
                    ? projectRepository.findAllByTeamProjectManagerIdAndDeletedFalse(currentUser.getId())
                    : projectRepository.findAllByTeamProjectManagerIdAndDeletedFalseAndStatus(currentUser.getId(), status);
        } else {
            base = status == null
                    ? projectRepository.findAllByDeletedFalse()
                    : projectRepository.findAllByDeletedFalseAndStatus(status);
        }

        // Si aucune recherche n'est fournie, on retourne directement la base
        if (query == null || query.isBlank()) {
            return base;
        }

        String normalized = query.trim().toLowerCase();

        // Filtrage mémoire sur nom ou client
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

    private boolean isManager(User user) {
        return user.getRoles().stream()
                .map(Role::getNom)
                .anyMatch(role -> role.equalsIgnoreCase("MANAGER"));
    }

    private void validateCurrentUserCanManageProject(Project project, User currentUser) {
        if (isResponsableContrat(currentUser)) {
            return;
        }
        if (!isChefProjet(currentUser)) {
            throw new ForbiddenException("FORBIDDEN", "Accès refusé");
        }
        if (project.getChefProjet() == null || !project.getChefProjet().getId().equals(currentUser.getId())) {
            throw new ForbiddenException("FORBIDDEN", "Vous n'êtes pas le chef de projet assigné");
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

    private User findValidChefProjetById(Long chefProjetId) {
        User chefProjet = userRepository.findByIdAndDeletedFalse(chefProjetId)
                .orElseThrow(() -> new NotFoundException("CHEF_PROJET_NOT_FOUND", "Chef de projet introuvable"));

        boolean isChefProjet = chefProjet.getRoles().stream()
                .map(Role::getNom)
                .anyMatch(role -> role.equalsIgnoreCase("CHEF_PROJET"));

        if (!isChefProjet) {
            throw new BusinessException(
                    "INVALID_CHEF_PROJET",
                    "L'utilisateur sélectionné n'est pas un chef de projet"
            );
        }

        return chefProjet;
    }

    private void assignChefProjetToProject(Project project, User chefProjet) {
        // On assigne directement le chef de projet, sans équipe.
        project.setChefProjet(chefProjet);
        // On peut aussi laisser team = null, il sera assigné plus tard par le chef de projet.
    }
    @Override
    public List<ProjectDto> getMyAssignedProjects(String query, ProjectStatus status) {
        User currentUser = getAuthenticatedUser();

        if (currentUser.getTeam() == null) {
            throw new NotFoundException(
                    "TEAM_NOT_FOUND",
                    "Aucune équipe n'est associée à l'utilisateur connecté"
            );
        }

        Long teamId = currentUser.getTeam().getId();

        List<Project> projects = (status == null)
                ? projectRepository.findAllByTeamIdAndDeletedFalse(teamId)
                : projectRepository.findAllByTeamIdAndDeletedFalseAndStatus(teamId, status);

        // optionnel : ne montrer que les projets utiles aux membres
        if (status == null) {
            projects = projects.stream()
                    .filter(project ->
                            project.getStatus() == ProjectStatus.EN_COURS
                                    || project.getStatus() == ProjectStatus.PRE_VALIDE
                                    || project.getStatus() == ProjectStatus.EN_VALIDATION)
                    .toList();
        }

        if (query != null && !query.isBlank()) {
            String normalized = query.trim().toLowerCase();

            projects = projects.stream()
                    .filter(project ->
                            project.getName().toLowerCase().contains(normalized)
                                    || project.getClient().toLowerCase().contains(normalized))
                    .toList();
        }

        return projects.stream()
                .map(projectMapper::toDto)
                .toList();
    }
}
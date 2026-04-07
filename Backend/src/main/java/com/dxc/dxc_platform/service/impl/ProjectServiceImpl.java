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

    public ProjectServiceImpl(ProjectRepository projectRepository,
                              TeamRepository teamRepository,
                              UserRepository userRepository,
                              ProjectMapper projectMapper,
                              ManagerProjectMapper managerProjectMapper,
                              UserMapper userMapper) {
        this.projectRepository = projectRepository;
        this.teamRepository = teamRepository;
        this.userRepository = userRepository;
        this.projectMapper = projectMapper;
        this.managerProjectMapper = managerProjectMapper;
        this.userMapper = userMapper;
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

        // Vérifie que l'utilisateur connecté a le droit de modifier ce projet
        validateCurrentUserCanManageProject(project, currentUser);

        // Vérification métier sur les dates
        validateProjectDates(request);

        String newName = request.getName().trim();

        // Si le nom change, on vérifie qu'il n'existe pas déjà
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

        // Mise à jour éventuelle du manager
        if (request.getManagerId() != null) {
            User manager = findValidManagerById(request.getManagerId());
            project.setManager(manager);
        } else {
            project.setManager(null);
        }

        // Si le responsable de contrat choisit explicitement une équipe
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

        // Dans ton code actuel, cette liste est réservée au chef de projet
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

        // Seul un manager peut accéder à cette liste
        if (!isManager(currentUser)) {
            throw new ForbiddenException("FORBIDDEN", "Accès réservé au manager");
        }

        // Le manager voit uniquement ses projets en cours de validation
        return projectRepository.findAllByDeletedFalseAndStatusAndManagerId(
                        ProjectStatus.EN_VALIDATION,
                        currentUser.getId()
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

        User chefProjet = findValidChefProjetById(request.getChefProjetId());
        assignChefProjetToProject(project, chefProjet);

        project.setManagerComment(request.getCommentaire().trim());
        project.setReviewedAt(java.time.LocalDateTime.now());

        if (request.getDecision() == ManagerProjectReviewDto.Decision.VALIDER) {
            project.setStatus(ProjectStatus.PRE_VALIDE);
        } else {
            project.setStatus(ProjectStatus.REJETE);
        }

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
        // Le responsable de contrat a tous les droits sur les projets
        if (isResponsableContrat(currentUser)) {
            return;
        }

        // Si ce n'est ni un responsable de contrat ni un chef de projet, accès refusé
        if (!isChefProjet(currentUser)) {
            throw new ForbiddenException("FORBIDDEN", "Accès refusé");
        }

        // Vérification que le projet appartient à l'équipe du chef de projet connecté
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
        // Dans ton modèle actuel, le projet n'a pas un champ direct chefProjet.
        // Le lien passe par Team.projectManager.
        // On cherche donc une équipe dont le projectManager est le chef de projet sélectionné.
        List<Team> teams = teamRepository.findByProjectManagerIdAndDeletedFalse(chefProjet.getId());

        if (teams.isEmpty()) {
            throw new BusinessException(
                    "TEAM_NOT_FOUND_FOR_CHEF_PROJET",
                    "Aucune équipe trouvée pour le chef de projet sélectionné"
            );
        }

        // Pour l'instant on prend la première équipe trouvée
        project.setTeam(teams.get(0));
    }
}
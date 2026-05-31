package com.dxc.dxc_platform.service.impl;
import com.dxc.dxc_platform.dto.reporting.TaskReportDto;
import com.dxc.dxc_platform.dto.reporting.UserReportDto;
import com.dxc.dxc_platform.dto.*;
import com.dxc.dxc_platform.entity.Project;
import com.dxc.dxc_platform.entity.Task;
import com.dxc.dxc_platform.entity.User;
import com.dxc.dxc_platform.enums.ProjectStatus;
import com.dxc.dxc_platform.enums.Status;
import com.dxc.dxc_platform.repository.ProjectRepository;
import com.dxc.dxc_platform.repository.TaskRepository;
import com.dxc.dxc_platform.repository.TeamRepository;
import com.dxc.dxc_platform.repository.UserRepository;
import com.dxc.dxc_platform.service.ReportingService;
import org.springframework.stereotype.Service;
import com.dxc.dxc_platform.entity.Role;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import com.dxc.dxc_platform.dto.ManagerProjectDto;
import com.dxc.dxc_platform.dto.ManagerProjectMemberDto;
import java.util.ArrayList;

@Service
public class ReportingServiceImpl implements ReportingService {

    private final ProjectRepository projectRepository;
    private final TaskRepository taskRepository;
    private final UserRepository userRepository;
    private final TeamRepository teamRepository;

    public ReportingServiceImpl(ProjectRepository projectRepository,
                                TaskRepository taskRepository,
                                UserRepository userRepository,
                                TeamRepository teamRepository) {
        this.projectRepository = projectRepository;
        this.taskRepository = taskRepository;
        this.userRepository = userRepository;
        this.teamRepository = teamRepository;
    }
    @Override
    public List<ManagerProjectDto> getProjectsByManager() {
        return projectRepository.findAll().stream()
                .filter(p -> !p.isDeleted())
                .map(this::toManagerProjectDto)
                .collect(Collectors.toList());
    }

    private ManagerProjectDto toManagerProjectDto(Project p) {
        String chefProjetName = null;
        if (p.getChefProjet() != null) {
            chefProjetName = (p.getChefProjet().getPrenom() != null ? p.getChefProjet().getPrenom() : "") +
                    " " + (p.getChefProjet().getNom() != null ? p.getChefProjet().getNom() : "");
            chefProjetName = chefProjetName.trim();
            if (chefProjetName.isEmpty()) chefProjetName = null;
        }

        String managerName = null;
        if (p.getManager() != null) {
            managerName = (p.getManager().getPrenom() != null ? p.getManager().getPrenom() : "") +
                    " " + (p.getManager().getNom() != null ? p.getManager().getNom() : "");
            managerName = managerName.trim();
            if (managerName.isEmpty()) managerName = null;
        }

        String teamName = p.getTeam() != null ? p.getTeam().getName() : null;

        List<ManagerProjectMemberDto> members = new ArrayList<>();
        if (p.getTeam() != null && p.getTeam().getMembers() != null) {
            members = p.getTeam().getMembers().stream()
                    .map(m -> {
                        String profileLibelle = "Non défini";
                        if (m.getProfile() != null && m.getProfile().getLibelle() != null) {
                            profileLibelle = m.getProfile().getLibelle();
                        }

                        String role = "Membre";
                        if (m.getRoles() != null && !m.getRoles().isEmpty()) {
                            role = m.getRoles().iterator().next().getDescription();
                            if (role == null) role = "Membre";
                        }

                        return new ManagerProjectMemberDto(
                                m.getId(),
                                (m.getPrenom() != null ? m.getPrenom() : "") + " " + (m.getNom() != null ? m.getNom() : ""),
                                m.getEmail(),
                                profileLibelle,
                                0, // TJM par défaut
                                role
                        );
                    })
                    .collect(Collectors.toList());
        }

        // Un projet est facturé s'il est pré-validé ou validé
        boolean isFactured = p.getStatus() == ProjectStatus.PRE_VALIDE ||
                p.getStatus() == ProjectStatus.VALIDE;

        return new ManagerProjectDto(
                p.getId(),
                p.getName(),
                p.getClient() != null ? p.getClient() : "Non défini",
                p.getStatus() != null ? p.getStatus().toString() : "INCONNU",
                p.getDescription() != null ? p.getDescription() : "",
                p.getStartDate(),
                p.getEndDate(),
                teamName,
                chefProjetName,
                managerName,
                isFactured,
                members
        );
    }
    @Override
    public List<ProjectReportDto> getProjectsReport(Integer year, String status, Long teamId) {
        return projectRepository.findAll().stream()
                .filter(p -> !p.isDeleted())
                .filter(p -> year == null || (p.getStartDate() != null && p.getStartDate().getYear() == year))
                .filter(p -> status == null || (p.getStatus() != null && p.getStatus().toString().equals(status)))
                .filter(p -> teamId == null || (p.getTeam() != null && p.getTeam().getId().equals(teamId)))
                .map(this::toProjectReportDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<ProjectReportDto> getOverdueProjects() {
        LocalDate today = LocalDate.now();
        return projectRepository.findAll().stream()
                .filter(p -> !p.isDeleted())
                .filter(p -> p.getEndDate() != null)
                .filter(p -> today.isAfter(p.getEndDate()))
                .filter(p -> p.getStatus() != ProjectStatus.CLOTURE)
                .map(this::toProjectReportDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<com.dxc.dxc_platform.dto.reporting.TaskReportDto> getOverdueTasks(Long projectId) {
        LocalDate today = LocalDate.now();
        return taskRepository.findAll().stream()
                .filter(t -> t.getEstimatedEndDate() != null)
                .filter(t -> today.isAfter(t.getEstimatedEndDate()))
                .filter(t -> t.getStatus() != Status.Terminé)
                .filter(t -> projectId == null || (t.getProject() != null && t.getProject().getId().equals(projectId)))
                .map(this::toTaskReportDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<com.dxc.dxc_platform.dto.reporting.UserReportDto> getUsersWithoutProfile() {
        return userRepository.findAll().stream()
                .filter(u -> !u.isDeleted())
                .filter(u -> u.getProfile() == null)
                .map(this::toUserReportDto)
                .collect(Collectors.toList());
    }

    @Override
    public UserStatusReportDto getUsersByStatus(Boolean active) {
        List<User> allUsers = userRepository.findAll().stream()
                .filter(u -> !u.isDeleted())
                .collect(Collectors.toList());

        List<com.dxc.dxc_platform.dto.reporting.UserReportDto> filtered = allUsers.stream()
                .filter(u -> active == null || u.isEnabled() == active)
                .map(this::toUserReportDto)
                .collect(Collectors.toList());

        long totalActive = allUsers.stream().filter(User::isEnabled).count();
        long totalInactive = allUsers.stream().filter(u -> !u.isEnabled()).count();
        long totalLocked = allUsers.stream().filter(User::isLocked).count();

        // Utilisateur avec le plus de réinitialisations de mdp (basé sur failedAttempts comme proxy)
        com.dxc.dxc_platform.dto.reporting.UserReportDto topResetter = allUsers.stream()
                .max(Comparator.comparingInt(User::getFailedAttempts))
                .map(this::toUserReportDto)
                .orElse(null);

        int maxResetCount = topResetter != null ? topResetter.getFailedAttempts() : 0;

        return new UserStatusReportDto(filtered, totalActive, totalInactive, totalLocked,
                topResetter, maxResetCount);
    }

    @Override
    public List<com.dxc.dxc_platform.dto.reporting.ProjectSelectDto> getProjectsForSelect() {
        return projectRepository.findAll().stream()
                .filter(p -> !p.isDeleted())
                .map(p -> new com.dxc.dxc_platform.dto.reporting.ProjectSelectDto(p.getId(), p.getName()))
                .collect(Collectors.toList());
    }

    @Override
    public List<com.dxc.dxc_platform.dto.reporting.TeamSelectDto> getTeamsForSelect() {
        return teamRepository.findAll().stream()
                .filter(t -> !t.isDeleted())
                .map(t -> new com.dxc.dxc_platform.dto.reporting.TeamSelectDto(t.getId(), t.getName()))
                .collect(Collectors.toList());
    }

    // ──────────────────────────────────────────────
    // Helpers
    // ──────────────────────────────────────────────

    private ProjectReportDto toProjectReportDto(Project p) {
        String chefNom = null, chefPrenom = null;
        if (p.getChefProjet() != null) {
            chefNom = p.getChefProjet().getNom();
            chefPrenom = p.getChefProjet().getPrenom();
        }
        String teamNom = p.getTeam() != null ? p.getTeam().getName() : null;
        Long teamId = p.getTeam() != null ? p.getTeam().getId() : null;

        return new ProjectReportDto(
                p.getId(), p.getName(), p.getDescription(), p.getStatus(),
                p.getStartDate(), p.getEndDate(), p.getEndDate(),
                chefNom, chefPrenom, teamNom, teamId
        );
    }

    private com.dxc.dxc_platform.dto.reporting.TaskReportDto toTaskReportDto(Task t) {
        String assigneNom = null, assignePrenom = null;
        if (t.getAssignedTo() != null) {
            assigneNom = t.getAssignedTo().getNom();
            assignePrenom = t.getAssignedTo().getPrenom();
        }
        Long projectId = t.getProject() != null ? t.getProject().getId() : null;
        String projectNom = t.getProject() != null ? t.getProject().getName() : null;

        return new TaskReportDto(
                t.getId(), t.getTitle(), t.getDescription(), t.getStatus(), t.getPriority(),
                t.getEstimatedEndDate(), assigneNom, assignePrenom, projectId, projectNom
        );
    }

    private UserReportDto toUserReportDto(User u) {
        // Récupérer TOUS les rôles séparés par des virgules
        String roles = u.getRoles().stream()
                .map(Role::getDescription)        // Récupère le nom de chaque rôle
                .collect(Collectors.joining(", "));  // Joint avec ", "

        if (roles.isEmpty()) {
            roles = "INCONNU";
        }

        String profileLibelle = u.getProfile() != null ? u.getProfile().getLibelle() : null;

        return new UserReportDto(
                u.getId(),
                u.getEmail(),
                u.getNom(),
                u.getPrenom(),
                roles,
                u.isEnabled(),
                u.isLocked(),
                u.getFailedAttempts(),
                0,
                profileLibelle
        );
    }
}
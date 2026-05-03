package com.dxc.dxc_platform.service.impl;

import com.dxc.dxc_platform.dto.TaskDto;
import com.dxc.dxc_platform.entity.Project;
import com.dxc.dxc_platform.entity.Task;
import com.dxc.dxc_platform.entity.User;
import com.dxc.dxc_platform.enums.Priority;
import com.dxc.dxc_platform.enums.Status;
import com.dxc.dxc_platform.repository.ProjectRepository;
import com.dxc.dxc_platform.repository.TaskRepository;
import com.dxc.dxc_platform.repository.UserRepository;
import com.dxc.dxc_platform.service.AuditService;
import com.dxc.dxc_platform.service.EmailService;
import com.dxc.dxc_platform.service.TaskService;
import com.dxc.dxc_platform.shared.exception.BusinessException;
import com.dxc.dxc_platform.shared.exception.ForbiddenException;
import com.dxc.dxc_platform.shared.exception.NotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Transactional
public class TaskServiceImpl implements TaskService {
    private static final Logger log = LoggerFactory.getLogger(TaskServiceImpl.class);
    private final TaskRepository taskRepository;
    private final UserRepository userRepository;
    private final ProjectRepository projectRepository;
    private final AuditService auditService;
    private final EmailService emailService;

    public TaskServiceImpl(TaskRepository taskRepository,
                           UserRepository userRepository,
                           ProjectRepository projectRepository,
                           AuditService auditService,
                           EmailService emailService) {
        this.taskRepository = taskRepository;
        this.userRepository = userRepository;
        this.projectRepository = projectRepository;
        this.auditService = auditService;
        this.emailService = emailService;
    }

    private String getCurrentUserEmail() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }

    private User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();

        return userRepository.findByEmailAndDeletedFalse(email)
                .orElseThrow(() -> new NotFoundException("USER_NOT_FOUND", "Utilisateur non trouvé"));
    }

    private boolean isChefProjet(User user) {
        return user.getRoles().stream()
                .anyMatch(r -> r.getNom().equalsIgnoreCase("CHEF_PROJET"));
    }

    private int calculerDureeDepuisCriticite(Integer criticite) {
        if (criticite == null) {
            return 3;
        }

        Set<Integer> valeursAutorisees = Set.of(1, 2, 3, 5, 8, 13);
        if (!valeursAutorisees.contains(criticite)) {
            throw new BusinessException(
                    "INVALID_CRITICITE",
                    "La criticité doit être une valeur Fibonacci parmi : 1, 2, 3, 5, 8, 13"
            );
        }

        return criticite;
    }

    private LocalDate calculerDateFin(LocalDate startDate, Integer dureeEstimee) {
        if (startDate == null || dureeEstimee == null) {
            return null;
        }
        return startDate.plusDays(dureeEstimee);
    }

    @Override
    public TaskDto createTask(TaskDto dto) {
        User currentUser = getCurrentUser();

        if (!isChefProjet(currentUser)) {
            throw new ForbiddenException("FORBIDDEN", "Seul un chef de projet peut créer des tâches");
        }

        Project project = projectRepository.findByIdAndDeletedFalse(dto.getProjectId())
                .orElseThrow(() -> new NotFoundException("PROJECT_NOT_FOUND", "Projet introuvable"));

        if (project.getTeam() == null || project.getTeam().getProjectManager() == null ||
                !project.getTeam().getProjectManager().getId().equals(currentUser.getId())) {
            throw new ForbiddenException("FORBIDDEN", "Vous n'êtes pas le chef de projet assigné à ce projet");
        }

        if (project.getTeam() == null) {
            throw new BusinessException(
                    "PROJECT_HAS_NO_TEAM",
                    "Aucune équipe n'est encore affectée à ce projet"
            );
        }

        User assignedUser = userRepository.findByIdAndDeletedFalse(dto.getAssignedToId())
                .orElseThrow(() -> new NotFoundException("USER_NOT_FOUND", "Utilisateur cible introuvable"));

        if (assignedUser.getTeam() == null || !assignedUser.getTeam().getId().equals(project.getTeam().getId())) {
            throw new ForbiddenException(
                    "FORBIDDEN",
                    "Cet utilisateur n'appartient pas à l'équipe du projet"
            );
        }

        LocalDate startDate = dto.getStartDate() != null ? dto.getStartDate() : LocalDate.now();
        int dureeEstimee = calculerDureeDepuisCriticite(dto.getCriticite());
        LocalDate estimatedEndDate = calculerDateFin(startDate, dureeEstimee);

        Task task = new Task();
        task.setTitle(dto.getTitle());
        task.setDescription(dto.getDescription());
        task.setStatus(dto.getStatus() != null ? dto.getStatus() : Status.A_faire);
        task.setStartDate(startDate);
        task.setCriticite(dto.getCriticite() != null ? dto.getCriticite() : 3);
        task.setDureeEstimee(dureeEstimee);
        task.setEstimatedEndDate(estimatedEndDate);
        task.setAssignedTo(assignedUser);
        task.setProject(project);
        task.setPriority(dto.getPriority() != null ? dto.getPriority() : Priority.MOYENNE);
        task.setRejected(false);
        task.setRejectionComment(null);

        Task saved = taskRepository.save(task);

        // === NOTIFICATIONS EMAIL ===
        try {
            // Notifier le membre assigné
            emailService.notifyTaskAssigned(saved, assignedUser, currentUser);
            // Confirmation au chef de projet
            emailService.notifyTaskCreated(saved, currentUser);
        } catch (Exception e) {
            log.error("Erreur lors de l'envoi des emails: {}", e.getMessage());
        }

        auditService.log("CREATE_TASK", "TASK", saved.getId(),
                "Création de la tâche '" + saved.getTitle() + "' pour le projet " + project.getName(),
                getCurrentUserEmail(), null);

        return toDto(saved);
    }
    @Override
    public TaskDto updateTask(Long id, TaskDto dto) {
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("TASK_NOT_FOUND", "Tâche introuvable"));

        User currentUser = getCurrentUser();

        if (task.getProject() == null ||
                task.getProject().getTeam() == null ||
                task.getProject().getTeam().getProjectManager() == null ||
                !task.getProject().getTeam().getProjectManager().getId().equals(currentUser.getId())) {
            throw new ForbiddenException("FORBIDDEN", "Vous ne pouvez pas modifier cette tâche");
        }

        String oldTitle = task.getTitle();
        User assignedUser = task.getAssignedTo();
        Status oldStatus = task.getStatus();
        boolean wasRejected = task.isRejected();

        // Sauvegarder l'ancienne assignation (si changée)
        Long oldAssignedToId = task.getAssignedTo() != null ? task.getAssignedTo().getId() : null;
        Long newAssignedToId = dto.getAssignedToId();

        task.setTitle(dto.getTitle());
        task.setDescription(dto.getDescription());

        LocalDate startDate = dto.getStartDate() != null ? dto.getStartDate() : task.getStartDate();
        Integer criticite = dto.getCriticite() != null ? dto.getCriticite() : task.getCriticite();
        int dureeEstimee = calculerDureeDepuisCriticite(criticite);
        LocalDate estimatedEndDate = calculerDateFin(startDate, dureeEstimee);

        task.setStartDate(startDate);
        task.setCriticite(criticite);
        task.setDureeEstimee(dureeEstimee);
        task.setEstimatedEndDate(estimatedEndDate);

        if (dto.getStatus() != null) {
            task.setStatus(dto.getStatus());
        }

        if (dto.getPriority() != null) {
            task.setPriority(dto.getPriority());
        }

        // Si l'assignation a changé, mettre à jour
        if (newAssignedToId != null && !newAssignedToId.equals(oldAssignedToId)) {
            User newAssignedUser = userRepository.findByIdAndDeletedFalse(newAssignedToId)
                    .orElseThrow(() -> new NotFoundException("USER_NOT_FOUND", "Utilisateur cible introuvable"));
            task.setAssignedTo(newAssignedUser);

            // Notifier le nouveau membre
            try {
                emailService.notifyTaskAssigned(task, newAssignedUser, currentUser);
            } catch (Exception e) {
                log.error("Erreur envoi email au nouveau membre: {}", e.getMessage());
            }
        }

        // Déterminer si c'est une validation ou un rejet
        boolean isValidation = false;
        boolean isRejection = false;
        String rejectionComment = null;

        if (dto.getRejected() != null) {
            task.setRejected(dto.getRejected());
            isRejection = dto.getRejected() && !wasRejected;
        }

        if (dto.getRejectionComment() != null) {
            task.setRejectionComment(dto.getRejectionComment());
            rejectionComment = dto.getRejectionComment();
        }

        // Si le statut passe de "Validation" à "Terminé" et la tâche n'est pas rejetée, c'est une validation
        if (oldStatus == Status.Validation && task.getStatus() == Status.Terminé && !task.isRejected()) {
            isValidation = true;
        }

        Task updated = taskRepository.save(task);

        // === NOTIFICATION EMAIL APPROPRIÉE ===
        try {
            if (isValidation) {
                // C'est une validation de tâche
                emailService.notifyTaskValidated(updated, assignedUser, rejectionComment);
            } else if (isRejection) {
                // C'est un rejet de tâche
                emailService.notifyTaskRejected(updated, assignedUser, rejectionComment);
            } else {
                // C'est une modification simple
                emailService.notifyTaskUpdated(updated, currentUser, assignedUser);
            }
        } catch (Exception e) {
            log.error("Erreur lors de l'envoi de l'email: {}", e.getMessage());
        }

        auditService.log("UPDATE_TASK", "TASK", id,
                "Modification de la tâche '" + oldTitle + "' → '" + updated.getTitle() + "'",
                getCurrentUserEmail(), null);

        return toDto(updated);
    }

    @Override
    public TaskDto getTaskById(Long id) {
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("TASK_NOT_FOUND", "Tâche introuvable"));
        return toDto(task);
    }

    @Override
    public List<TaskDto> getTasksByProject(Long projectId) {
        User currentUser = getCurrentUser();

        if (!isChefProjet(currentUser)) {
            throw new ForbiddenException("FORBIDDEN", "Accès réservé au chef de projet");
        }

        Project project = projectRepository.findByIdAndDeletedFalse(projectId)
                .orElseThrow(() -> new NotFoundException("PROJECT_NOT_FOUND", "Projet introuvable"));

        if (project.getTeam() == null) {
            throw new ForbiddenException("FORBIDDEN", "Aucune équipe n'est assignée à ce projet");
        }

        if (project.getTeam().getProjectManager() == null) {
            throw new ForbiddenException("FORBIDDEN", "Aucun chef de projet n'est assigné à ce projet");
        }

        if (!project.getTeam().getProjectManager().getId().equals(currentUser.getId())) {
            throw new ForbiddenException("FORBIDDEN", "Accès non autorisé");
        }

        List<Task> tasks = taskRepository.findAllByProjectIdAndDeletedFalse(projectId);

        return tasks.stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<TaskDto> getMyTasks(String query, Priority priority, Long assignedToId) {
        User currentUser = getCurrentUser();

        if (currentUser.getTeam() == null) {
            throw new NotFoundException("TEAM_NOT_FOUND", "Aucune équipe associée à l'utilisateur connecté");
        }

        List<Task> tasks = taskRepository.findAllByAssignedToIdAndProjectTeamIdAndDeletedFalse(
                currentUser.getId(),
                currentUser.getTeam().getId()
        );

        return tasks.stream()
                .filter(task -> {
                    boolean matchesQuery = true;
                    boolean matchesPriority = true;
                    boolean matchesAssignedTo = true;

                    if (query != null && !query.isBlank()) {
                        String q = query.trim().toLowerCase();

                        String title = task.getTitle() != null ? task.getTitle().toLowerCase() : "";
                        String projectName = task.getProject() != null && task.getProject().getName() != null
                                ? task.getProject().getName().toLowerCase()
                                : "";
                        String assignedToName = task.getAssignedTo() != null
                                ? ((task.getAssignedTo().getPrenom() != null ? task.getAssignedTo().getPrenom() : "") + " " +
                                (task.getAssignedTo().getNom() != null ? task.getAssignedTo().getNom() : "")).trim().toLowerCase()
                                : "";

                        matchesQuery = title.contains(q)
                                || projectName.contains(q)
                                || assignedToName.contains(q);
                    }

                    if (priority != null) {
                        matchesPriority = task.getPriority() == priority;
                    }

                    if (assignedToId != null) {
                        matchesAssignedTo = task.getAssignedTo() != null
                                && task.getAssignedTo().getId().equals(assignedToId);
                    }

                    return matchesQuery && matchesPriority && matchesAssignedTo;
                })
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public TaskDto updateMyTaskStatus(Long taskId, Status status) {
        User currentUser = getCurrentUser();

        if (currentUser.getTeam() == null) {
            throw new NotFoundException("TEAM_NOT_FOUND", "Aucune équipe associée à l'utilisateur connecté");
        }

        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new NotFoundException("TASK_NOT_FOUND", "Tâche introuvable"));

        if (task.isDeleted()) {
            throw new NotFoundException("TASK_NOT_FOUND", "Tâche introuvable");
        }

        if (task.getAssignedTo() == null || !task.getAssignedTo().getId().equals(currentUser.getId())) {
            throw new ForbiddenException("FORBIDDEN", "Cette tâche ne vous est pas assignée");
        }

        if (task.getProject() == null || task.getProject().getTeam() == null ||
                !task.getProject().getTeam().getId().equals(currentUser.getTeam().getId())) {
            throw new ForbiddenException("FORBIDDEN", "Cette tâche n'appartient pas à un projet de votre équipe");
        }

        Status oldStatus = task.getStatus();
        task.setStatus(status);

        if (status == Status.Validation) {
            task.setRejected(false);
            task.setRejectionComment(null);
        }

        Task updated = taskRepository.save(task);

        // === NOTIFICATION EMAIL SI SOUMISSION ===
        if (status == Status.Validation && task.getProject() != null &&
                task.getProject().getTeam() != null && task.getProject().getTeam().getProjectManager() != null) {
            try {
                User chefProjet = task.getProject().getTeam().getProjectManager();
                emailService.notifyTaskSubmittedForValidation(updated, currentUser, chefProjet);
            } catch (Exception e) {
                log.error("Erreur envoi email notification chef: {}", e.getMessage());
            }
        }

        auditService.log("UPDATE_TASK_STATUS", "TASK", taskId,
                "Membre équipe '" + currentUser.getEmail() + "' a changé le statut de la tâche '" +
                        task.getTitle() + "' de " + oldStatus + " à " + status,
                getCurrentUserEmail(), null);

        if (status == Status.Validation) {
            auditService.log("SUBMIT_TASK", "TASK", taskId,
                    "Membre équipe '" + currentUser.getEmail() + "' a soumis la tâche '" +
                            task.getTitle() + "' pour validation au chef de projet",
                    getCurrentUserEmail(), null);
        }

        return toDto(updated);
    }

    @Override
    public TaskDto submitTaskForValidation(Long taskId) {
        User currentUser = getCurrentUser();

        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new NotFoundException("TASK_NOT_FOUND", "Tâche introuvable"));

        if (task.isDeleted()) {
            throw new NotFoundException("TASK_NOT_FOUND", "Tâche introuvable");
        }

        if (task.getAssignedTo() == null || !task.getAssignedTo().getId().equals(currentUser.getId())) {
            throw new ForbiddenException("FORBIDDEN", "Cette tâche ne vous est pas assignée");
        }

        if (task.getStatus() != Status.En_cours && task.getStatus() != Status.A_faire) {
            throw new BusinessException("INVALID_STATUS", "Seules les tâches en cours peuvent être soumises pour validation");
        }

        task.setStatus(Status.Validation);
        task.setRejected(false);
        task.setRejectionComment(null);

        Task updated = taskRepository.save(task);

        // === NOTIFICATION EMAIL AU CHEF DE PROJET ===
        if (task.getProject() != null && task.getProject().getTeam() != null &&
                task.getProject().getTeam().getProjectManager() != null) {
            try {
                User chefProjet = task.getProject().getTeam().getProjectManager();
                emailService.notifyTaskSubmittedForValidation(updated, currentUser, chefProjet);
            } catch (Exception e) {
                log.error("Erreur envoi email: {}", e.getMessage());
            }
        }

        auditService.log("SUBMIT_TASK", "TASK", taskId,
                "Membre équipe '" + currentUser.getEmail() + "' a soumis la tâche '" +
                        task.getTitle() + "' pour validation",
                getCurrentUserEmail(), null);

        return toDto(updated);
    }

    @Override
    public TaskDto validateTask(Long taskId, String commentaire) {
        User currentUser = getCurrentUser();

        if (!isChefProjet(currentUser)) {
            throw new ForbiddenException("FORBIDDEN", "Seul un chef de projet peut valider les tâches");
        }

        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new NotFoundException("TASK_NOT_FOUND", "Tâche introuvable"));

        if (task.isDeleted()) {
            throw new NotFoundException("TASK_NOT_FOUND", "Tâche introuvable");
        }

        if (task.getProject() == null || task.getProject().getTeam() == null ||
                task.getProject().getTeam().getProjectManager() == null ||
                !task.getProject().getTeam().getProjectManager().getId().equals(currentUser.getId())) {
            throw new ForbiddenException("FORBIDDEN", "Vous n'êtes pas le chef de projet de ce projet");
        }

        if (task.getStatus() != Status.Validation) {
            throw new BusinessException("INVALID_STATUS", "Cette tâche n'est pas en attente de validation");
        }

        task.setStatus(Status.Terminé);
        task.setRejected(false);
        task.setRejectionComment(null);

        Task updated = taskRepository.save(task);

        // === NOTIFICATION EMAIL AU MEMBRE ===
        if (task.getAssignedTo() != null) {
            try {
                emailService.notifyTaskValidated(updated, task.getAssignedTo(), commentaire);
            } catch (Exception e) {
                log.error("Erreur envoi email: {}", e.getMessage());
            }
        }

        auditService.log("VALIDATE_TASK", "TASK", taskId,
                "Chef de projet '" + currentUser.getEmail() + "' a validé la tâche '" +
                        task.getTitle() + "'. Commentaire: " + (commentaire != null ? commentaire : ""),
                getCurrentUserEmail(), null);

        return toDto(updated);
    }

    @Override
    public TaskDto rejectTask(Long taskId, String commentaire) {
        User currentUser = getCurrentUser();

        if (!isChefProjet(currentUser)) {
            throw new ForbiddenException("FORBIDDEN", "Seul un chef de projet peut rejeter les tâches");
        }

        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new NotFoundException("TASK_NOT_FOUND", "Tâche introuvable"));

        if (task.isDeleted()) {
            throw new NotFoundException("TASK_NOT_FOUND", "Tâche introuvable");
        }

        if (task.getProject() == null || task.getProject().getTeam() == null ||
                task.getProject().getTeam().getProjectManager() == null ||
                !task.getProject().getTeam().getProjectManager().getId().equals(currentUser.getId())) {
            throw new ForbiddenException("FORBIDDEN", "Vous n'êtes pas le chef de projet de ce projet");
        }

        if (task.getStatus() != Status.Validation) {
            throw new BusinessException("INVALID_STATUS", "Cette tâche n'est pas en attente de validation");
        }

        task.setStatus(Status.En_cours);
        task.setRejected(true);
        task.setRejectionComment(commentaire != null ? commentaire.trim() : null);

        Task updated = taskRepository.save(task);

        // === NOTIFICATION EMAIL AU MEMBRE ===
        if (task.getAssignedTo() != null) {
            try {
                emailService.notifyTaskRejected(updated, task.getAssignedTo(), commentaire);
            } catch (Exception e) {
                log.error("Erreur envoi email: {}", e.getMessage());
            }
        }

        auditService.log("REJECT_TASK", "TASK", taskId,
                "Chef de projet '" + currentUser.getEmail() + "' a rejeté la tâche '" +
                        task.getTitle() + "'. Motif: " + (commentaire != null ? commentaire : ""),
                getCurrentUserEmail(), null);

        return toDto(updated);
    }
    @Override
    public void deleteTask(Long id) {
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("TASK_NOT_FOUND", "Tâche introuvable"));

        User currentUser = getCurrentUser();

        if (task.getProject() == null ||
                task.getProject().getTeam() == null ||
                task.getProject().getTeam().getProjectManager() == null ||
                !task.getProject().getTeam().getProjectManager().getId().equals(currentUser.getId())) {
            throw new ForbiddenException("FORBIDDEN", "Vous ne pouvez pas supprimer cette tâche");
        }

        String taskTitle = task.getTitle();
        User assignedUser = task.getAssignedTo();

        // === NOTIFICATION EMAIL DE SUPPRESSION (avant suppression) ===
        if (assignedUser != null) {
            try {
                emailService.notifyTaskDeleted(task, currentUser, assignedUser);
            } catch (Exception e) {
                log.error("Erreur lors de l'envoi de l'email de suppression: {}", e.getMessage());
            }
        }

        task.setDeleted(true);

        auditService.log("DELETE_TASK", "TASK", id,
                "Suppression de la tâche '" + taskTitle + "'",
                getCurrentUserEmail(), null);

        taskRepository.save(task);
    }
    private TaskDto toDto(Task task) {
        TaskDto dto = new TaskDto();
        dto.setId(task.getId());
        dto.setTitle(task.getTitle());
        dto.setDescription(task.getDescription());
        dto.setStatus(task.getStatus());
        dto.setCreatedAt(task.getCreatedAt());
        dto.setStartDate(task.getStartDate());
        dto.setCriticite(task.getCriticite());
        dto.setDureeEstimee(task.getDureeEstimee());
        dto.setEstimatedEndDate(task.getEstimatedEndDate());
        dto.setPriority(task.getPriority());

        if (task.getAssignedTo() != null) {
            dto.setAssignedToId(task.getAssignedTo().getId());
            dto.setAssignedToName(task.getAssignedTo().getPrenom() + " " + task.getAssignedTo().getNom());
        }

        if (task.getProject() != null) {
            dto.setProjectId(task.getProject().getId());
            dto.setProjectName(task.getProject().getName());
        }

        dto.setDeleted(task.isDeleted());
        dto.setRejected(task.isRejected());
        dto.setRejectionComment(task.getRejectionComment());

        return dto;
    }
}

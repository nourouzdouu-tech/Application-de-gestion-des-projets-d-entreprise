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
import com.dxc.dxc_platform.service.TaskService;
import com.dxc.dxc_platform.shared.exception.BusinessException;
import com.dxc.dxc_platform.shared.exception.ForbiddenException;
import com.dxc.dxc_platform.shared.exception.NotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class TaskServiceImpl implements TaskService {

    private final TaskRepository taskRepository;
    private final UserRepository userRepository;
    private final ProjectRepository projectRepository;

    public TaskServiceImpl(TaskRepository taskRepository,
                           UserRepository userRepository,
                           ProjectRepository projectRepository) {
        this.taskRepository = taskRepository;
        this.userRepository = userRepository;
        this.projectRepository = projectRepository;
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

    @Override
    public TaskDto createTask(TaskDto dto) {
        User currentUser = getCurrentUser();

        if (!isChefProjet(currentUser)) {
            throw new ForbiddenException("FORBIDDEN", "Seul un chef de projet peut créer des tâches");
        }

        Project project = projectRepository.findByIdAndDeletedFalse(dto.getProjectId())
                .orElseThrow(() -> new NotFoundException("PROJECT_NOT_FOUND", "Projet introuvable"));

        if (project.getChefProjet() == null || !project.getChefProjet().getId().equals(currentUser.getId())) {
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

        Task task = new Task();
        task.setTitle(dto.getTitle());
        task.setDescription(dto.getDescription());
        task.setStatus(dto.getStatus() != null ? dto.getStatus() : Status.A_faire);
        task.setDueDate(dto.getDueDate());
        task.setAssignedTo(assignedUser);
        task.setProject(project);
        task.setPriority(dto.getPriority() != null ? dto.getPriority() : Priority.MOYENNE);

        Task saved = taskRepository.save(task);
        return toDto(saved);
    }

    @Override
    public TaskDto updateTask(Long id, TaskDto dto) {
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("TASK_NOT_FOUND", "Tâche introuvable"));

        User currentUser = getCurrentUser();

        if (task.getProject() == null ||
                task.getProject().getChefProjet() == null ||
                !task.getProject().getChefProjet().getId().equals(currentUser.getId())) {
            throw new ForbiddenException("FORBIDDEN", "Vous ne pouvez pas modifier cette tâche");
        }

        task.setTitle(dto.getTitle());
        task.setDescription(dto.getDescription());
        task.setDueDate(dto.getDueDate());

        if (dto.getStatus() != null) {
            task.setStatus(dto.getStatus());
        }

        if (dto.getPriority() != null) {
            task.setPriority(dto.getPriority());
        }

        Task updated = taskRepository.save(task);
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

        if (project.getChefProjet() == null) {
            throw new ForbiddenException("FORBIDDEN", "Aucun chef de projet n'est assigné à ce projet");
        }

        if (!project.getChefProjet().getId().equals(currentUser.getId())) {
            throw new ForbiddenException("FORBIDDEN", "Accès non autorisé");
        }

        List<Task> tasks = taskRepository.findAllByProjectIdAndDeletedFalse(projectId);

        return tasks.stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<TaskDto> getMyTasks() {
        User currentUser = getCurrentUser();

        if (currentUser.getTeam() == null) {
            throw new NotFoundException("TEAM_NOT_FOUND", "Aucune équipe associée à l'utilisateur connecté");
        }

        List<Task> tasks = taskRepository.findAllByAssignedToIdAndProjectTeamIdAndDeletedFalse(
                currentUser.getId(),
                currentUser.getTeam().getId()
        );

        return tasks.stream()
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

        task.setStatus(status);
        Task updated = taskRepository.save(task);

        return toDto(updated);
    }

    @Override
    public void deleteTask(Long id) {
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("TASK_NOT_FOUND", "Tâche introuvable"));

        User currentUser = getCurrentUser();

        if (task.getProject() == null ||
                task.getProject().getChefProjet() == null ||
                !task.getProject().getChefProjet().getId().equals(currentUser.getId())) {
            throw new ForbiddenException("FORBIDDEN", "Vous ne pouvez pas supprimer cette tâche");
        }

        task.setDeleted(true);
        taskRepository.save(task);
    }

    private TaskDto toDto(Task task) {
        TaskDto dto = new TaskDto();
        dto.setId(task.getId());
        dto.setTitle(task.getTitle());
        dto.setDescription(task.getDescription());
        dto.setStatus(task.getStatus());
        dto.setCreatedAt(task.getCreatedAt());
        dto.setDueDate(task.getDueDate());
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
        return dto;
    }
}
package com.dxc.dxc_platform.service.impl;

import com.dxc.dxc_platform.dto.TaskDto;
import com.dxc.dxc_platform.entity.*;
import com.dxc.dxc_platform.enums.Status;
import com.dxc.dxc_platform.repository.*;
import com.dxc.dxc_platform.service.TaskService;
import com.dxc.dxc_platform.shared.exception.*;
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

    @Override
    public TaskDto createTask(TaskDto dto) {
        User currentUser = getCurrentUser();
        // Seul un chef de projet peut créer une tâche
        boolean isChef = currentUser.getRoles().stream()
                .anyMatch(r -> r.getNom().equalsIgnoreCase("CHEF_PROJET"));
        if (!isChef) {
            throw new ForbiddenException("FORBIDDEN", "Seul un chef de projet peut créer des tâches");
        }

        // Vérifier que le projet existe et que le chef est bien le chefProjet du projet
        Project project = projectRepository.findByIdAndDeletedFalse(dto.getProjectId())
                .orElseThrow(() -> new NotFoundException("PROJECT_NOT_FOUND", "Projet introuvable"));
        if (project.getChefProjet() == null || !project.getChefProjet().getId().equals(currentUser.getId())) {
            throw new ForbiddenException("FORBIDDEN", "Vous n'êtes pas le chef de projet assigné à ce projet");
        }

        // Vérifier que le membre assigné existe et fait partie de l'équipe du projet
        User assignedUser = userRepository.findByIdAndDeletedFalse(dto.getAssignedToId())
                .orElseThrow(() -> new NotFoundException("USER_NOT_FOUND", "Utilisateur cible introuvable"));
        if (assignedUser.getTeam() == null || !assignedUser.getTeam().getId().equals(project.getTeam().getId())) {
            throw new ForbiddenException("FORBIDDEN", "Cet utilisateur n'appartient pas à l'équipe du projet");
        }

        Task task = new Task();
        task.setTitle(dto.getTitle());
        task.setDescription(dto.getDescription());
        task.setStatus(dto.getStatus() != null ? dto.getStatus() : Status.A_faire);
        task.setDueDate(dto.getDueDate());
        task.setAssignedTo(assignedUser);
        task.setProject(project);

        Task saved = taskRepository.save(task);
        return toDto(saved);
    }

    @Override
    public TaskDto updateTask(Long id, TaskDto dto) {
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("TASK_NOT_FOUND", "Tâche introuvable"));
        User currentUser = getCurrentUser();
        // Vérifier que l'utilisateur est le chef de projet du projet associé
        if (task.getProject().getChefProjet() == null ||
                !task.getProject().getChefProjet().getId().equals(currentUser.getId())) {
            throw new ForbiddenException("FORBIDDEN", "Vous ne pouvez pas modifier cette tâche");
        }
        task.setTitle(dto.getTitle());
        task.setDescription(dto.getDescription());
        task.setDueDate(dto.getDueDate());
        if (dto.getStatus() != null) {
            task.setStatus(dto.getStatus());
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
        Project project = projectRepository.findByIdAndDeletedFalse(projectId)
                .orElseThrow(() -> new NotFoundException("PROJECT_NOT_FOUND", "Projet introuvable"));
        // Seul le chef de projet du projet peut voir les tâches
        if (project.getChefProjet() == null || !project.getChefProjet().getId().equals(currentUser.getId())) {
            throw new ForbiddenException("FORBIDDEN", "Accès non autorisé");
        }
        List<Task> tasks = taskRepository.findAllByProjectIdAndDeletedFalse(projectId);
        return tasks.stream().map(this::toDto).collect(Collectors.toList());
    }

    @Override
    public List<TaskDto> getMyTasks() {
        User currentUser = getCurrentUser();
        List<Task> tasks = taskRepository.findAllByAssignedToIdAndDeletedFalse(currentUser.getId());
        return tasks.stream().map(this::toDto).collect(Collectors.toList());
    }

    @Override
    public void deleteTask(Long id) {
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("TASK_NOT_FOUND", "Tâche introuvable"));
        User currentUser = getCurrentUser();
        if (task.getProject().getChefProjet() == null ||
                !task.getProject().getChefProjet().getId().equals(currentUser.getId())) {
            throw new ForbiddenException("FORBIDDEN", "Vous ne pouvez pas supprimer cette tâche");
        }
        task.setDeleted(true);
        taskRepository.save(task);
    }

    // Implémentez les autres méthodes (update, getById, getByProject, getMyTasks, delete) de façon similaire
    // ...

    private TaskDto toDto(Task task) {
        TaskDto dto = new TaskDto();
        dto.setId(task.getId());
        dto.setTitle(task.getTitle());
        dto.setDescription(task.getDescription());
        dto.setStatus(task.getStatus());
        dto.setCreatedAt(task.getCreatedAt());
        dto.setDueDate(task.getDueDate());
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
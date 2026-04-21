package com.dxc.dxc_platform.service;

import com.dxc.dxc_platform.dto.TaskDto;
import com.dxc.dxc_platform.enums.Priority;
import com.dxc.dxc_platform.enums.Status;

import java.util.List;

public interface TaskService {
    TaskDto createTask(TaskDto dto);
    TaskDto updateTask(Long id, TaskDto dto);
    TaskDto getTaskById(Long id);
    List<TaskDto> getTasksByProject(Long projectId);
    List<TaskDto> getMyTasks(String query, Priority priority, Long assignedToId);
    TaskDto updateMyTaskStatus(Long taskId, Status status);
    void deleteTask(Long id);

    // ✅ Nouvelle méthode pour que le chef de projet valide une tâche
    TaskDto validateTask(Long taskId, String commentaire);

    // ✅ Nouvelle méthode pour que le chef de projet rejette une tâche
    TaskDto rejectTask(Long taskId, String commentaire);

    // ✅ Nouvelle méthode pour soumettre une tâche pour validation
    TaskDto submitTaskForValidation(Long taskId);
}


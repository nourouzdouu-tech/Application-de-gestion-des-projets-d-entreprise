package com.dxc.dxc_platform.service;

import com.dxc.dxc_platform.dto.TaskDto;
import com.dxc.dxc_platform.enums.Status;
import java.util.List;

public interface TaskService {
    TaskDto createTask(TaskDto dto);
    TaskDto updateTask(Long id, TaskDto dto);
    TaskDto getTaskById(Long id);
    List<TaskDto> getTasksByProject(Long projectId);
    List<TaskDto> getMyTasks();
    TaskDto updateMyTaskStatus(Long taskId, Status status);
    void deleteTask(Long id);
}
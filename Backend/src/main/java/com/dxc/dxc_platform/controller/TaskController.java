package com.dxc.dxc_platform.controller;

import com.dxc.dxc_platform.dto.TaskDto;
import com.dxc.dxc_platform.service.TaskService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tasks")
public class TaskController {

    private final TaskService taskService;

    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    @PostMapping
    @PreAuthorize("hasRole('CHEF_PROJET')")
    public ResponseEntity<TaskDto> createTask(@Valid @RequestBody TaskDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(taskService.createTask(dto));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('CHEF_PROJET')")
    public ResponseEntity<TaskDto> updateTask(@PathVariable Long id, @Valid @RequestBody TaskDto dto) {
        return ResponseEntity.ok(taskService.updateTask(id, dto));
    }

    @GetMapping("/project/{projectId}")
    @PreAuthorize("hasRole('CHEF_PROJET')")
    public ResponseEntity<List<TaskDto>> getTasksByProject(@PathVariable Long projectId) {
        return ResponseEntity.ok(taskService.getTasksByProject(projectId));
    }

    @GetMapping("/my-tasks")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<TaskDto>> getMyTasks() {
        return ResponseEntity.ok(taskService.getMyTasks());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('CHEF_PROJET')")
    public ResponseEntity<Void> deleteTask(@PathVariable Long id) {
        taskService.deleteTask(id);
        return ResponseEntity.noContent().build();
    }
}
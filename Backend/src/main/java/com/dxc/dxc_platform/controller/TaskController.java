package com.dxc.dxc_platform.controller;

import com.dxc.dxc_platform.dto.TaskDto;
import com.dxc.dxc_platform.enums.Priority;
import com.dxc.dxc_platform.enums.Status;
import com.dxc.dxc_platform.service.TaskService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

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
    public ResponseEntity<List<TaskDto>> getMyTasks(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) Priority priority,
            @RequestParam(required = false) Long assignedToId
    ) {
        return ResponseEntity.ok(taskService.getMyTasks(query, priority, assignedToId));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<TaskDto> updateMyTaskStatus(@PathVariable Long id, @RequestParam Status status) {
        return ResponseEntity.ok(taskService.updateMyTaskStatus(id, status));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('CHEF_PROJET')")
    public ResponseEntity<Void> deleteTask(@PathVariable Long id) {
        taskService.deleteTask(id);
        return ResponseEntity.noContent().build();
    }
    @PostMapping("/{id}/submit")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<TaskDto> submitTaskForValidation(@PathVariable Long id) {
        return ResponseEntity.ok(taskService.submitTaskForValidation(id));
    }

    // ✅ Valider une tâche (chef de projet)
    @PostMapping("/{id}/validate")
    @PreAuthorize("hasRole('CHEF_PROJET')")
    public ResponseEntity<TaskDto> validateTask(
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, String> body) {
        String commentaire = body != null ? body.get("commentaire") : null;
        return ResponseEntity.ok(taskService.validateTask(id, commentaire));
    }

    // ✅ Rejeter une tâche (chef de projet)
    @PostMapping("/{id}/reject")
    @PreAuthorize("hasRole('CHEF_PROJET')")
    public ResponseEntity<TaskDto> rejectTask(
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, String> body) {
        String commentaire = body != null ? body.get("commentaire") : null;
        return ResponseEntity.ok(taskService.rejectTask(id, commentaire));
    }
}
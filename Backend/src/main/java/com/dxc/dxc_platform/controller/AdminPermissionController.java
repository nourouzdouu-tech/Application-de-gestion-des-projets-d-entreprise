package com.dxc.dxc_platform.controller;

import com.dxc.dxc_platform.dto.PermissionDto;
import com.dxc.dxc_platform.service.PermissionAdminService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/permissions")
public class AdminPermissionController {

    private final PermissionAdminService permissionService;

    public AdminPermissionController(PermissionAdminService permissionService) {
        this.permissionService = permissionService;
    }

    @PostMapping
    public ResponseEntity<PermissionDto.Response> create(
            @Valid @RequestBody PermissionDto.CreateRequest request
    ) {
        return ResponseEntity.ok(permissionService.create(request));
    }

    @GetMapping
    public ResponseEntity<List<PermissionDto.Response>> list() {
        return ResponseEntity.ok(permissionService.list());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        permissionService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}")
    public ResponseEntity<PermissionDto.Response> update(
            @PathVariable Long id,
            @Valid @RequestBody PermissionDto.UpdateRequest request
    ) {
        return ResponseEntity.ok(permissionService.update(id, request));
    }
}
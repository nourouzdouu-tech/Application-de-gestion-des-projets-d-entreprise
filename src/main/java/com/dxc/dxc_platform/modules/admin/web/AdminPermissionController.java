package com.dxc.dxc_platform.modules.admin.web;

import com.dxc.dxc_platform.modules.admin.service.PermissionAdminService;
import com.dxc.dxc_platform.modules.admin.web.dto.permission.CreatePermissionRequest;
import com.dxc.dxc_platform.modules.admin.web.dto.permission.PermissionResponse;
import com.dxc.dxc_platform.modules.admin.web.dto.permission.UpdatePermissionRequest;
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
    public ResponseEntity<PermissionResponse> create(
            @Valid @RequestBody CreatePermissionRequest request
    ) {
        return ResponseEntity.ok(permissionService.create(request));
    }

    @GetMapping
    public ResponseEntity<List<PermissionResponse>> list() {
        return ResponseEntity.ok(permissionService.list());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        permissionService.delete(id);
        return ResponseEntity.noContent().build();
    }
    @PutMapping("/{id}")
    public ResponseEntity<PermissionResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody UpdatePermissionRequest request
    ) {
        return ResponseEntity.ok(permissionService.update(id, request));
    }
}
package com.dxc.dxc_platform.modules.admin.web;

import com.dxc.dxc_platform.modules.admin.service.RoleAdminService;
import com.dxc.dxc_platform.modules.admin.web.dto.role.*;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/roles")
public class AdminRoleController {

    private final RoleAdminService roleAdminService;

    public AdminRoleController(RoleAdminService roleAdminService) {
        this.roleAdminService = roleAdminService;
    }

    @PostMapping
    public ResponseEntity<RoleResponse> create(@Valid @RequestBody CreateRoleRequest req) {
        return ResponseEntity.ok(roleAdminService.create(req));
    }

    @GetMapping
    public ResponseEntity<List<RoleResponse>> list() {
        return ResponseEntity.ok(roleAdminService.list());
    }

    @GetMapping("/{id}")
    public ResponseEntity<RoleResponse> get(@PathVariable Long id) {
        return ResponseEntity.ok(roleAdminService.get(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<RoleResponse> update(@PathVariable Long id,
                                               @Valid @RequestBody UpdateRoleRequest req) {
        return ResponseEntity.ok(roleAdminService.update(id, req));
    }

    @PatchMapping("/{id}/activate")
    public ResponseEntity<Void> activate(@PathVariable Long id) {
        roleAdminService.activate(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/deactivate")
    public ResponseEntity<Void> deactivate(@PathVariable Long id) {
        roleAdminService.deactivate(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/permissions")
    public ResponseEntity<RoleResponse> updatePermissions(@PathVariable Long id,
                                                          @RequestBody UpdateRolePermissionsRequest req) {
        return ResponseEntity.ok(roleAdminService.updatePermissions(id, req));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        roleAdminService.softDelete(id);
        return ResponseEntity.noContent().build();
    }
}
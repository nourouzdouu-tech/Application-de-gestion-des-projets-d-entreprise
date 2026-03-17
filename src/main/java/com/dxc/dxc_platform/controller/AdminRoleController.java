package com.dxc.dxc_platform.controller;

import com.dxc.dxc_platform.dto.RoleDto;
import com.dxc.dxc_platform.service.RoleAdminService;
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
    public ResponseEntity<RoleDto.Response> create(@Valid @RequestBody RoleDto.CreateRequest req) {
        return ResponseEntity.ok(roleAdminService.create(req));
    }

    @GetMapping
    public ResponseEntity<List<RoleDto.Response>> list() {
        return ResponseEntity.ok(roleAdminService.list());
    }

    @GetMapping("/{id}")
    public ResponseEntity<RoleDto.Response> get(@PathVariable Long id) {
        return ResponseEntity.ok(roleAdminService.get(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<RoleDto.Response> update(@PathVariable Long id,
                                                   @Valid @RequestBody RoleDto.UpdateRequest req) {
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
    public ResponseEntity<RoleDto.Response> updatePermissions(@PathVariable Long id,
                                                              @RequestBody RoleDto.UpdatePermissionsRequest req) {
        return ResponseEntity.ok(roleAdminService.updatePermissions(id, req));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        roleAdminService.softDelete(id);
        return ResponseEntity.noContent().build();
    }
}
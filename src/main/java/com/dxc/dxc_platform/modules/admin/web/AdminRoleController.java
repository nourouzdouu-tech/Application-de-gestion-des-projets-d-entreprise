
package com.dxc.dxc_platform.modules.admin.web;

import com.dxc.dxc_platform.modules.admin.domain.entity.Role;
import com.dxc.dxc_platform.modules.admin.service.RoleAdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

        import java.util.List;

@RestController
@RequestMapping("/api/admin/roles")
@RequiredArgsConstructor
public class AdminRoleController {

    private final RoleAdminService roleAdminService;

    @PostMapping
    public Role createRole(@RequestBody Role role) {
        return roleAdminService.createRole(role);
    }

    @GetMapping
    public List<Role> getAllRoles() {
        return roleAdminService.getAllRoles();
    }

    @GetMapping("/{id}")
    public Role getRoleById(@PathVariable Long id) {
        return roleAdminService.getRoleById(id);
    }

    @DeleteMapping("/{id}")
    public void deleteRole(@PathVariable Long id) {
        roleAdminService.deleteRole(id);
    }
}
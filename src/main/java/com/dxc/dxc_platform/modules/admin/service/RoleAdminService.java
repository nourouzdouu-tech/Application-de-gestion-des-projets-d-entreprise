package com.dxc.dxc_platform.modules.admin.service;

import com.dxc.dxc_platform.modules.admin.domain.entity.Role;
import com.dxc.dxc_platform.modules.admin.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RoleAdminService {

    private final RoleRepository roleRepository;

    public Role createRole(Role role) {

        if (roleRepository.existsByNom(role.getNom())) {
            throw new RuntimeException("Role already exists");
        }

        return roleRepository.save(role);
    }

    public List<Role> getAllRoles() {
        return roleRepository.findAll();
    }

    public Role getRoleById(Long id) {
        return roleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Role not found"));
    }

    public void deleteRole(Long id) {
        Role role = getRoleById(id);
        role.setDeleted(true); // soft delete
        roleRepository.save(role);
    }
}
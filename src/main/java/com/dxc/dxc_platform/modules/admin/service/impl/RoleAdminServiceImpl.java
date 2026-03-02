package com.dxc.dxc_platform.modules.admin.service.impl;

import com.dxc.dxc_platform.modules.admin.domain.entity.Permission;
import com.dxc.dxc_platform.modules.admin.domain.entity.Role;
import com.dxc.dxc_platform.modules.admin.repository.PermissionRepository;
import com.dxc.dxc_platform.modules.admin.repository.RoleRepository;
import com.dxc.dxc_platform.modules.admin.service.RoleAdminService;
import com.dxc.dxc_platform.modules.admin.web.dto.role.*;
import com.dxc.dxc_platform.shared.exception.ConflictException;
import com.dxc.dxc_platform.shared.exception.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Transactional
public class RoleAdminServiceImpl implements RoleAdminService {

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;

    public RoleAdminServiceImpl(RoleRepository roleRepository,
                                PermissionRepository permissionRepository) {
        this.roleRepository = roleRepository;
        this.permissionRepository = permissionRepository;
    }

    @Override
    public RoleResponse create(CreateRoleRequest req) {
        if (roleRepository.existsByNomAndDeletedFalse(req.nom())) {
            throw new ConflictException("ROLE_ALREADY_EXISTS", "Rôle existe déjà: " + req.nom());
        }

        Role role = new Role(req.nom(), req.description());
        role.setActive(true);
        role.setDeleted(false);
        role.setPermissions(resolvePermissions(req.permissionCodes()));

        role = roleRepository.save(role);
        return toResponse(role);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RoleResponse> list() {
        return roleRepository.findAllByDeletedFalse()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public RoleResponse get(Long id) {
        Role role = roleRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new NotFoundException("ROLE_NOT_FOUND", "Rôle introuvable: " + id));
        return toResponse(role);
    }

    @Override
    public RoleResponse update(Long id, UpdateRoleRequest req) {
        Role role = roleRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new NotFoundException("ROLE_NOT_FOUND", "Rôle introuvable: " + id));

        if (!role.getNom().equalsIgnoreCase(req.nom())
                && roleRepository.existsByNomAndDeletedFalse(req.nom())) {
            throw new ConflictException("ROLE_ALREADY_EXISTS", "Rôle existe déjà: " + req.nom());
        }

        role.setNom(req.nom());
        role.setDescription(req.description());
        return toResponse(role);
    }

    @Override
    public void activate(Long id) {
        Role role = roleRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new NotFoundException("ROLE_NOT_FOUND", "Rôle introuvable: " + id));
        role.setActive(true);
    }

    @Override
    public void deactivate(Long id) {
        Role role = roleRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new NotFoundException("ROLE_NOT_FOUND", "Rôle introuvable: " + id));
        role.setActive(false);
    }

    @Override
    public RoleResponse updatePermissions(Long id, UpdateRolePermissionsRequest req) {
        Role role = roleRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new NotFoundException("ROLE_NOT_FOUND", "Rôle introuvable: " + id));

        role.setPermissions(resolvePermissions(req.permissionCodes()));
        return toResponse(role);
    }

    @Override
    public void softDelete(Long id) {
        Role role = roleRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new NotFoundException("ROLE_NOT_FOUND", "Rôle introuvable: " + id));
        role.setDeleted(true);
        role.setActive(false);
    }

    private Set<Permission> resolvePermissions(Set<String> codes) {
        if (codes == null || codes.isEmpty()) {
            return new HashSet<>();
        }

        return codes.stream()
                .map(code -> permissionRepository.findByNom(code)
                        .orElseThrow(() -> new NotFoundException(
                                "PERMISSION_NOT_FOUND",
                                "Permission introuvable: " + code
                        )))
                .collect(Collectors.toSet());
    }

    private RoleResponse toResponse(Role r) {
        Set<String> perms = r.getPermissions().stream()
                .map(Permission::getNom)
                .collect(Collectors.toSet());

        return new RoleResponse(
                r.getId(),
                r.getNom(),
                r.getDescription(),
                r.isActive(),
                perms
        );
    }
}
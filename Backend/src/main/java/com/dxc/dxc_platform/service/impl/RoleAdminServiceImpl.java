package com.dxc.dxc_platform.service.impl;

import com.dxc.dxc_platform.dto.RoleDto;
import com.dxc.dxc_platform.entity.Permission;
import com.dxc.dxc_platform.entity.Role;
import com.dxc.dxc_platform.mapper.RoleMapper;
import com.dxc.dxc_platform.repository.PermissionRepository;
import com.dxc.dxc_platform.repository.RoleRepository;
import com.dxc.dxc_platform.service.RoleAdminService;
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
    private final RoleMapper roleMapper;

    public RoleAdminServiceImpl(RoleRepository roleRepository,
                                PermissionRepository permissionRepository,
                                RoleMapper roleMapper) {
        this.roleRepository = roleRepository;
        this.permissionRepository = permissionRepository;
        this.roleMapper = roleMapper;
    }

    @Override
    public RoleDto.Response create(RoleDto.CreateRequest req) {
        if (roleRepository.existsByNom(req.nom())) {
            throw new ConflictException("ROLE_ALREADY_EXISTS", "Rôle existe déjà: " + req.nom());
        }

        Role role = new Role(req.nom(), req.description());
        role.setActive(true);
        role.setPermissions(resolvePermissions(req.permissionCodes()));

        role = roleRepository.save(role);
        return roleMapper.toResponse(role);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RoleDto.Response> list() {
        return roleRepository.findAll()
                .stream()
                .map(role -> {
                    RoleDto.Response response = roleMapper.toResponse(role);
                    Long userCount = roleRepository.countUsersForRole(role.getId());
                    return new RoleDto.Response(
                            response.id(),
                            response.nom(),
                            response.description(),
                            response.active(),
                            response.permissions(),
                            userCount.intValue()
                    );
                })
                .toList();
    }


    @Override
    @Transactional(readOnly = true)
    public RoleDto.Response get(Long id) {
        Role role = roleRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("ROLE_NOT_FOUND", "Rôle introuvable: " + id));

        RoleDto.Response response = roleMapper.toResponse(role);
        Long userCount = roleRepository.countUsersForRole(role.getId());

        return new RoleDto.Response(
                response.id(),
                response.nom(),
                response.description(),
                response.active(),
                response.permissions(),
                userCount.intValue()
        );
    }

    @Override
    public RoleDto.Response update(Long id, RoleDto.UpdateRequest req) {
        Role role = roleRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("ROLE_NOT_FOUND", "Rôle introuvable: " + id));

        if (!role.getNom().equalsIgnoreCase(req.nom())
                && roleRepository.existsByNom(req.nom())) {
            throw new ConflictException("ROLE_ALREADY_EXISTS", "Rôle existe déjà: " + req.nom());
        }

        role.setNom(req.nom());
        role.setDescription(req.description());

        if (req.active() != null) {
            role.setActive(req.active());
        }

        if (req.permissionIds() != null) {
            List<Permission> permissions = permissionRepository.findAllById(req.permissionIds());
            role.setPermissions(new HashSet<>(permissions));
        }

        role = roleRepository.save(role);
        return roleMapper.toResponse(role);
    }

    @Override
    public void activate(Long id) {
        Role role = roleRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("ROLE_NOT_FOUND", "Rôle introuvable: " + id));
        role.setActive(true);
        roleRepository.save(role);
    }

    @Override
    public void deactivate(Long id) {
        Role role = roleRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("ROLE_NOT_FOUND", "Rôle introuvable: " + id));
        role.setActive(false);
        roleRepository.save(role);
    }

    @Override
    public RoleDto.Response updatePermissions(Long id, RoleDto.UpdatePermissionsRequest req) {
        Role role = roleRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("ROLE_NOT_FOUND", "Rôle introuvable: " + id));

        role.setPermissions(resolvePermissions(req.permissionCodes()));
        role = roleRepository.save(role);
        return roleMapper.toResponse(role);
    }

    @Override
    public void softDelete(Long id) {
        Role role = roleRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("ROLE_NOT_FOUND", "Rôle introuvable: " + id));

        roleRepository.delete(role);
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

}
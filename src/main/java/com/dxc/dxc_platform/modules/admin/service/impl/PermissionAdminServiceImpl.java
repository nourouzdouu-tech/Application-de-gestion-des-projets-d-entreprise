package com.dxc.dxc_platform.modules.admin.service.impl;

import com.dxc.dxc_platform.modules.admin.domain.entity.Permission;
import com.dxc.dxc_platform.modules.admin.repository.PermissionRepository;
import com.dxc.dxc_platform.modules.admin.service.PermissionAdminService;
import com.dxc.dxc_platform.modules.admin.web.dto.permission.CreatePermissionRequest;
import com.dxc.dxc_platform.modules.admin.web.dto.permission.PermissionResponse;
import com.dxc.dxc_platform.modules.admin.web.dto.permission.UpdatePermissionRequest;
import com.dxc.dxc_platform.shared.exception.ConflictException;
import com.dxc.dxc_platform.shared.exception.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class PermissionAdminServiceImpl implements PermissionAdminService {

    private final PermissionRepository permissionRepository;

    public PermissionAdminServiceImpl(PermissionRepository permissionRepository) {
        this.permissionRepository = permissionRepository;
    }

    @Override
    public PermissionResponse create(CreatePermissionRequest request) {

        if (permissionRepository.existsByNom(request.nom())) {
            throw new ConflictException(
                    "PERMISSION_ALREADY_EXISTS",
                    "Permission existe déjà: " + request.nom()
            );
        }

        Permission permission = new Permission(
                request.nom(),
                request.description()
        );

        permission = permissionRepository.save(permission);

        return new PermissionResponse(
                permission.getId(),
                permission.getNom(),
                permission.getDescription()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<PermissionResponse> list() {
        return permissionRepository.findAll()
                .stream()
                .map(p -> new PermissionResponse(
                        p.getId(),
                        p.getNom(),
                        p.getDescription()
                ))
                .toList();
    }

    @Override
    public void delete(Long id) {

        Permission permission = permissionRepository.findById(id)
                .orElseThrow(() -> new NotFoundException(
                        "PERMISSION_NOT_FOUND",
                        "Permission introuvable: " + id
                ));

        permissionRepository.delete(permission);
    }
    @Override
    public PermissionResponse update(Long id, UpdatePermissionRequest request) {

        Permission permission = permissionRepository.findById(id)
                .orElseThrow(() ->
                        new NotFoundException("PERMISSION_NOT_FOUND",
                                "Permission introuvable: " + id));

        // Vérifier unicité du nom si changé
        if (!permission.getNom().equalsIgnoreCase(request.nom())
                && permissionRepository.existsByNom(request.nom())) {

            throw new ConflictException("PERMISSION_ALREADY_EXISTS",
                    "Permission existe déjà: " + request.nom());
        }

        permission.setNom(request.nom());
        permission.setDescription(request.description());

        permission = permissionRepository.save(permission);

        return new PermissionResponse(
                permission.getId(),
                permission.getNom(),
                permission.getDescription()
        );
    }

}
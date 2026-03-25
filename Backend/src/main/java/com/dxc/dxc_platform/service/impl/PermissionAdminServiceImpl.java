package com.dxc.dxc_platform.service.impl;

import com.dxc.dxc_platform.dto.PermissionDto;
import com.dxc.dxc_platform.entity.Permission;
import com.dxc.dxc_platform.mapper.PermissionMapper;
import com.dxc.dxc_platform.repository.PermissionRepository;
import com.dxc.dxc_platform.service.PermissionAdminService;
import com.dxc.dxc_platform.shared.exception.ConflictException;
import com.dxc.dxc_platform.shared.exception.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class PermissionAdminServiceImpl implements PermissionAdminService {

    private final PermissionRepository permissionRepository;
    private final PermissionMapper permissionMapper;

    public PermissionAdminServiceImpl(PermissionRepository permissionRepository,
                                      PermissionMapper permissionMapper) {
        this.permissionRepository = permissionRepository;
        this.permissionMapper = permissionMapper;
    }

    @Override
    public PermissionDto.Response create(PermissionDto.CreateRequest request) {
        if (permissionRepository.existsByNom(request.nom())) {
            throw new ConflictException(
                    "PERMISSION_ALREADY_EXISTS",
                    "Permission existe déjà: " + request.nom()
            );
        }

        Permission permission = new Permission(request.nom(), request.description());
        permission = permissionRepository.save(permission);

        return permissionMapper.toResponse(permission);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PermissionDto.Response> list() {
        return permissionRepository.findAll()
                .stream()
                .map(permissionMapper::toResponse)
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
    public PermissionDto.Response update(Long id, PermissionDto.UpdateRequest request) {
        Permission permission = permissionRepository.findById(id)
                .orElseThrow(() -> new NotFoundException(
                        "PERMISSION_NOT_FOUND",
                        "Permission introuvable: " + id
                ));

        if (!permission.getNom().equalsIgnoreCase(request.nom())
                && permissionRepository.existsByNom(request.nom())) {
            throw new ConflictException(
                    "PERMISSION_ALREADY_EXISTS",
                    "Permission existe déjà: " + request.nom()
            );
        }

        permission.setNom(request.nom());
        permission.setDescription(request.description());

        permission = permissionRepository.save(permission);
        return permissionMapper.toResponse(permission);
        
    }
}
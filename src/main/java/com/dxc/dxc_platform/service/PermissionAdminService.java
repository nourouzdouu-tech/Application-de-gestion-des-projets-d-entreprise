package com.dxc.dxc_platform.service;

import com.dxc.dxc_platform.dto.PermissionDto.*;

import java.util.List;

public interface PermissionAdminService {
    PermissionResponse update(Long id, UpdatePermissionRequest request);
    PermissionResponse create(CreatePermissionRequest request);
    List<PermissionResponse> list();
    void delete(Long id);
}
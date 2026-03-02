package com.dxc.dxc_platform.modules.admin.service;

import com.dxc.dxc_platform.modules.admin.web.dto.permission.CreatePermissionRequest;
import com.dxc.dxc_platform.modules.admin.web.dto.permission.PermissionResponse;
import com.dxc.dxc_platform.modules.admin.web.dto.permission.UpdatePermissionRequest;

import java.util.List;

public interface PermissionAdminService {
    PermissionResponse update(Long id, UpdatePermissionRequest request);
    PermissionResponse create(CreatePermissionRequest request);

    List<PermissionResponse> list();

    void delete(Long id);
}
package com.dxc.dxc_platform.modules.admin.service;

import com.dxc.dxc_platform.modules.admin.web.dto.role.*;

import java.util.List;

public interface RoleAdminService {

    RoleResponse create(CreateRoleRequest req);

    List<RoleResponse> list();

    RoleResponse get(Long id);

    RoleResponse update(Long id, UpdateRoleRequest req);

    void activate(Long id);

    void deactivate(Long id);

    RoleResponse updatePermissions(Long id, UpdateRolePermissionsRequest req);

    void softDelete(Long id);
}
package com.dxc.dxc_platform.service;

import com.dxc.dxc_platform.dto.role.CreateRoleRequest;
import com.dxc.dxc_platform.dto.role.RoleResponse;
import com.dxc.dxc_platform.dto.role.UpdateRolePermissionsRequest;
import com.dxc.dxc_platform.dto.role.UpdateRoleRequest;

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
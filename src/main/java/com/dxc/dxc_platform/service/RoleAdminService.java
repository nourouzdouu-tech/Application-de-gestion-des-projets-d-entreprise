package com.dxc.dxc_platform.service;

import com.dxc.dxc_platform.dto.RoleDto;

import java.util.List;

public interface RoleAdminService {

    RoleDto.Response create(RoleDto.CreateRequest req);

    List<RoleDto.Response> list();

    RoleDto.Response get(Long id);

    RoleDto.Response update(Long id, RoleDto.UpdateRequest req);

    void activate(Long id);

    void deactivate(Long id);

    RoleDto.Response updatePermissions(Long id, RoleDto.UpdatePermissionsRequest req);

    void softDelete(Long id);
}
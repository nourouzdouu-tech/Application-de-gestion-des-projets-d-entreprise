package com.dxc.dxc_platform.service;

import com.dxc.dxc_platform.dto.PermissionDto;

import java.util.List;

public interface PermissionAdminService {

    PermissionDto.Response create(PermissionDto.CreateRequest request);

    List<PermissionDto.Response> list();

    void delete(Long id);

    PermissionDto.Response update(Long id, PermissionDto.UpdateRequest request);
}
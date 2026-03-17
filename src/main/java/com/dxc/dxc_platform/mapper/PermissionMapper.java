package com.dxc.dxc_platform.mapper;

import com.dxc.dxc_platform.dto.PermissionDto;
import com.dxc.dxc_platform.entity.Permission;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface PermissionMapper {

    PermissionDto.Response toResponse(Permission permission);

    PermissionDto.Summary toSummary(Permission permission);
}
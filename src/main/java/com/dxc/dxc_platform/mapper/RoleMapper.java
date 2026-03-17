package com.dxc.dxc_platform.mapper;

import com.dxc.dxc_platform.dto.RoleDto;
import com.dxc.dxc_platform.entity.Role;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring", uses = PermissionMapper.class)
public interface RoleMapper {

    RoleDto.Response toResponse(Role role);

    RoleDto.Summary toSummary(Role role);
}
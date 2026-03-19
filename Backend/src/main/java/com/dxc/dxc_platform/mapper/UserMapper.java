package com.dxc.dxc_platform.mapper;

import com.dxc.dxc_platform.dto.UserDto;
import com.dxc.dxc_platform.entity.User;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring", uses = RoleMapper.class)
public interface UserMapper {

    UserDto.Response toResponse(User user);

    UserDto.Summary toSummary(User user);
}
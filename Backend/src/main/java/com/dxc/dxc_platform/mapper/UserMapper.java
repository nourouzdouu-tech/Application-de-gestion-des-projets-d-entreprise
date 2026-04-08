package com.dxc.dxc_platform.mapper;

import com.dxc.dxc_platform.dto.UserDto;
import com.dxc.dxc_platform.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = RoleMapper.class)
public interface UserMapper {

    @Mapping(source = "profile.id", target = "profileId")
    @Mapping(source = "profile.libelle", target = "profileLibelle")
    UserDto.Response toResponse(User user);

    UserDto.Summary toSummary(User user);
}
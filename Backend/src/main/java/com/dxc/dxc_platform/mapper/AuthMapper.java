package com.dxc.dxc_platform.mapper;

import com.dxc.dxc_platform.dto.AuthDto;
import com.dxc.dxc_platform.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.Set;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring")
public interface AuthMapper {

    @Mapping(target = "accessToken", ignore = true)
    @Mapping(target = "tokenType", constant = "Bearer")
    @Mapping(target = "roles", expression = "java(toRoleNames(user))")
    @Mapping(target = "redirectTo", ignore = true)
    AuthDto.Response toResponse(User user);

    default Set<String> toRoleNames(User user) {
        return user.getRoles().stream()
                .map(role -> role.getNom())
                .collect(Collectors.toSet());
    }
}

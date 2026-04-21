package com.dxc.dxc_platform.mapper;

import com.dxc.dxc_platform.dto.TeamDto;
import com.dxc.dxc_platform.entity.Role;
import com.dxc.dxc_platform.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.Set;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring")
public interface TeamMemberMapper {

    @Mapping(target = "fullName", expression = "java(buildFullName(user))")
    @Mapping(target = "enabled", expression = "java(isEnabled(user))")
    @Mapping(target = "roleName", expression = "java(extractRoles(user.getRoles()))")
    @Mapping(source = "profile.id", target = "profileId")
    @Mapping(source = "profile.libelle", target = "profileLibelle")
    @Mapping(source = "profile.tjm", target = "tjm")
    TeamDto.MemberInfo toMemberInfo(User user);

    @Named("buildFullName")
    default String buildFullName(User user) {
        String prenom = user.getPrenom() != null ? user.getPrenom() : "";
        String nom = user.getNom() != null ? user.getNom() : "";
        return (prenom + " " + nom).trim();
    }

    @Named("isEnabled")
    default Boolean isEnabled(User user) {
        return !user.isDeleted() && !user.isLocked();
    }

    @Named("extractRoles")
    default String extractRoles(Set<Role> roles) {
        if (roles == null || roles.isEmpty()) {
            return null;
        }

        return roles.stream()
                .map(Role::getNom)
                .collect(Collectors.joining(", "));
    }

    @Named("projectManagerName")
    default String projectManagerName(User user) {
        if (user == null) {
            return null;
        }
        return buildFullName(user);
    }
}
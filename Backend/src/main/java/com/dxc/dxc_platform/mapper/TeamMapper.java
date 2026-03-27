package com.dxc.dxc_platform.mapper;

import com.dxc.dxc_platform.dto.TeamDto;
import com.dxc.dxc_platform.entity.Team;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = TeamMemberMapper.class)
public interface TeamMapper {

    @Mapping(source = "projectManager.id", target = "projectManagerId")
    @Mapping(source = "projectManager", target = "projectManagerName", qualifiedByName = "projectManagerName")
    @Mapping(source = "members", target = "members")
    @Mapping(source = "deleted", target = "deleted")
    TeamDto toDto(Team team);
}
package com.dxc.dxc_platform.mapper;

import com.dxc.dxc_platform.dto.ProfileDto;
import com.dxc.dxc_platform.entity.Profile;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ProfileMapper {

    @Mapping(source = "deleted", target = "deleted")
    ProfileDto toDto(Profile profile);
}
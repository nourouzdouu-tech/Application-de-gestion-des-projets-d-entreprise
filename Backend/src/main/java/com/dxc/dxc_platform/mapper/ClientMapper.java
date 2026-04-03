package com.dxc.dxc_platform.mapper;

import com.dxc.dxc_platform.dto.ClientDto;
import com.dxc.dxc_platform.entity.Client;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ClientMapper {
    ClientDto.Response toResponse(Client client);
}
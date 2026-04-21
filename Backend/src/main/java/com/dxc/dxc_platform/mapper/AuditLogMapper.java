package com.dxc.dxc_platform.mapper;

import com.dxc.dxc_platform.dto.AuditLogDto;
import com.dxc.dxc_platform.entity.AuditLog;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface AuditLogMapper {
    AuditLogDto toDto(AuditLog log);
}
package com.dxc.dxc_platform.dto;

import java.time.LocalDateTime;

public record AuditLogDto(
        Long id,
        String action,
        String entityType,
        Long entityId,
        String details,
        String performedBy,
        LocalDateTime performedAt,
        String ipAddress
) {}
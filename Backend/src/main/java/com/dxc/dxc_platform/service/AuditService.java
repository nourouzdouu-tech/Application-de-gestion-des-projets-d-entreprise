package com.dxc.dxc_platform.service;

import com.dxc.dxc_platform.dto.AuditLogDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.time.LocalDateTime;

public interface AuditService {
    void log(String action, String entityType, Long entityId, String details,
             String performedBy, String ipAddress);

    Page<AuditLogDto> getLogs(String action, String performedBy,
                              LocalDateTime startDate, LocalDateTime endDate, Pageable pageable);
}
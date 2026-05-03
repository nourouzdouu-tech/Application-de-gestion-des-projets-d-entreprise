package com.dxc.dxc_platform.controller;

import com.dxc.dxc_platform.dto.AuditLogDto;
import com.dxc.dxc_platform.service.impl.AuditServiceImpl;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/manager/audit")
@PreAuthorize("hasRole('MANAGER')")
public class AuditManagerController {

    private final AuditServiceImpl auditService;  // Utilisez l'implémentation

    public AuditManagerController(AuditServiceImpl auditService) {
        this.auditService = auditService;
    }

    @GetMapping
    public ResponseEntity<Page<AuditLogDto>> getWorkflowAudit(
            @RequestParam(required = false) String action,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "500") int size,
            Authentication authentication) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("performedAt").descending());
        return ResponseEntity.ok(
                auditService.getWorkflowLogs(action, authentication.getName(), startDate, endDate, pageable)
        );
    }
}

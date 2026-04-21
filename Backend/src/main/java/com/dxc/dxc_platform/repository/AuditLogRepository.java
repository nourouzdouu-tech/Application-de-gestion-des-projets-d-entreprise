package com.dxc.dxc_platform.repository;

import com.dxc.dxc_platform.entity.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Set;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    @Query("SELECT l FROM AuditLog l ORDER BY l.performedAt DESC")
    Page<AuditLog> findAllOrderByPerformedAtDesc(Pageable pageable);

    long countByActionAndPerformedAtBetween(String action, LocalDateTime start, LocalDateTime end);

    long countByActionInAndPerformedAtBetween(Set<String> actions, LocalDateTime start, LocalDateTime end);
}
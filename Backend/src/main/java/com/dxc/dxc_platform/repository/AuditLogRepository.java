package com.dxc.dxc_platform.repository;

import com.dxc.dxc_platform.entity.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    @Query("SELECT l FROM AuditLog l ORDER BY l.performedAt DESC")
    Page<AuditLog> findAllOrderByPerformedAtDesc(Pageable pageable);

    @Query("SELECT COUNT(l) FROM AuditLog l WHERE l.action = :action AND l.performedAt BETWEEN :start AND :end")
    long countByActionAndPerformedAtBetween(@Param("action") String action,
                                            @Param("start") LocalDateTime start,
                                            @Param("end") LocalDateTime end);

    @Query("SELECT COUNT(l) FROM AuditLog l WHERE l.action IN :actions AND l.performedAt BETWEEN :start AND :end")
    long countByActionInAndPerformedAtBetween(@Param("actions") Set<String> actions,
                                              @Param("start") LocalDateTime start,
                                              @Param("end") LocalDateTime end);

    // ✅ Pour rechercher par action et performedBy (au lieu de status)
    @Query("SELECT l FROM AuditLog l WHERE l.action = :action AND l.performedBy = :performedBy")
    List<AuditLog> findByActionAndPerformedBy(@Param("action") String action, @Param("performedBy") String performedBy);

    // ✅ Pour les échecs de connexion (cherche dans les détails ou par action spécifique)
    @Query("SELECT l FROM AuditLog l WHERE l.action = 'LOGIN_ATTEMPT' AND l.details LIKE '%failed%' ORDER BY l.performedAt DESC")
    List<AuditLog> findFailedLoginAttempts();

    // ✅ Pour les activités récentes
    @Query("SELECT l FROM AuditLog l ORDER BY l.performedAt DESC")
    List<AuditLog> findTop20ByOrderByPerformedAtDesc();

    // ✅ Pour compter les connexions échouées par utilisateur
    @Query("SELECT l.performedBy, COUNT(l) FROM AuditLog l WHERE l.action = 'LOGIN_ATTEMPT' AND l.details LIKE '%failed%' GROUP BY l.performedBy")
    List<Object[]> countFailedLoginsByUser();



}
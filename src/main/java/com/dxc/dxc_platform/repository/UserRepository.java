package com.dxc.dxc_platform.repository;

import com.dxc.dxc_platform.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByIdAndDeletedFalse(Long id);

    Optional<User> findByEmailAndDeletedFalse(String email);

    boolean existsByEmailAndDeletedFalse(String email);

    // ─── Requêtes SQL directes ────────────────────────────────────────────────

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Transactional
    @Query("UPDATE User u SET u.failedAttempts = u.failedAttempts + 1 WHERE u.email = :email")
    void incrementFailedAttempts(@Param("email") String email);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Transactional
    @Query("UPDATE User u SET u.locked = true, u.enabled = false, u.failedAttempts = 3 WHERE u.email = :email")
    void lockUser(@Param("email") String email);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Transactional
    @Query("UPDATE User u SET u.failedAttempts = 0 WHERE u.email = :email")
    void resetFailedAttempts(@Param("email") String email);

    // ─── Recherche paginée ────────────────────────────────────────────────────
    @Query("""
        SELECT u FROM User u
        JOIN u.roles r
        WHERE u.deleted = false
          AND (:enabled IS NULL OR u.enabled = :enabled)
          AND (:role = '' OR LOWER(r.nom) = :role)
          AND (
              LOWER(u.email)  LIKE :q OR
              LOWER(u.prenom) LIKE :q OR
              LOWER(u.nom)    LIKE :q
          )
    """)
    Page<User> search(
            @Param("q")       String q,
            @Param("enabled") Boolean enabled,
            @Param("role")    String role,
            Pageable pageable
    );
}
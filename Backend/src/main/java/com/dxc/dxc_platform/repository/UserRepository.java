package com.dxc.dxc_platform.repository;

import com.dxc.dxc_platform.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByIdAndDeletedFalse(Long id);

    Optional<User> findByEmailAndDeletedFalse(String email);

    boolean existsByEmailAndDeletedFalse(String email);

    // ✅ CORRIGÉ : findByLockedTrue (pas findByIsLockedTrue)
    List<User> findByLockedTrue();

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE User u SET u.failedAttempts = u.failedAttempts + 1 WHERE u.email = :email")
    void incrementFailedAttempts(@Param("email") String email);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE User u SET u.locked = true, u.failedAttempts = 3 WHERE u.email = :email")
    void lockUser(@Param("email") String email);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE User u SET u.failedAttempts = 0 WHERE u.email = :email")
    void resetFailedAttempts(@Param("email") String email);

    @Query("""
        SELECT DISTINCT u FROM User u
        LEFT JOIN u.roles r
        WHERE u.deleted = false
          AND (:locked IS NULL OR u.locked = :locked)
          AND (:role = '' OR LOWER(r.nom) = :role)
          AND (
              LOWER(u.email) LIKE :q OR
              LOWER(u.prenom) LIKE :q OR
              LOWER(u.nom) LIKE :q
          )
    """)
    Page<User> search(
            @Param("q") String q,
            @Param("locked") Boolean locked,
            @Param("role") String role,
            Pageable pageable
    );

    @Query("SELECT u FROM User u " +
            "WHERE u.deleted = false " +
            "AND u.locked = false " +
            "AND u.id != :excludeUserId " +
            "AND (LOWER(CONCAT(u.prenom, ' ', u.nom)) LIKE LOWER(CONCAT('%', :query, '%')) " +
            "OR LOWER(u.email) LIKE LOWER(CONCAT('%', :query, '%')) " +
            "OR EXISTS (SELECT r FROM u.roles r WHERE LOWER(r.nom) LIKE LOWER(CONCAT('%', :query, '%'))))")
    List<User> searchAvailableUsers(@Param("query") String query, @Param("excludeUserId") Long excludeUserId);

    @Query("""
        SELECT DISTINCT u
        FROM User u
        JOIN u.roles r
        WHERE u.deleted = false
          AND u.locked = false
          AND LOWER(r.nom) = 'manager'
        ORDER BY u.prenom ASC, u.nom ASC
    """)
    List<User> findAllActiveManagers();

    @Query("""
        SELECT DISTINCT u
        FROM User u
        JOIN u.roles r
        WHERE u.deleted = false
          AND u.locked = false
          AND LOWER(r.nom) = 'chef_projet'
        ORDER BY u.prenom ASC, u.nom ASC
    """)
    List<User> findAllActiveChefsProjet();

    @Query("SELECT COUNT(u) FROM User u WHERE u.deleted = false")
    long countByDeletedFalse();

    @Query("SELECT r.nom, COUNT(u) FROM User u JOIN u.roles r WHERE u.deleted = false GROUP BY r.nom")
    List<Object[]> countUsersByRole();

    @Query("SELECT p.libelle, COUNT(u) FROM User u LEFT JOIN u.profile p WHERE u.deleted = false GROUP BY p.libelle")
    List<Object[]> countUsersByProfile();

    List<User> findTop5ByDeletedFalseOrderByCreatedAtDesc();

    Optional<User> findByEmail(String email);

    @Query("SELECT COUNT(u) FROM User u WHERE u.locked = false AND u.deleted = false")
    long countActiveUsers();

    @Query("SELECT COUNT(u) FROM User u WHERE u.locked = false AND u.deleted = false AND u.createdAt >= :date")
    long countActiveUsersCreatedAfter(@Param("date") LocalDateTime date);

    @Query("SELECT COUNT(u) FROM User u WHERE u.locked = false AND u.deleted = false AND u.createdAt BETWEEN :start AND :end")
    long countActiveUsersCreatedBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("SELECT u FROM User u WHERE u.deleted = false AND EXISTS (SELECT r FROM u.roles r WHERE r.nom = 'ADMIN')")
    List<User> findAllAdmins();
}
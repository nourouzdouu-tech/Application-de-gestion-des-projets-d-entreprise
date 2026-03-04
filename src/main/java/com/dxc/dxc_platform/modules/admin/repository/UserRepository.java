package com.dxc.dxc_platform.modules.admin.repository;

import com.dxc.dxc_platform.modules.admin.domain.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByIdAndDeletedFalse(Long id);

    // Requis par Spring Security (CustomUserDetailsService)
    Optional<User> findByEmailAndDeletedFalse(String email);

    boolean existsByEmailAndDeletedFalse(String email);

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

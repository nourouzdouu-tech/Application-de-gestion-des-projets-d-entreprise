package com.dxc.dxc_platform.repository;

import com.dxc.dxc_platform.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface RoleRepository extends JpaRepository<Role, Long> {
    boolean existsByNom(String nom);
    Optional<Role> findByNom(String nom);
    long countByActiveTrue();

    @Query("SELECT COUNT(u) FROM User u JOIN u.roles r WHERE r.id = :roleId AND u.deleted = false")
    Long countUsersForRole(@Param("roleId") Long roleId);
}

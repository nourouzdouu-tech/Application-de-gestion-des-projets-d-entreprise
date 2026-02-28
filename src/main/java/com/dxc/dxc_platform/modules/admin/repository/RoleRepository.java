
package com.dxc.dxc_platform.modules.admin.repository;

import com.dxc.dxc_platform.modules.admin.domain.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RoleRepository extends JpaRepository<Role, Long> {
    Optional<Role> findByNom(String nom);

    boolean existsByNom(String nom);
}
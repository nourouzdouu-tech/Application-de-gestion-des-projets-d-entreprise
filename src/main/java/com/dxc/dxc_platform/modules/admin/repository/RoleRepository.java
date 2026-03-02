package com.dxc.dxc_platform.modules.admin.repository;

import com.dxc.dxc_platform.modules.admin.domain.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RoleRepository extends JpaRepository<Role, Long> {

    boolean existsByNomAndDeletedFalse(String nom);

    Optional<Role> findByIdAndDeletedFalse(Long id);

    Optional<Role> findByNomAndDeletedFalse(String nom);

    List<Role> findAllByDeletedFalse();
}
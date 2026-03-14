package com.dxc.dxc_platform.repository;

import com.dxc.dxc_platform.entity.Permission;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PermissionRepository extends JpaRepository<Permission, Long> {

    boolean existsByNom(String nom);

    Optional<Permission> findByNom(String nom);
}
package com.dxc.dxc_platform.repository;

import com.dxc.dxc_platform.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RoleRepository extends JpaRepository<Role, Long> {
    boolean existsByNom(String nom);
    Optional<Role> findByNom(String nom);
}
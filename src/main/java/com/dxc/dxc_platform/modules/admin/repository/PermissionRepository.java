package com.dxc.dxc_platform.modules.admin.repository;

import com.dxc.dxc_platform.modules.admin.domain.entity.Permission;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PermissionRepository extends JpaRepository<Permission, Long> {
}
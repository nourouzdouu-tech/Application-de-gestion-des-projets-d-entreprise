package com.dxc.dxc_platform.modules.admin.repository;

import com.dxc.dxc_platform.modules.admin.domain.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);
}
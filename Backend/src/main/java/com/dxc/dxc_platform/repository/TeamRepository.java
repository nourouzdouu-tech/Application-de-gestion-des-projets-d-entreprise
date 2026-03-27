package com.dxc.dxc_platform.repository;

import com.dxc.dxc_platform.entity.Team;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TeamRepository extends JpaRepository<Team, Long> {

    boolean existsByNameIgnoreCase(String name);
}
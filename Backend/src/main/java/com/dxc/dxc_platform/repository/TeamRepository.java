package com.dxc.dxc_platform.repository;

import com.dxc.dxc_platform.entity.Team;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TeamRepository extends JpaRepository<Team, Long> {

    boolean existsByNameIgnoreCaseAndDeletedFalse(String name);

    Optional<Team> findByIdAndDeletedFalse(Long id);

    Optional<Team> findByNameIgnoreCaseAndDeletedFalse(String name);

    List<Team> findAllByDeletedFalse();
}
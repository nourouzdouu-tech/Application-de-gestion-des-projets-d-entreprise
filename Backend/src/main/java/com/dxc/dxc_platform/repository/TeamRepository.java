package com.dxc.dxc_platform.repository;

import com.dxc.dxc_platform.entity.Team;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface TeamRepository extends JpaRepository<Team, Long> {

    boolean existsByNameIgnoreCaseAndDeletedFalse(String name);

    @Query("SELECT COUNT(t) > 0 FROM Team t WHERE t.name = :name AND t.deleted = false AND t.projectManager.id = :managerId")
    boolean existsByNameForManager(@Param("name") String name, @Param("managerId") Long managerId);

    Optional<Team> findByIdAndDeletedFalse(Long id);

    Optional<Team> findByNameIgnoreCaseAndDeletedFalse(String name);

    List<Team> findAllByDeletedFalse();

    List<Team> findByProjectManagerIdAndDeletedFalse(Long projectManagerId);

    @Query("SELECT COUNT(t) > 0 FROM Team t JOIN t.members m WHERE m.id = :userId AND t.deleted = false")
    boolean isUserInAnyTeam(@Param("userId") Long userId);
}
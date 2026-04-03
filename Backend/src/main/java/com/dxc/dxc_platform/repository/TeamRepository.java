package com.dxc.dxc_platform.repository;

import com.dxc.dxc_platform.entity.Team;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface TeamRepository extends JpaRepository<Team, Long> {

    // Vérifier si un nom existe déjà (sans tenir compte du manager)
    boolean existsByNameIgnoreCaseAndDeletedFalse(String name);

    // Vérifier si un nom existe déjà pour un manager spécifique
    @Query("SELECT COUNT(t) > 0 FROM Team t WHERE t.name = :name AND t.deleted = false AND t.projectManager.id = :managerId")
    boolean existsByNameForManager(@Param("name") String name, @Param("managerId") Long managerId);

    Optional<Team> findByIdAndDeletedFalse(Long id);

    Optional<Team> findByNameIgnoreCaseAndDeletedFalse(String name);

    List<Team> findAllByDeletedFalse();

    // Récupérer toutes les équipes d'un chef de projet (remplace findByProjectManagerIdAndDeletedFalse)
    List<Team> findByProjectManagerIdAndDeletedFalse(Long projectManagerId);

    // Vérifier si un utilisateur est déjà dans une équipe
    @Query("SELECT COUNT(t) > 0 FROM Team t JOIN t.members m WHERE m.id = :userId AND t.deleted = false")
    boolean isUserInAnyTeam(@Param("userId") Long userId);
}
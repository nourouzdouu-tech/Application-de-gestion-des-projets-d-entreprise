package com.dxc.dxc_platform.repository;

import com.dxc.dxc_platform.entity.Project;
import com.dxc.dxc_platform.enums.ProjectStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ProjectRepository extends JpaRepository<Project, Long> {

    boolean existsByNameIgnoreCaseAndDeletedFalse(String name);

    Optional<Project> findByIdAndDeletedFalse(Long id);

    List<Project> findAllByDeletedFalse();

    List<Project> findAllByDeletedFalseAndStatus(ProjectStatus status);

    List<Project> findAllByDeletedFalseAndNameContainingIgnoreCaseOrDeletedFalseAndClientContainingIgnoreCase(
            String name,
            String client
    );

    List<Project> findAllByTeamProjectManagerIdAndDeletedFalse(Long projectManagerId);

    List<Project> findAllByTeamProjectManagerIdAndDeletedFalseAndStatus(Long projectManagerId, ProjectStatus status);

    List<Project> findAllByDeletedFalseAndStatusAndManagerId(ProjectStatus status, Long managerId);

    @Query("SELECT p FROM Project p WHERE p.chefProjet.id = :chefProjetId AND p.deleted = false")
    List<Project> findAllByChefProjetIdAndDeletedFalse(@Param("chefProjetId") Long chefProjetId);

    @Query("SELECT p FROM Project p WHERE p.chefProjet.id = :chefProjetId AND p.deleted = false AND p.status = :status")
    List<Project> findAllByChefProjetIdAndDeletedFalseAndStatus(@Param("chefProjetId") Long chefProjetId, @Param("status") ProjectStatus status);

    boolean existsByTeamIdAndDeletedFalseAndIdNot(Long teamId, Long projectId);

    List<Project> findAllByDeletedFalseAndManagerId(Long managerId);

    List<Project> findAllByDeletedFalseAndManagerIdAndStatusIn(Long managerId, List<ProjectStatus> statuses);

    List<Project> findAllByTeamIdAndDeletedFalse(Long teamId);

    List<Project> findAllByTeamIdAndDeletedFalseAndStatus(Long teamId, ProjectStatus status);

    long countByStatus(String status);
    long countByCreatedAtAfter(LocalDateTime date);
    long countByDeletedFalseAndCreatedAtAfter(LocalDateTime date);
    List<Project> findTop5ByDeletedFalseOrderByCreatedAtDesc();
    List<Project> findByManagerIdAndStatus(Long managerId, String status);
    List<Project> findByManagerIdAndStatusIn(Long managerId, List<String> statuses);
    int countByManagerId(Long managerId);
    int countByManagerIdAndStatus(Long managerId, String status);
    List<Project> findByManagerId(Long managerId);

    @Query("SELECT p FROM Project p WHERE p.chefProjet.id = :chefProjetId")
    List<Project> findByChefProjetId(@Param("chefProjetId") Long chefProjetId);

    List<Project> findByEndDateBefore(LocalDateTime date);

    // List<Project> findByBilledTrue();

    List<Project> findByStatus(ProjectStatus status);
    List<Project> findByStatusAndManagerId(ProjectStatus status, Long managerId);

    @Query("SELECT p FROM Project p WHERE p.chefProjet.id = :chefProjetId")
    List<Project> findByChefDeProjetId(@Param("chefProjetId") Long chefProjetId);

    @Query("SELECT COUNT(p) FROM Project p WHERE p.deleted = false AND MONTH(p.createdAt) = :month")
    long countByMonth(@Param("month") int month);

    @Query("SELECT COUNT(DISTINCT p.client) FROM Project p WHERE p.deleted = false AND p.client IS NOT NULL AND p.client != ''")
    long countDistinctClient();
}
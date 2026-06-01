package com.dxc.dxc_platform.repository;

import com.dxc.dxc_platform.entity.Task;
import com.dxc.dxc_platform.enums.Status;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface TaskRepository extends JpaRepository<Task, Long> {

    List<Task> findAllByProjectIdAndDeletedFalse(Long projectId);

    List<Task> findAllByProjectIdInAndDeletedFalse(List<Long> projectIds);

    List<Task> findAllByAssignedToIdAndDeletedFalse(Long userId);

    List<Task> findAllByAssignedToIdAndProjectTeamIdAndDeletedFalse(Long userId, Long teamId);

    List<Task> findAllByAssignedToIdAndDeletedFalseAndStatusIn(Long memberId, List<Status> statuses);

    List<Task> findAllByAssignedToIdAndDeletedFalseAndCreatedAtBetween(Long memberId, LocalDateTime start, LocalDateTime end);

    List<Task> findByProjectId(Long projectId);

    List<Task> findByAssignedToId(Long memberId);


    // Pour récupérer les tâches d'un projet (utilisé par PredictionService)
    List<Task> findByProjectIdAndDeletedFalse(Long projectId);

    // Pour récupérer les tâches d'un membre sur un projet (utilisé par WorkloadAnalysisService)
    @Query("SELECT t FROM Task t WHERE t.assignedTo.id = :userId " +
            "AND t.project.id = :projectId AND t.deleted = false")
    List<Task> findByAssignedToIdAndProjectIdAndDeletedFalse(
            @Param("userId") Long userId,
            @Param("projectId") Long projectId
    );
    @Query("SELECT t FROM Task t WHERE t.project.chefProjet.id = :chefProjetId")
    List<Task> findByProjectChefDeProjetId(@Param("chefProjetId") Long chefProjetId);

    @Query("SELECT t FROM Task t WHERE t.project.chefProjet.id = :chefProjetId AND t.estimatedEndDate < CURRENT_DATE AND t.status != 'Terminé'")
    List<Task> findCriticalTasksByProjectManager(@Param("chefProjetId") Long chefProjetId);

    // ========== COUNT METHODS ==========
    long countByProjectIdAndDeletedFalse(Long projectId);

    long countByAssignedToId(Long userId);

    long countByAssignedToIdAndStatus(Long userId, String status);

    long countByAssignedToIdAndStatus(Long userId, Status status);

    @Query("SELECT COUNT(t) FROM Task t WHERE t.project.chefProjet.id = :chefProjetId AND t.status = :status")
    long countByProjectChefDeProjetIdAndStatus(@Param("chefProjetId") Long chefProjetId, @Param("status") String status);

    // ========== USER TASKS ==========
    @Query("SELECT t FROM Task t WHERE t.assignedTo.id = :userId AND t.estimatedEndDate < CURRENT_DATE AND t.status != 'Terminé'")
    List<Task> findLateTasksByUserId(@Param("userId") Long userId);

    @Query("SELECT COUNT(t) FROM Task t WHERE t.assignedTo.id = :userId AND t.status = 'Terminé'")
    long countCompletedTasksByUser(@Param("userId") Long userId);

    @Query("SELECT COUNT(t) FROM Task t WHERE t.assignedTo.id = :userId")
    long countTotalTasksByUser(@Param("userId") Long userId);

    @Query("SELECT COUNT(t) FROM Task t WHERE t.assignedTo.id = :userId AND t.status = :status")
    long countValidationsByUserAndStatus(@Param("userId") Long userId, @Param("status") String status);

    // ========== VALIDATION RATE ==========
    @Query("SELECT COUNT(t) FROM Task t WHERE t.project.chefProjet.id = :chefProjetId AND t.status = 'Terminé'")
    long countValidatedByChefProjet(@Param("chefProjetId") Long chefProjetId);

    @Query("SELECT COUNT(t) FROM Task t WHERE t.project.chefProjet.id = :chefProjetId")
    long countTotalByChefProjet(@Param("chefProjetId") Long chefProjetId);
}
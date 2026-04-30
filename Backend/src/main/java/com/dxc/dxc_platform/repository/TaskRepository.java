package com.dxc.dxc_platform.repository;

import com.dxc.dxc_platform.entity.Task;
import com.dxc.dxc_platform.enums.Status;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface TaskRepository extends JpaRepository<Task, Long> {
    List<Task> findAllByProjectIdAndDeletedFalse(Long projectId);
    List<Task> findAllByAssignedToIdAndDeletedFalse(Long userId);
    List<Task> findAllByAssignedToIdAndProjectTeamIdAndDeletedFalse(Long userId, Long teamId);
    long countByProjectIdAndDeletedFalse(Long projectId);
    List<Task> findByAssignedToId(Long memberId);
    List<Task> findByProjectId(Long projectId);
    List<Task> findAllByAssignedToIdAndDeletedFalseAndStatusIn(Long memberId, List<Status> statuses);
    List<Task> findAllByAssignedToIdAndDeletedFalseAndCreatedAtBetween(Long memberId, LocalDateTime start, LocalDateTime end);
}

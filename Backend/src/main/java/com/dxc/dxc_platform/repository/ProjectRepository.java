package com.dxc.dxc_platform.repository;

import com.dxc.dxc_platform.entity.Project;
import com.dxc.dxc_platform.enums.ProjectStatus;
import org.springframework.data.jpa.repository.JpaRepository;

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
    List<Project> findAllByChefProjetIdAndDeletedFalse(Long chefProjetId);
    List<Project> findAllByChefProjetIdAndDeletedFalseAndStatus(Long chefProjetId, ProjectStatus status);
    boolean existsByTeamIdAndDeletedFalseAndIdNot(Long teamId, Long projectId);
    List<Project> findAllByDeletedFalseAndManagerId(Long managerId);

    List<Project> findAllByDeletedFalseAndManagerIdAndStatusIn(Long managerId, List<ProjectStatus> statuses);
    List<Project> findAllByTeamIdAndDeletedFalse(Long teamId);

    List<Project> findAllByTeamIdAndDeletedFalseAndStatus(Long teamId, ProjectStatus status);
}

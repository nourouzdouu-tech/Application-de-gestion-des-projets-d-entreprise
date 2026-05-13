package com.dxc.dxc_platform.service;

import com.dxc.dxc_platform.dto.ProjectDto;
import com.dxc.dxc_platform.enums.ProjectStatus;
import com.dxc.dxc_platform.dto.ManagerProjectItemDto;
import com.dxc.dxc_platform.dto.ManagerProjectReviewDto;
import com.dxc.dxc_platform.dto.UserDto;
import java.util.List;

public interface ProjectService {

    ProjectDto createProject(ProjectDto request);

    ProjectDto updateProject(Long projectId, ProjectDto request);

    ProjectDto getProjectById(Long projectId);
    void updateProjectStatus(Long id, ProjectStatus status);

    List<ProjectDto> getAllProjects(String query, ProjectStatus status);

    List<ProjectDto> getMyProjects(String query, ProjectStatus status);

    ProjectDto setDeletedStatus(Long projectId, boolean deleted);

    List<ManagerProjectItemDto> getManagerProjects();

    List<UserDto.Summary> getChefsProjetForSelect();

    ProjectDto reviewProjectByManager(ManagerProjectReviewDto request);
    ProjectDto assignTeamToProject(Long projectId, Long teamId);
    List<ProjectDto> getMyAssignedProjects(String query, ProjectStatus status);
}
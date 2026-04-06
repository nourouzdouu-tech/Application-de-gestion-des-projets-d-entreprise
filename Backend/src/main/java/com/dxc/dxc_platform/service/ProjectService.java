package com.dxc.dxc_platform.service;

import com.dxc.dxc_platform.dto.ProjectDto;
import com.dxc.dxc_platform.enums.ProjectStatus;

import java.util.List;

public interface ProjectService {

    ProjectDto createProject(ProjectDto request);

    ProjectDto updateProject(Long projectId, ProjectDto request);

    ProjectDto getProjectById(Long projectId);

    List<ProjectDto> getAllProjects(String query, ProjectStatus status);

    List<ProjectDto> getMyProjects(String query, ProjectStatus status);

    ProjectDto setDeletedStatus(Long projectId, boolean deleted);
}
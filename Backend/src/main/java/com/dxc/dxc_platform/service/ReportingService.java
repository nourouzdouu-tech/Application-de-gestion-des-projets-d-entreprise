package com.dxc.dxc_platform.service;

import com.dxc.dxc_platform.dto.*;
import java.util.List;

public interface ReportingService {

    List<ProjectReportDto> getProjectsReport(Integer year, String status, Long teamId);

    List<ProjectReportDto> getOverdueProjects();

    List<com.dxc.dxc_platform.dto.reporting.TaskReportDto> getOverdueTasks(Long projectId);

    List<com.dxc.dxc_platform.dto.reporting.UserReportDto> getUsersWithoutProfile();

    UserStatusReportDto getUsersByStatus(Boolean active);

    List<com.dxc.dxc_platform.dto.reporting.ProjectSelectDto> getProjectsForSelect();

    List<com.dxc.dxc_platform.dto.reporting.TeamSelectDto> getTeamsForSelect();
}
package com.dxc.dxc_platform.dto;

import java.util.List;

public class UserStatusReportDto {
    private List<com.dxc.dxc_platform.dto.reporting.UserReportDto> users;
    private long totalActive;
    private long totalInactive;
    private long totalLocked;
    // Utilisateur qui réinitialise le plus souvent son mot de passe
    private com.dxc.dxc_platform.dto.reporting.UserReportDto topPasswordResetter;
    private int maxPasswordResetCount;

    public UserStatusReportDto() {}

    public UserStatusReportDto(List<com.dxc.dxc_platform.dto.reporting.UserReportDto> users, long totalActive, long totalInactive,
                               long totalLocked, com.dxc.dxc_platform.dto.reporting.UserReportDto topPasswordResetter,
                               int maxPasswordResetCount) {
        this.users = users;
        this.totalActive = totalActive;
        this.totalInactive = totalInactive;
        this.totalLocked = totalLocked;
        this.topPasswordResetter = topPasswordResetter;
        this.maxPasswordResetCount = maxPasswordResetCount;
    }

    // Getters & Setters
    public List<com.dxc.dxc_platform.dto.reporting.UserReportDto> getUsers() { return users; }
    public void setUsers(List<com.dxc.dxc_platform.dto.reporting.UserReportDto> users) { this.users = users; }

    public long getTotalActive() { return totalActive; }
    public void setTotalActive(long totalActive) { this.totalActive = totalActive; }

    public long getTotalInactive() { return totalInactive; }
    public void setTotalInactive(long totalInactive) { this.totalInactive = totalInactive; }

    public long getTotalLocked() { return totalLocked; }
    public void setTotalLocked(long totalLocked) { this.totalLocked = totalLocked; }

    public com.dxc.dxc_platform.dto.reporting.UserReportDto getTopPasswordResetter() { return topPasswordResetter; }
    public void setTopPasswordResetter(com.dxc.dxc_platform.dto.reporting.UserReportDto topPasswordResetter) { this.topPasswordResetter = topPasswordResetter; }

    public int getMaxPasswordResetCount() { return maxPasswordResetCount; }
    public void setMaxPasswordResetCount(int maxPasswordResetCount) { this.maxPasswordResetCount = maxPasswordResetCount; }
}
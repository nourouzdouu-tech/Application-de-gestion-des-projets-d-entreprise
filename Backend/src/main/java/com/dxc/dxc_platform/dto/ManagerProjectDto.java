package com.dxc.dxc_platform.dto;

import java.time.LocalDate;
import java.util.List;

public class ManagerProjectDto {
    private Long id;
    private String name;
    private String client;
    private String status;
    private String description;
    private LocalDate startDate;
    private LocalDate endDate;
    private String teamName;
    private String chefProjetName;
    private String managerName;
    private boolean factured;
    private List<ManagerProjectMemberDto> members;

    public ManagerProjectDto() {}

    public ManagerProjectDto(Long id, String name, String client, String status,
                             String description, LocalDate startDate, LocalDate endDate,
                             String teamName, String chefProjetName, String managerName,
                             boolean factured, List<ManagerProjectMemberDto> members) {
        this.id = id;
        this.name = name;
        this.client = client;
        this.status = status;
        this.description = description;
        this.startDate = startDate;
        this.endDate = endDate;
        this.teamName = teamName;
        this.chefProjetName = chefProjetName;
        this.managerName = managerName;
        this.factured = factured;
        this.members = members;
    }

    // Getters
    public Long getId() { return id; }
    public String getName() { return name; }
    public String getClient() { return client; }
    public String getStatus() { return status; }
    public String getDescription() { return description; }
    public LocalDate getStartDate() { return startDate; }
    public LocalDate getEndDate() { return endDate; }
    public String getTeamName() { return teamName; }
    public String getChefProjetName() { return chefProjetName; }
    public String getManagerName() { return managerName; }
    public boolean isFactured() { return factured; }
    public List<ManagerProjectMemberDto> getMembers() { return members; }

    // Setters
    public void setId(Long id) { this.id = id; }
    public void setName(String name) { this.name = name; }
    public void setClient(String client) { this.client = client; }
    public void setStatus(String status) { this.status = status; }
    public void setDescription(String description) { this.description = description; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }
    public void setTeamName(String teamName) { this.teamName = teamName; }
    public void setChefProjetName(String chefProjetName) { this.chefProjetName = chefProjetName; }
    public void setManagerName(String managerName) { this.managerName = managerName; }
    public void setFactured(boolean factured) { this.factured = factured; }
    public void setMembers(List<ManagerProjectMemberDto> members) { this.members = members; }
}
package com.dxc.dxc_platform.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public class ReportingDataDto {

    private UserInfoDto currentUser;
    private List<String> roles;

    private UserStatsDto userStats;
    private long activeRolesCount;
    private List<RoleDistributionDto> roleDistribution;
    private List<ProfileDistributionDto> profileDistribution;
    private List<ClientActivityDto> topClients;
    private List<EvolutionDataDto> userEvolution;
    private ProjectStatsDto projectStats;
    private List<RecentActivityDto> recentActivities;

    private List<ProjectReviewDto> pendingProjects;
    private List<ProjectReviewDto> processedProjects;
    private ValidationStatsDto validationStats;
    private List<ProjectSummaryDto> managerProjects;
    private ChartDataDto projectDistribution;

    private List<ProjectSummaryDto> myProjects;
    private List<TeamPerformanceDto> teamPerformance;
    private TaskStatsDto taskStats;
    private List<TaskSummaryDto> recentTasks;

    private TaskStatsDto personalTaskStats;
    private List<TaskSummaryDto> currentTasks;
    private List<WeeklyEvolutionDto> weeklyEvolution;
    private PriorityDistributionDto priorityDistribution;
    private double personalCompletionRate;

    private BillingStatsDto billingStats;
    private List<ClientRevenueDto> topClientsByRevenue;
    private List<MonthlyProjectDto> projectsByMonth;
    private long activeClientsCount;
    private double contractValidationRate;

    private List<ProjectSummaryDto> allProjects;
    private List<TaskSummaryDto> allTasks;

    private int currentPage;
    private int itemsPerPage;
    private int totalPages;
    private long totalItems;

    public UserInfoDto getCurrentUser() { return currentUser; }
    public void setCurrentUser(UserInfoDto currentUser) { this.currentUser = currentUser; }

    public List<String> getRoles() { return roles; }
    public void setRoles(List<String> roles) { this.roles = roles; }

    public UserStatsDto getUserStats() { return userStats; }
    public void setUserStats(UserStatsDto userStats) { this.userStats = userStats; }

    public long getActiveRolesCount() { return activeRolesCount; }
    public void setActiveRolesCount(long activeRolesCount) { this.activeRolesCount = activeRolesCount; }

    public List<RoleDistributionDto> getRoleDistribution() { return roleDistribution; }
    public void setRoleDistribution(List<RoleDistributionDto> roleDistribution) { this.roleDistribution = roleDistribution; }

    public List<ProfileDistributionDto> getProfileDistribution() { return profileDistribution; }
    public void setProfileDistribution(List<ProfileDistributionDto> profileDistribution) { this.profileDistribution = profileDistribution; }

    public List<ClientActivityDto> getTopClients() { return topClients; }
    public void setTopClients(List<ClientActivityDto> topClients) { this.topClients = topClients; }

    public List<EvolutionDataDto> getUserEvolution() { return userEvolution; }
    public void setUserEvolution(List<EvolutionDataDto> userEvolution) { this.userEvolution = userEvolution; }

    public ProjectStatsDto getProjectStats() { return projectStats; }
    public void setProjectStats(ProjectStatsDto projectStats) { this.projectStats = projectStats; }

    public List<RecentActivityDto> getRecentActivities() { return recentActivities; }
    public void setRecentActivities(List<RecentActivityDto> recentActivities) { this.recentActivities = recentActivities; }

    public List<ProjectReviewDto> getPendingProjects() { return pendingProjects; }
    public void setPendingProjects(List<ProjectReviewDto> pendingProjects) { this.pendingProjects = pendingProjects; }

    public List<ProjectReviewDto> getProcessedProjects() { return processedProjects; }
    public void setProcessedProjects(List<ProjectReviewDto> processedProjects) { this.processedProjects = processedProjects; }

    public ValidationStatsDto getValidationStats() { return validationStats; }
    public void setValidationStats(ValidationStatsDto validationStats) { this.validationStats = validationStats; }

    public List<ProjectSummaryDto> getManagerProjects() { return managerProjects; }
    public void setManagerProjects(List<ProjectSummaryDto> managerProjects) { this.managerProjects = managerProjects; }

    public ChartDataDto getProjectDistribution() { return projectDistribution; }
    public void setProjectDistribution(ChartDataDto projectDistribution) { this.projectDistribution = projectDistribution; }

    public List<ProjectSummaryDto> getMyProjects() { return myProjects; }
    public void setMyProjects(List<ProjectSummaryDto> myProjects) { this.myProjects = myProjects; }

    public List<TeamPerformanceDto> getTeamPerformance() { return teamPerformance; }
    public void setTeamPerformance(List<TeamPerformanceDto> teamPerformance) { this.teamPerformance = teamPerformance; }

    public TaskStatsDto getTaskStats() { return taskStats; }
    public void setTaskStats(TaskStatsDto taskStats) { this.taskStats = taskStats; }

    public List<TaskSummaryDto> getRecentTasks() { return recentTasks; }
    public void setRecentTasks(List<TaskSummaryDto> recentTasks) { this.recentTasks = recentTasks; }

    public TaskStatsDto getPersonalTaskStats() { return personalTaskStats; }
    public void setPersonalTaskStats(TaskStatsDto personalTaskStats) { this.personalTaskStats = personalTaskStats; }

    public List<TaskSummaryDto> getCurrentTasks() { return currentTasks; }
    public void setCurrentTasks(List<TaskSummaryDto> currentTasks) { this.currentTasks = currentTasks; }

    public List<WeeklyEvolutionDto> getWeeklyEvolution() { return weeklyEvolution; }
    public void setWeeklyEvolution(List<WeeklyEvolutionDto> weeklyEvolution) { this.weeklyEvolution = weeklyEvolution; }

    public PriorityDistributionDto getPriorityDistribution() { return priorityDistribution; }
    public void setPriorityDistribution(PriorityDistributionDto priorityDistribution) { this.priorityDistribution = priorityDistribution; }

    public double getPersonalCompletionRate() { return personalCompletionRate; }
    public void setPersonalCompletionRate(double personalCompletionRate) { this.personalCompletionRate = personalCompletionRate; }

    public BillingStatsDto getBillingStats() { return billingStats; }
    public void setBillingStats(BillingStatsDto billingStats) { this.billingStats = billingStats; }

    public List<ClientRevenueDto> getTopClientsByRevenue() { return topClientsByRevenue; }
    public void setTopClientsByRevenue(List<ClientRevenueDto> topClientsByRevenue) { this.topClientsByRevenue = topClientsByRevenue; }

    public List<MonthlyProjectDto> getProjectsByMonth() { return projectsByMonth; }
    public void setProjectsByMonth(List<MonthlyProjectDto> projectsByMonth) { this.projectsByMonth = projectsByMonth; }

    public long getActiveClientsCount() { return activeClientsCount; }
    public void setActiveClientsCount(long activeClientsCount) { this.activeClientsCount = activeClientsCount; }

    public double getContractValidationRate() { return contractValidationRate; }
    public void setContractValidationRate(double contractValidationRate) { this.contractValidationRate = contractValidationRate; }

    public List<ProjectSummaryDto> getAllProjects() { return allProjects; }
    public void setAllProjects(List<ProjectSummaryDto> allProjects) { this.allProjects = allProjects; }

    public List<TaskSummaryDto> getAllTasks() { return allTasks; }
    public void setAllTasks(List<TaskSummaryDto> allTasks) { this.allTasks = allTasks; }

    public int getCurrentPage() { return currentPage; }
    public void setCurrentPage(int currentPage) { this.currentPage = currentPage; }

    public int getItemsPerPage() { return itemsPerPage; }
    public void setItemsPerPage(int itemsPerPage) { this.itemsPerPage = itemsPerPage; }

    public int getTotalPages() { return totalPages; }
    public void setTotalPages(int totalPages) { this.totalPages = totalPages; }

    public long getTotalItems() { return totalItems; }
    public void setTotalItems(long totalItems) { this.totalItems = totalItems; }

    public static class UserInfoDto {
        private Long id;
        private String prenom;
        private String nom;
        private String email;
        private boolean enabled;
        private String profileLibelle;
        private List<String> roles;
        private LocalDateTime createdAt;

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getPrenom() { return prenom; }
        public void setPrenom(String prenom) { this.prenom = prenom; }
        public String getNom() { return nom; }
        public void setNom(String nom) { this.nom = nom; }
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public String getProfileLibelle() { return profileLibelle; }
        public void setProfileLibelle(String profileLibelle) { this.profileLibelle = profileLibelle; }
        public List<String> getRoles() { return roles; }
        public void setRoles(List<String> roles) { this.roles = roles; }
        public LocalDateTime getCreatedAt() { return createdAt; }
        public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    }

    public static class UserStatsDto {
        private long totalUsers;
        private long activeUsers;
        private long inactiveUsers;
        private int newUsersThisMonth;
        private double activePercentage;

        public long getTotalUsers() { return totalUsers; }
        public void setTotalUsers(long totalUsers) { this.totalUsers = totalUsers; }
        public long getActiveUsers() { return activeUsers; }
        public void setActiveUsers(long activeUsers) { this.activeUsers = activeUsers; }
        public long getInactiveUsers() { return inactiveUsers; }
        public void setInactiveUsers(long inactiveUsers) { this.inactiveUsers = inactiveUsers; }
        public int getNewUsersThisMonth() { return newUsersThisMonth; }
        public void setNewUsersThisMonth(int newUsersThisMonth) { this.newUsersThisMonth = newUsersThisMonth; }
        public double getActivePercentage() { return activePercentage; }
        public void setActivePercentage(double activePercentage) { this.activePercentage = activePercentage; }
    }

    public static class RoleDistributionDto {
        private String role;
        private long count;
        private double percentage;
        private String color;

        public String getRole() { return role; }
        public void setRole(String role) { this.role = role; }
        public long getCount() { return count; }
        public void setCount(long count) { this.count = count; }
        public double getPercentage() { return percentage; }
        public void setPercentage(double percentage) { this.percentage = percentage; }
        public String getColor() { return color; }
        public void setColor(String color) { this.color = color; }
    }

    public static class ProfileDistributionDto {
        private String profile;
        private long count;
        private double percentage;
        private String color;

        public String getProfile() { return profile; }
        public void setProfile(String profile) { this.profile = profile; }
        public long getCount() { return count; }
        public void setCount(long count) { this.count = count; }
        public double getPercentage() { return percentage; }
        public void setPercentage(double percentage) { this.percentage = percentage; }
        public String getColor() { return color; }
        public void setColor(String color) { this.color = color; }
    }

    public static class ClientActivityDto {
        private String client;
        private long projectsCount;
        private double averageProgress;
        private String latestActivity;
        private int representantsCount;
        private double sharePercentage;

        public String getClient() { return client; }
        public void setClient(String client) { this.client = client; }
        public long getProjectsCount() { return projectsCount; }
        public void setProjectsCount(long projectsCount) { this.projectsCount = projectsCount; }
        public double getAverageProgress() { return averageProgress; }
        public void setAverageProgress(double averageProgress) { this.averageProgress = averageProgress; }
        public String getLatestActivity() { return latestActivity; }
        public void setLatestActivity(String latestActivity) { this.latestActivity = latestActivity; }
        public int getRepresentantsCount() { return representantsCount; }
        public void setRepresentantsCount(int representantsCount) { this.representantsCount = representantsCount; }
        public double getSharePercentage() { return sharePercentage; }
        public void setSharePercentage(double sharePercentage) { this.sharePercentage = sharePercentage; }
    }

    public static class EvolutionDataDto {
        private String month;
        private long count;

        public String getMonth() { return month; }
        public void setMonth(String month) { this.month = month; }
        public long getCount() { return count; }
        public void setCount(long count) { this.count = count; }
    }

    public static class ProjectStatsDto {
        private long totalProjects;
        private long projectsEnCours;
        private long projectsEnValidation;
        private long projectsPreValides;
        private long projectsRejetes;
        private double completionRate;
        private int newProjectsThisMonth;

        public long getTotalProjects() { return totalProjects; }
        public void setTotalProjects(long totalProjects) { this.totalProjects = totalProjects; }
        public long getProjectsEnCours() { return projectsEnCours; }
        public void setProjectsEnCours(long projectsEnCours) { this.projectsEnCours = projectsEnCours; }
        public long getProjectsEnValidation() { return projectsEnValidation; }
        public void setProjectsEnValidation(long projectsEnValidation) { this.projectsEnValidation = projectsEnValidation; }
        public long getProjectsPreValides() { return projectsPreValides; }
        public void setProjectsPreValides(long projectsPreValides) { this.projectsPreValides = projectsPreValides; }
        public long getProjectsRejetes() { return projectsRejetes; }
        public void setProjectsRejetes(long projectsRejetes) { this.projectsRejetes = projectsRejetes; }
        public double getCompletionRate() { return completionRate; }
        public void setCompletionRate(double completionRate) { this.completionRate = completionRate; }
        public int getNewProjectsThisMonth() { return newProjectsThisMonth; }
        public void setNewProjectsThisMonth(int newProjectsThisMonth) { this.newProjectsThisMonth = newProjectsThisMonth; }
    }

    public static class RecentActivityDto {
        private Long id;
        private String type;
        private String text;
        private String time;
        private LocalDateTime timestamp;

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        public String getText() { return text; }
        public void setText(String text) { this.text = text; }
        public String getTime() { return time; }
        public void setTime(String time) { this.time = time; }
        public LocalDateTime getTimestamp() { return timestamp; }
        public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
    }

    public static class ProjectReviewDto {
        private Long id;
        private String name;
        private String client;
        private String status;
        private String createdAt;
        private String reviewedAt;
        private String managerComment;
        private String chefProjetName;
        private String managerName;
        private int progressPercentage;

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getClient() { return client; }
        public void setClient(String client) { this.client = client; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public String getCreatedAt() { return createdAt; }
        public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
        public String getReviewedAt() { return reviewedAt; }
        public void setReviewedAt(String reviewedAt) { this.reviewedAt = reviewedAt; }
        public String getManagerComment() { return managerComment; }
        public void setManagerComment(String managerComment) { this.managerComment = managerComment; }
        public String getChefProjetName() { return chefProjetName; }
        public void setChefProjetName(String chefProjetName) { this.chefProjetName = chefProjetName; }
        public String getManagerName() { return managerName; }
        public void setManagerName(String managerName) { this.managerName = managerName; }
        public int getProgressPercentage() { return progressPercentage; }
        public void setProgressPercentage(int progressPercentage) { this.progressPercentage = progressPercentage; }
    }

    public static class ValidationStatsDto {
        private int totalProjects;
        private int pendingCount;
        private int validatedCount;
        private int rejectedCount;
        private double validationRate;

        public int getTotalProjects() { return totalProjects; }
        public void setTotalProjects(int totalProjects) { this.totalProjects = totalProjects; }
        public int getPendingCount() { return pendingCount; }
        public void setPendingCount(int pendingCount) { this.pendingCount = pendingCount; }
        public int getValidatedCount() { return validatedCount; }
        public void setValidatedCount(int validatedCount) { this.validatedCount = validatedCount; }
        public int getRejectedCount() { return rejectedCount; }
        public void setRejectedCount(int rejectedCount) { this.rejectedCount = rejectedCount; }
        public double getValidationRate() { return validationRate; }
        public void setValidationRate(double validationRate) { this.validationRate = validationRate; }
    }

    public static class ProjectSummaryDto {
        private Long id;
        private String name;
        private String client;
        private String status;
        private int progressPercentage;
        private String createdAt;
        private String updatedAt;
        private String managerName;
        private String chefProjetName;
        private String teamName;
        private String description;
        private String riskLevel;
        private LocalDate startDate;
        private LocalDate endDate;

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getClient() { return client; }
        public void setClient(String client) { this.client = client; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public int getProgressPercentage() { return progressPercentage; }
        public void setProgressPercentage(int progressPercentage) { this.progressPercentage = progressPercentage; }
        public String getCreatedAt() { return createdAt; }
        public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
        public String getUpdatedAt() { return updatedAt; }
        public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }
        public String getManagerName() { return managerName; }
        public void setManagerName(String managerName) { this.managerName = managerName; }
        public String getChefProjetName() { return chefProjetName; }
        public void setChefProjetName(String chefProjetName) { this.chefProjetName = chefProjetName; }
        public String getTeamName() { return teamName; }
        public void setTeamName(String teamName) { this.teamName = teamName; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public String getRiskLevel() { return riskLevel; }
        public void setRiskLevel(String riskLevel) { this.riskLevel = riskLevel; }
        public LocalDate getStartDate() { return startDate; }
        public void setStartDate(LocalDate startDate) { this.startDate = startDate; }
        public LocalDate getEndDate() { return endDate; }
        public void setEndDate(LocalDate endDate) { this.endDate = endDate; }
    }

    public static class ChartDataDto {
        private List<String> labels;
        private List<Double> data;
        private List<String> colors;

        public List<String> getLabels() { return labels; }
        public void setLabels(List<String> labels) { this.labels = labels; }
        public List<Double> getData() { return data; }
        public void setData(List<Double> data) { this.data = data; }
        public List<String> getColors() { return colors; }
        public void setColors(List<String> colors) { this.colors = colors; }
    }

    public static class TeamPerformanceDto {
        private Long memberId;
        private String memberName;
        private String email;
        private long completedTasks;
        private double efficiency;
        private double averageDelay;
        private String status;

        public Long getMemberId() { return memberId; }
        public void setMemberId(Long memberId) { this.memberId = memberId; }
        public String getMemberName() { return memberName; }
        public void setMemberName(String memberName) { this.memberName = memberName; }
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public long getCompletedTasks() { return completedTasks; }
        public void setCompletedTasks(long completedTasks) { this.completedTasks = completedTasks; }
        public double getEfficiency() { return efficiency; }
        public void setEfficiency(double efficiency) { this.efficiency = efficiency; }
        public double getAverageDelay() { return averageDelay; }
        public void setAverageDelay(double averageDelay) { this.averageDelay = averageDelay; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
    }

    public static class TaskStatsDto {
        private long totalTasks;
        private long completedTasks;
        private long inProgressTasks;
        private long pendingTasks;
        private long lateTasks;
        private double completionRate;

        public long getTotalTasks() { return totalTasks; }
        public void setTotalTasks(long totalTasks) { this.totalTasks = totalTasks; }
        public long getCompletedTasks() { return completedTasks; }
        public void setCompletedTasks(long completedTasks) { this.completedTasks = completedTasks; }
        public long getInProgressTasks() { return inProgressTasks; }
        public void setInProgressTasks(long inProgressTasks) { this.inProgressTasks = inProgressTasks; }
        public long getPendingTasks() { return pendingTasks; }
        public void setPendingTasks(long pendingTasks) { this.pendingTasks = pendingTasks; }
        public long getLateTasks() { return lateTasks; }
        public void setLateTasks(long lateTasks) { this.lateTasks = lateTasks; }
        public double getCompletionRate() { return completionRate; }
        public void setCompletionRate(double completionRate) { this.completionRate = completionRate; }
    }

    public static class TaskSummaryDto {
        private Long id;
        private String title;
        private String projectName;
        private Long projectId;
        private String priority;
        private String status;
        private String statusLabel;
        private String estimatedEndDate;
        private String createdAt;
        private boolean late;
        private String priorityColor;
        private String statusColor;

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public String getProjectName() { return projectName; }
        public void setProjectName(String projectName) { this.projectName = projectName; }
        public Long getProjectId() { return projectId; }
        public void setProjectId(Long projectId) { this.projectId = projectId; }
        public String getPriority() { return priority; }
        public void setPriority(String priority) { this.priority = priority; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public String getStatusLabel() { return statusLabel; }
        public void setStatusLabel(String statusLabel) { this.statusLabel = statusLabel; }
        public String getEstimatedEndDate() { return estimatedEndDate; }
        public void setEstimatedEndDate(String estimatedEndDate) { this.estimatedEndDate = estimatedEndDate; }
        public String getCreatedAt() { return createdAt; }
        public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
        public boolean isLate() { return late; }
        public void setLate(boolean late) { this.late = late; }
        public String getPriorityColor() { return priorityColor; }
        public void setPriorityColor(String priorityColor) { this.priorityColor = priorityColor; }
        public String getStatusColor() { return statusColor; }
        public void setStatusColor(String statusColor) { this.statusColor = statusColor; }
    }

    public static class WeeklyEvolutionDto {
        private String week;
        private int total;
        private int completed;
        private double completionRate;

        public String getWeek() { return week; }
        public void setWeek(String week) { this.week = week; }
        public int getTotal() { return total; }
        public void setTotal(int total) { this.total = total; }
        public int getCompleted() { return completed; }
        public void setCompleted(int completed) { this.completed = completed; }
        public double getCompletionRate() { return completionRate; }
        public void setCompletionRate(double completionRate) { this.completionRate = completionRate; }
    }

    public static class PriorityDistributionDto {
        private long highPriorityCount;
        private long mediumPriorityCount;
        private long lowPriorityCount;
        private double highPriorityPercent;
        private double mediumPriorityPercent;
        private double lowPriorityPercent;

        public long getHighPriorityCount() { return highPriorityCount; }
        public void setHighPriorityCount(long highPriorityCount) { this.highPriorityCount = highPriorityCount; }
        public long getMediumPriorityCount() { return mediumPriorityCount; }
        public void setMediumPriorityCount(long mediumPriorityCount) { this.mediumPriorityCount = mediumPriorityCount; }
        public long getLowPriorityCount() { return lowPriorityCount; }
        public void setLowPriorityCount(long lowPriorityCount) { this.lowPriorityCount = lowPriorityCount; }
        public double getHighPriorityPercent() { return highPriorityPercent; }
        public void setHighPriorityPercent(double highPriorityPercent) { this.highPriorityPercent = highPriorityPercent; }
        public double getMediumPriorityPercent() { return mediumPriorityPercent; }
        public void setMediumPriorityPercent(double mediumPriorityPercent) { this.mediumPriorityPercent = mediumPriorityPercent; }
        public double getLowPriorityPercent() { return lowPriorityPercent; }
        public void setLowPriorityPercent(double lowPriorityPercent) { this.lowPriorityPercent = lowPriorityPercent; }
    }

    public static class BillingStatsDto {
        private BigDecimal totalHT;
        private BigDecimal totalTTC;
        private BigDecimal tvaRate;
        private List<ProfileBillingDto> byProfile;

        public BigDecimal getTotalHT() { return totalHT; }
        public void setTotalHT(BigDecimal totalHT) { this.totalHT = totalHT; }
        public BigDecimal getTotalTTC() { return totalTTC; }
        public void setTotalTTC(BigDecimal totalTTC) { this.totalTTC = totalTTC; }
        public BigDecimal getTvaRate() { return tvaRate; }
        public void setTvaRate(BigDecimal tvaRate) { this.tvaRate = tvaRate; }
        public List<ProfileBillingDto> getByProfile() { return byProfile; }
        public void setByProfile(List<ProfileBillingDto> byProfile) { this.byProfile = byProfile; }
    }

    public static class ProfileBillingDto {
        private String profile;
        private BigDecimal total;
        private long count;
        private double percentage;

        public String getProfile() { return profile; }
        public void setProfile(String profile) { this.profile = profile; }
        public BigDecimal getTotal() { return total; }
        public void setTotal(BigDecimal total) { this.total = total; }
        public long getCount() { return count; }
        public void setCount(long count) { this.count = count; }
        public double getPercentage() { return percentage; }
        public void setPercentage(double percentage) { this.percentage = percentage; }
    }

    public static class ClientRevenueDto {
        private String client;
        private long projectsCount;
        private BigDecimal totalHT;
        private double sharePercentage;

        public String getClient() { return client; }
        public void setClient(String client) { this.client = client; }
        public long getProjectsCount() { return projectsCount; }
        public void setProjectsCount(long projectsCount) { this.projectsCount = projectsCount; }
        public BigDecimal getTotalHT() { return totalHT; }
        public void setTotalHT(BigDecimal totalHT) { this.totalHT = totalHT; }
        public double getSharePercentage() { return sharePercentage; }
        public void setSharePercentage(double sharePercentage) { this.sharePercentage = sharePercentage; }
    }

    public static class MonthlyProjectDto {
        private String month;
        private long count;
        private double percentage;

        public String getMonth() { return month; }
        public void setMonth(String month) { this.month = month; }
        public long getCount() { return count; }
        public void setCount(long count) { this.count = count; }
        public double getPercentage() { return percentage; }
        public void setPercentage(double percentage) { this.percentage = percentage; }
    }
}
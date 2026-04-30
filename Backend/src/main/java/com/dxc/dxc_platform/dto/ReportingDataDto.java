package com.dxc.dxc_platform.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class ReportingDataDto {

    // ================= INFOS UTILISATEUR =================
    private UserInfoDto currentUser;
    private List<String> roles;

    // ================= ADMIN =================
    private UserStatsDto userStats;
    private long activeRolesCount;
    private List<RoleDistributionDto> roleDistribution;
    private List<ProfileDistributionDto> profileDistribution;
    private List<ClientActivityDto> topClients;
    private List<EvolutionDataDto> userEvolution;
    private ProjectStatsDto projectStats;
    private List<RecentActivityDto> recentActivities;

    // ================= MANAGER =================
    private List<ProjectReviewDto> pendingProjects;
    private List<ProjectReviewDto> processedProjects;
    private ValidationStatsDto validationStats;
    private List<ProjectSummaryDto> managerProjects;
    private ChartDataDto projectDistribution;

    // ================= CHEF PROJET =================
    private List<ProjectSummaryDto> myProjects;
    private List<TeamPerformanceDto> teamPerformance;
    private TaskStatsDto taskStats;
    private List<TaskSummaryDto> recentTasks;

    // ================= MEMBRE EQUIPE =================
    private TaskStatsDto personalTaskStats;
    private List<TaskSummaryDto> currentTasks;
    private List<WeeklyEvolutionDto> weeklyEvolution;
    private PriorityDistributionDto priorityDistribution;
    private double personalCompletionRate;

    // ================= RESPONSABLE CONTRAT =================
    private BillingStatsDto billingStats;
    private List<ClientRevenueDto> topClientsByRevenue;
    private List<MonthlyProjectDto> projectsByMonth;
    private long activeClientsCount;
    private double contractValidationRate;

    // ================= COMMUN =================
    private List<ProjectSummaryDto> allProjects;
    private List<TaskSummaryDto> allTasks;

    // ================= PAGINATION =================
    private int currentPage;
    private int itemsPerPage;
    private int totalPages;
    private long totalItems;

    // ============== CLASSES INTERNES ==============

    @Data
    public static class UserInfoDto {
        private Long id;
        private String prenom;
        private String nom;
        private String email;
        private boolean enabled;
        private String profileLibelle;
        private List<String> roles;
        private LocalDateTime createdAt;
    }

    @Data
    public static class UserStatsDto {
        private long totalUsers;
        private long activeUsers;
        private long inactiveUsers;
        private int newUsersThisMonth;
        private double activePercentage;
    }

    @Data
    public static class RoleDistributionDto {
        private String role;
        private long count;
        private double percentage;
        private String color;
    }

    @Data
    public static class ProfileDistributionDto {
        private String profile;
        private long count;
        private double percentage;
        private String color;
    }

    @Data
    public static class ClientActivityDto {
        private String client;
        private long projectsCount;
        private double averageProgress;
        private String latestActivity;
        private int representantsCount;
        private double sharePercentage;
    }

    @Data
    public static class EvolutionDataDto {
        private String month;
        private long count;
    }

    @Data
    public static class ProjectStatsDto {
        private long totalProjects;
        private long projectsEnCours;
        private long projectsEnValidation;
        private long projectsPreValides;
        private long projectsRejetes;
        private double completionRate;
        private int newProjectsThisMonth;
    }

    @Data
    public static class RecentActivityDto {
        private Long id;
        private String type;
        private String text;
        private String time;
        private LocalDateTime timestamp;
    }

    @Data
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
    }

    @Data
    public static class ValidationStatsDto {
        private int totalProjects;
        private int pendingCount;
        private int validatedCount;
        private int rejectedCount;
        private double validationRate;
    }

    @Data
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
    }

    @Data
    public static class ChartDataDto {
        private List<String> labels;
        private List<Double> data;
        private List<String> colors;
    }

    @Data
    public static class TeamPerformanceDto {
        private Long memberId;
        private String memberName;
        private String email;
        private long completedTasks;
        private double efficiency;
        private double averageDelay;
        private String status;
    }

    @Data
    public static class TaskStatsDto {
        private long totalTasks;
        private long completedTasks;
        private long inProgressTasks;
        private long pendingTasks;
        private long lateTasks;
        private double completionRate;
    }

    @Data
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
        private boolean isLate;
        private String priorityColor;
        private String statusColor;
    }

    @Data
    public static class WeeklyEvolutionDto {
        private String week;
        private int total;
        private int completed;
        private double completionRate;
    }

    @Data
    public static class PriorityDistributionDto {
        private long highPriorityCount;
        private long mediumPriorityCount;
        private long lowPriorityCount;
        private double highPriorityPercent;
        private double mediumPriorityPercent;
        private double lowPriorityPercent;
    }

    @Data
    public static class BillingStatsDto {
        private BigDecimal totalHT;
        private BigDecimal totalTTC;
        private BigDecimal tvaRate;
        private List<ProfileBillingDto> byProfile;
    }

    @Data
    public static class ProfileBillingDto {
        private String profile;
        private BigDecimal total;
        private long count;
        private double percentage;
    }

    @Data
    public static class ClientRevenueDto {
        private String client;
        private long projectsCount;
        private BigDecimal totalHT;
        private double sharePercentage;
    }

    @Data
    public static class MonthlyProjectDto {
        private String month;
        private long count;
        private double percentage;
    }
}

package com.dxc.dxc_platform.service;

import com.dxc.dxc_platform.dto.ReportingDataDto;
import com.dxc.dxc_platform.entity.*;
import com.dxc.dxc_platform.enums.ProjectStatus;
import com.dxc.dxc_platform.enums.Status;
import com.dxc.dxc_platform.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ReportingService {

    private final UserRepository userRepository;
    private final ProjectRepository projectRepository;
    private final TaskRepository taskRepository;
    private final ClientRepository clientRepository;
    private final RoleRepository roleRepository;
    private final ProfileRepository profileRepository;

    public ReportingService(
            UserRepository userRepository,
            ProjectRepository projectRepository,
            TaskRepository taskRepository,
            ClientRepository clientRepository,
            RoleRepository roleRepository,
            ProfileRepository profileRepository
    ) {
        this.userRepository = userRepository;
        this.projectRepository = projectRepository;
        this.taskRepository = taskRepository;
        this.clientRepository = clientRepository;
        this.roleRepository = roleRepository;
        this.profileRepository = profileRepository;
    }
    @Transactional(readOnly = true)
    public ReportingDataDto getCompleteReporting(User currentUser) {
        ReportingDataDto dto = new ReportingDataDto();

        dto.setCurrentUser(mapToUserInfo(currentUser));
        dto.setRoles(currentUser.getRoles().stream()
                .map(Role::getNom)
                .collect(Collectors.toList()));

        dto.setAllProjects(getAllProjectSummaries());
        dto.setAllTasks(getAllTaskSummaries());

        dto.setCurrentPage(1);
        dto.setItemsPerPage(10);

        List<String> roles = dto.getRoles();

        if (roles.contains("ADMIN")) {
            dto.setUserStats(getUserStats());
            dto.setActiveRolesCount(roleRepository.countByActiveTrue());
            dto.setRoleDistribution(getRoleDistribution());
            dto.setProfileDistribution(getProfileDistribution());
            dto.setTopClients(getTopClientActivities());
            dto.setUserEvolution(getUserEvolution(6));
            dto.setProjectStats(getProjectStats());
            dto.setRecentActivities(getRecentActivities());
        }

        if (roles.contains("MANAGER")) {
            dto.setPendingProjects(getPendingProjects(currentUser.getId()));
            dto.setProcessedProjects(getProcessedProjects(currentUser.getId()));
            dto.setValidationStats(getValidationStats(currentUser.getId()));
            dto.setManagerProjects(getManagerProjects(currentUser.getId()));
            dto.setProjectDistribution(getProjectDistributionForManager(currentUser.getId()));
        }

        if (roles.contains("CHEF_PROJET")) {
            dto.setMyProjects(getChefProjetProjects(currentUser.getId()));
            dto.setTeamPerformance(getTeamPerformance(currentUser.getId()));
            dto.setTaskStats(getTaskStatsForChefProjet(currentUser.getId()));
            dto.setRecentTasks(getRecentTasksForChefProjet(currentUser.getId()));
        }

        if (roles.contains("MEMBRE_EQUIPE")) {
            dto.setPersonalTaskStats(getPersonalTaskStats(currentUser));
            dto.setCurrentTasks(getCurrentTasks(currentUser));
            dto.setWeeklyEvolution(getWeeklyEvolution(currentUser));
            dto.setPriorityDistribution(getPriorityDistribution(currentUser));
            dto.setPersonalCompletionRate(calculatePersonalCompletionRate(currentUser));
        }

        if (roles.contains("RESPONSABLE_CONTRAT")) {
            dto.setBillingStats(calculateBillingStats());
            dto.setTopClientsByRevenue(getTopClientsByRevenue());
            dto.setProfileDistribution(getProfileDistribution());
            dto.setProjectsByMonth(getProjectsByMonth());
            dto.setActiveClientsCount(getActiveClientsCount());
            dto.setContractValidationRate(calculateContractValidationRate());
        }

        int totalItems = dto.getAllProjects() != null ? dto.getAllProjects().size() : 0;
        dto.setTotalItems(totalItems);
        dto.setTotalPages((int) Math.ceil((double) totalItems / 10));

        return dto;
    }

    // ================= MÉTHODES DE MAPPING =================

    private ReportingDataDto.UserInfoDto mapToUserInfo(User user) {
        ReportingDataDto.UserInfoDto dto = new ReportingDataDto.UserInfoDto();
        dto.setId(user.getId());
        dto.setPrenom(user.getPrenom());
        dto.setNom(user.getNom());
        dto.setEmail(user.getEmail());
        dto.setEnabled(!user.isLocked() && !user.isDeleted());
        dto.setProfileLibelle(user.getProfile() != null ? user.getProfile().getLibelle() : "Non défini");
        dto.setRoles(user.getRoles().stream().map(Role::getNom).collect(Collectors.toList()));
        dto.setCreatedAt(user.getCreatedAt());
        return dto;
    }

    private String getUserFullName(User user) {
        if (user == null) return null;
        return (user.getPrenom() != null ? user.getPrenom() : "") + " " + (user.getNom() != null ? user.getNom() : "");
    }

    private ReportingDataDto.ProjectSummaryDto mapToProjectSummary(Project project) {
        ReportingDataDto.ProjectSummaryDto dto = new ReportingDataDto.ProjectSummaryDto();
        dto.setId(project.getId());
        dto.setName(project.getName());
        dto.setClient(project.getClient());
        dto.setStatus(project.getStatus() != null ? project.getStatus().name() : null);
        dto.setProgressPercentage(project.getProgressPercentage() != null ? project.getProgressPercentage() : 0);
        dto.setCreatedAt(project.getCreatedAt() != null ? project.getCreatedAt().toString() : null);
        dto.setUpdatedAt(project.getUpdatedAt() != null ? project.getUpdatedAt().toString() : null);
        dto.setManagerName(project.getManager() != null ? getUserFullName(project.getManager()) : null);
        dto.setChefProjetName(project.getChefProjet() != null ? getUserFullName(project.getChefProjet()) : null);
        dto.setTeamName(project.getTeam() != null ? project.getTeam().getName() : null);
        dto.setDescription(project.getDescription());
        dto.setRiskLevel(project.getRiskLevel() != null ? project.getRiskLevel().name() : "FAIBLE");
        dto.setStartDate(project.getStartDate());
        dto.setEndDate(project.getEndDate());
        return dto;
    }

    private ReportingDataDto.TaskSummaryDto mapToTaskSummary(Task task) {
        ReportingDataDto.TaskSummaryDto dto = new ReportingDataDto.TaskSummaryDto();
        dto.setId(task.getId());
        dto.setTitle(task.getTitle());
        dto.setProjectName(task.getProject() != null ? task.getProject().getName() : null);
        dto.setProjectId(task.getProject() != null ? task.getProject().getId() : null);
        dto.setPriority(task.getPriority() != null ? task.getPriority().name() : null);
        dto.setStatus(task.getStatus() != null ? task.getStatus().name() : null);
        dto.setStatusLabel(getTaskStatusLabel(task.getStatus()));
        dto.setEstimatedEndDate(task.getEstimatedEndDate() != null ? task.getEstimatedEndDate().toString() : null);
        dto.setCreatedAt(task.getCreatedAt() != null ? task.getCreatedAt().toString() : null);

        boolean isLate = task.getEstimatedEndDate() != null &&
                task.getEstimatedEndDate().isBefore(LocalDate.now()) &&
                task.getStatus() != null && !"Terminé".equals(task.getStatus().name());
        dto.setLate(isLate);
        dto.setPriorityColor(getPriorityColor(task.getPriority()));
        dto.setStatusColor(getStatusColor(task.getStatus()));
        return dto;
    }

    private String getTaskStatusLabel(Status status) {
        if (status == null) return "-";
        switch (status) {
            case A_faire: return "À faire";
            case En_cours: return "En cours";
            case Terminé: return "Terminé";
            case Validation: return "Validation";
            default: return status.name();
        }
    }

    private String getPriorityColor(com.dxc.dxc_platform.enums.Priority priority) {
        if (priority == null) return "#9ca3af";
        switch (priority) {
            case HAUTE: return "#ef4444";
            case MOYENNE: return "#f59e0b";
            case BASSE: return "#10b981";
            default: return "#9ca3af";
        }
    }

    private String getStatusColor(Status status) {
        if (status == null) return "#9ca3af";
        switch (status) {
            case Terminé: return "#10b981";
            case En_cours: return "#3b82f6";
            case Validation: return "#f59e0b";
            case A_faire: return "#9ca3af";
            default: return "#9ca3af";
        }
    }

    // ================= MÉTHODES DE RÉCUPÉRATION =================

    private List<ReportingDataDto.ProjectSummaryDto> getAllProjectSummaries() {
        return projectRepository.findAllByDeletedFalse().stream()
                .map(this::mapToProjectSummary)
                .collect(Collectors.toList());
    }

    private List<ReportingDataDto.TaskSummaryDto> getAllTaskSummaries() {
        return taskRepository.findAll().stream()
                .map(this::mapToTaskSummary)
                .collect(Collectors.toList());
    }

    private ReportingDataDto.UserStatsDto getUserStats() {
        ReportingDataDto.UserStatsDto stats = new ReportingDataDto.UserStatsDto();
        long total = userRepository.countByDeletedFalse();
        long active = userRepository.countActiveUsers();
        stats.setTotalUsers(total);
        stats.setActiveUsers(active);
        stats.setInactiveUsers(total - active);
        stats.setNewUsersThisMonth((int) countNewUsersThisMonth());
        stats.setActivePercentage(total > 0 ? (active * 100.0 / total) : 0);
        return stats;
    }


    private long countNewUsersThisMonth() {
        LocalDateTime startOfMonth = LocalDateTime.now().withDayOfMonth(1).withHour(0).withMinute(0);
        return userRepository.countActiveUsersCreatedAfter(startOfMonth);
    }

    private List<ReportingDataDto.RoleDistributionDto> getRoleDistribution() {
        List<Object[]> results = userRepository.countUsersByRole();
        long total = userRepository.countByDeletedFalse();
        List<ReportingDataDto.RoleDistributionDto> distribution = new ArrayList<>();
        String[] colors = {"#7c3aed", "#3b82f6", "#10b981", "#f59e0b", "#ec4899"};
        int i = 0;
        if (results != null) {
            for (Object[] result : results) {
                ReportingDataDto.RoleDistributionDto dto = new ReportingDataDto.RoleDistributionDto();
                dto.setRole((String) result[0]);
                dto.setCount((Long) result[1]);
                dto.setPercentage(total > 0 ? (dto.getCount() * 100.0 / total) : 0);
                dto.setColor(colors[i++ % colors.length]);
                distribution.add(dto);
            }
        }
        return distribution;
    }

    private List<ReportingDataDto.ProfileDistributionDto> getProfileDistribution() {
        List<Object[]> results = userRepository.countUsersByProfile();
        long total = userRepository.countByDeletedFalse();
        Map<String, Long> countsByProfile = new HashMap<>();

        if (results != null) {
            for (Object[] result : results) {
                String profileName = result[0] != null ? (String) result[0] : "Non défini";
                countsByProfile.put(profileName, (Long) result[1]);
            }
        }

        List<ReportingDataDto.ProfileDistributionDto> distribution = new ArrayList<>();
        String[] colors = {"#7c3aed", "#2563eb", "#0f766e", "#f59e0b", "#dc2626"};
        int i = 0;

        for (Profile profile : profileRepository.findAllByDeletedFalse()) {
            ReportingDataDto.ProfileDistributionDto dto = new ReportingDataDto.ProfileDistributionDto();
            dto.setProfile(profile.getLibelle());
            dto.setCount(countsByProfile.getOrDefault(profile.getLibelle(), 0L));
            dto.setPercentage(total > 0 ? (dto.getCount() * 100.0 / total) : 0);
            dto.setColor(colors[i++ % colors.length]);
            distribution.add(dto);
        }

        if (countsByProfile.containsKey("Non défini")) {
            ReportingDataDto.ProfileDistributionDto dto = new ReportingDataDto.ProfileDistributionDto();
            dto.setProfile("Non défini");
            dto.setCount(countsByProfile.get("Non défini"));
            dto.setPercentage(total > 0 ? (dto.getCount() * 100.0 / total) : 0);
            dto.setColor(colors[i % colors.length]);
            distribution.add(dto);
        }

        distribution.sort(Comparator
                .comparingLong(ReportingDataDto.ProfileDistributionDto::getCount).reversed()
                .thenComparing(ReportingDataDto.ProfileDistributionDto::getProfile, String.CASE_INSENSITIVE_ORDER));
        return distribution;
    }

    private List<ReportingDataDto.ClientActivityDto> getTopClientActivities() {
        List<Project> projects = projectRepository.findAllByDeletedFalse();
        long totalProjects = projects.size();
        Map<String, List<Project>> projectsByClient = projects.stream()
                .filter(project -> project.getClient() != null && !project.getClient().isBlank())
                .collect(Collectors.groupingBy(project -> project.getClient().trim()));

        List<Client> managedClients = clientRepository.findAllByDeletedFalse();
        Set<String> clientNames = new LinkedHashSet<>();

        managedClients.stream()
                .map(Client::getNom)
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(name -> !name.isBlank())
                .forEach(clientNames::add);

        projectsByClient.keySet().forEach(clientNames::add);

        return clientNames.stream()
                .map(clientName -> {
                    List<Project> clientProjects = projectsByClient.getOrDefault(clientName, Collections.emptyList());
                    Client managedClient = managedClients.stream()
                            .filter(client -> client.getNom() != null && clientName.equalsIgnoreCase(client.getNom().trim()))
                            .findFirst()
                            .orElse(null);

                    ReportingDataDto.ClientActivityDto dto = new ReportingDataDto.ClientActivityDto();
                    dto.setClient(clientName);
                    dto.setProjectsCount(clientProjects.size());
                    dto.setAverageProgress(
                            clientProjects.stream()
                                    .map(Project::getProgressPercentage)
                                    .filter(Objects::nonNull)
                                    .mapToInt(Integer::intValue)
                                    .average()
                                    .orElse(0)
                    );

                    clientProjects.stream()
                            .map(project -> project.getUpdatedAt() != null ? project.getUpdatedAt() : project.getCreatedAt())
                            .filter(Objects::nonNull)
                            .max(LocalDateTime::compareTo)
                            .ifPresent(latest -> dto.setLatestActivity(latest.toString()));

                    dto.setRepresentantsCount(
                            managedClient != null && managedClient.getRepresentants() != null
                                    ? managedClient.getRepresentants().size()
                                    : 0
                    );
                    dto.setSharePercentage(totalProjects > 0 ? (clientProjects.size() * 100.0 / totalProjects) : 0);
                    return dto;
                })
                .sorted(Comparator
                        .comparingLong(ReportingDataDto.ClientActivityDto::getProjectsCount).reversed()
                        .thenComparing(Comparator.comparingDouble(ReportingDataDto.ClientActivityDto::getAverageProgress).reversed())
                        .thenComparing(ReportingDataDto.ClientActivityDto::getClient, String.CASE_INSENSITIVE_ORDER))
                .collect(Collectors.toList());
    }

    private List<ReportingDataDto.EvolutionDataDto> getUserEvolution(int months) {
        List<ReportingDataDto.EvolutionDataDto> evolution = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();
        String[] monthNames = {"Jan", "Fév", "Mar", "Avr", "Mai", "Juin", "Juil", "Août", "Sep", "Oct", "Nov", "Déc"};

        for (int i = months - 1; i >= 0; i--) {
            LocalDateTime startDate = now.minusMonths(i).withDayOfMonth(1).withHour(0).withMinute(0);
            LocalDateTime endDate = startDate.plusMonths(1);
            long count = userRepository.countActiveUsersCreatedBetween(startDate, endDate);  // Changement ici

            ReportingDataDto.EvolutionDataDto dto = new ReportingDataDto.EvolutionDataDto();
            dto.setMonth(monthNames[startDate.getMonthValue() - 1]);
            dto.setCount(count);
            evolution.add(dto);
        }
        return evolution;
    }

    private ReportingDataDto.ProjectStatsDto getProjectStats() {
        ReportingDataDto.ProjectStatsDto stats = new ReportingDataDto.ProjectStatsDto();
        List<Project> allProjects = projectRepository.findAllByDeletedFalse();

        stats.setTotalProjects(allProjects.size());
        stats.setProjectsEnCours(allProjects.stream().filter(p -> p.getStatus() == ProjectStatus.EN_COURS).count());
        stats.setProjectsEnValidation(allProjects.stream().filter(p -> p.getStatus() == ProjectStatus.EN_VALIDATION).count());
        stats.setProjectsPreValides(allProjects.stream().filter(p -> p.getStatus() == ProjectStatus.PRE_VALIDE).count());
        stats.setProjectsRejetes(allProjects.stream().filter(p -> p.getStatus() == ProjectStatus.REJETE).count());
        stats.setCompletionRate(0);
        stats.setNewProjectsThisMonth((int) countNewProjectsThisMonth());
        return stats;
    }

    private long countNewProjectsThisMonth() {
        LocalDateTime startOfMonth = LocalDateTime.now().withDayOfMonth(1).withHour(0).withMinute(0);
        return projectRepository.countByDeletedFalseAndCreatedAtAfter(startOfMonth);
    }

    private List<ReportingDataDto.RecentActivityDto> getRecentActivities() {
        List<ReportingDataDto.RecentActivityDto> activities = new ArrayList<>();

        List<Project> recentProjects = projectRepository.findTop5ByDeletedFalseOrderByCreatedAtDesc();
        for (Project project : recentProjects) {
            ReportingDataDto.RecentActivityDto activity = new ReportingDataDto.RecentActivityDto();
            activity.setId(project.getId());
            activity.setType("project");
            activity.setText("Projet \"" + project.getName() + "\" créé");
            activity.setTimestamp(project.getCreatedAt());
            activity.setTime(formatRelativeTime(project.getCreatedAt()));
            activities.add(activity);
        }

        List<User> recentUsers = userRepository.findTop5ByDeletedFalseOrderByCreatedAtDesc();
        for (User user : recentUsers) {
            ReportingDataDto.RecentActivityDto activity = new ReportingDataDto.RecentActivityDto();
            activity.setId(user.getId());
            activity.setType("user");
            activity.setText("Utilisateur \"" + user.getPrenom() + " " + user.getNom() + "\" ajouté");
            activity.setTimestamp(user.getCreatedAt());
            activity.setTime(formatRelativeTime(user.getCreatedAt()));
            activities.add(activity);
        }

        activities.sort((a, b) -> b.getTimestamp().compareTo(a.getTimestamp()));
        return activities.stream().limit(10).collect(Collectors.toList());
    }

    private String formatRelativeTime(LocalDateTime dateTime) {
        if (dateTime == null) return "Date inconnue";
        long minutes = java.time.Duration.between(dateTime, LocalDateTime.now()).toMinutes();
        if (minutes < 1) return "À l'instant";
        if (minutes < 60) return "Il y a " + minutes + " minute(s)";
        if (minutes < 1440) return "Il y a " + (minutes / 60) + " heure(s)";
        return "Il y a " + (minutes / 1440) + " jour(s)";
    }

    // MANAGER METHODS
    private List<ReportingDataDto.ProjectReviewDto> getPendingProjects(Long managerId) {
        List<Project> projects = projectRepository.findAllByDeletedFalseAndStatusAndManagerId(ProjectStatus.EN_VALIDATION, managerId);
        return projects.stream().map(this::mapToProjectReview).collect(Collectors.toList());
    }

    private List<ReportingDataDto.ProjectReviewDto> getProcessedProjects(Long managerId) {
        List<Project> projects = projectRepository.findAllByDeletedFalseAndManagerIdAndStatusIn(managerId,
                Arrays.asList(ProjectStatus.PRE_VALIDE, ProjectStatus.REJETE));
        return projects.stream().map(this::mapToProjectReview).collect(Collectors.toList());
    }

    private ReportingDataDto.ValidationStatsDto getValidationStats(Long managerId) {
        ReportingDataDto.ValidationStatsDto stats = new ReportingDataDto.ValidationStatsDto();
        List<Project> allProjects = projectRepository.findAllByDeletedFalseAndManagerId(managerId);

        int total = allProjects.size();
        int pending = (int) allProjects.stream().filter(p -> p.getStatus() == ProjectStatus.EN_VALIDATION).count();
        int validated = (int) allProjects.stream().filter(p -> p.getStatus() == ProjectStatus.PRE_VALIDE).count();
        int rejected = (int) allProjects.stream().filter(p -> p.getStatus() == ProjectStatus.REJETE).count();

        stats.setTotalProjects(total);
        stats.setPendingCount(pending);
        stats.setValidatedCount(validated);
        stats.setRejectedCount(rejected);
        stats.setValidationRate(total > 0 ? (validated * 100.0 / total) : 0);
        return stats;
    }

    private List<ReportingDataDto.ProjectSummaryDto> getManagerProjects(Long managerId) {
        return projectRepository.findAllByDeletedFalseAndManagerId(managerId).stream()
                .map(this::mapToProjectSummary)
                .collect(Collectors.toList());
    }

    private ReportingDataDto.ChartDataDto getProjectDistributionForManager(Long managerId) {
        ReportingDataDto.ChartDataDto chart = new ReportingDataDto.ChartDataDto();
        chart.setLabels(Arrays.asList("En validation", "Assignés", "Rejetés", "En cours"));

        List<Project> projects = projectRepository.findAllByDeletedFalseAndManagerId(managerId);

        List<Double> data = new ArrayList<>();
        data.add((double) projects.stream().filter(p -> p.getStatus() == ProjectStatus.EN_VALIDATION).count());
        data.add((double) projects.stream().filter(p -> p.getStatus() == ProjectStatus.PRE_VALIDE).count());
        data.add((double) projects.stream().filter(p -> p.getStatus() == ProjectStatus.REJETE).count());
        data.add((double) projects.stream().filter(p -> p.getStatus() == ProjectStatus.EN_COURS).count());

        chart.setData(data);
        chart.setColors(Arrays.asList("#f59e0b", "#10b981", "#ef4444", "#3b82f6"));
        return chart;
    }

    private ReportingDataDto.ProjectReviewDto mapToProjectReview(Project project) {
        ReportingDataDto.ProjectReviewDto dto = new ReportingDataDto.ProjectReviewDto();
        dto.setId(project.getId());
        dto.setName(project.getName());
        dto.setClient(project.getClient());
        dto.setStatus(project.getStatus() != null ? project.getStatus().name() : null);
        dto.setCreatedAt(project.getCreatedAt() != null ? project.getCreatedAt().toString() : null);
        dto.setManagerComment(project.getManagerComment());
        dto.setChefProjetName(project.getChefProjet() != null ? getUserFullName(project.getChefProjet()) : null);
        dto.setManagerName(project.getManager() != null ? getUserFullName(project.getManager()) : null);
        dto.setProgressPercentage(project.getProgressPercentage() != null ? project.getProgressPercentage() : 0);
        return dto;
    }

    // CHEF PROJET METHODS
    private List<ReportingDataDto.ProjectSummaryDto> getChefProjetProjects(Long chefProjetId) {
        return projectRepository.findAllByChefProjetIdAndDeletedFalse(chefProjetId).stream()
                .map(this::mapToProjectSummary)
                .collect(Collectors.toList());
    }

    private List<ReportingDataDto.TeamPerformanceDto> getTeamPerformance(Long chefProjetId) {
        List<Project> projects = projectRepository.findAllByChefProjetIdAndDeletedFalse(chefProjetId);
        Set<Long> memberIds = new HashSet<>();
        for (Project project : projects) {
            if (project.getTeam() != null && project.getTeam().getMembers() != null) {
                for (User member : project.getTeam().getMembers()) {
                    memberIds.add(member.getId());
                }
            }
        }

        List<ReportingDataDto.TeamPerformanceDto> performances = new ArrayList<>();
        for (Long memberId : memberIds) {
            List<Task> memberTasks = taskRepository.findByAssignedToId(memberId);
            long completed = memberTasks.stream().filter(t -> t.getStatus() != null && "Terminé".equals(t.getStatus().name())).count();
            double efficiency = memberTasks.isEmpty() ? 0 : (completed * 100.0 / memberTasks.size());

            ReportingDataDto.TeamPerformanceDto dto = new ReportingDataDto.TeamPerformanceDto();
            User member = userRepository.findById(memberId).orElse(null);
            if (member != null) {
                dto.setMemberId(memberId);
                dto.setMemberName(getUserFullName(member));
                dto.setEmail(member.getEmail());
            }
            dto.setCompletedTasks(completed);
            dto.setEfficiency(efficiency);
            dto.setAverageDelay(0);
            dto.setStatus(efficiency >= 90 ? "EXCELLENT" : efficiency >= 70 ? "BON" : efficiency >= 50 ? "MOYEN" : "À AMÉLIORER");
            performances.add(dto);
        }
        performances.sort((a, b) -> Double.compare(b.getEfficiency(), a.getEfficiency()));
        return performances;
    }

    private ReportingDataDto.TaskStatsDto getTaskStatsForChefProjet(Long chefProjetId) {
        List<Project> projects = projectRepository.findAllByChefProjetIdAndDeletedFalse(chefProjetId);
        List<Task> allTasks = new ArrayList<>();
        for (Project project : projects) {
            allTasks.addAll(taskRepository.findByProjectId(project.getId()));
        }
        return calculateTaskStats(allTasks);
    }

    private List<ReportingDataDto.TaskSummaryDto> getRecentTasksForChefProjet(Long chefProjetId) {
        List<Project> projects = projectRepository.findAllByChefProjetIdAndDeletedFalse(chefProjetId);
        List<Task> allTasks = new ArrayList<>();
        for (Project project : projects) {
            allTasks.addAll(taskRepository.findByProjectId(project.getId()));
        }
        allTasks.sort((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()));
        return allTasks.stream().limit(10).map(this::mapToTaskSummary).collect(Collectors.toList());
    }

    // MEMBRE EQUIPE METHODS
    private ReportingDataDto.TaskStatsDto getPersonalTaskStats(User currentUser) {
        List<Task> tasks = getMemberTasks(currentUser);
        return calculateTaskStats(tasks);
    }

    private ReportingDataDto.TaskStatsDto calculateTaskStats(List<Task> tasks) {
        ReportingDataDto.TaskStatsDto stats = new ReportingDataDto.TaskStatsDto();
        stats.setTotalTasks(tasks.size());
        stats.setCompletedTasks(tasks.stream().filter(t -> t.getStatus() != null && "Terminé".equals(t.getStatus().name())).count());
        stats.setInProgressTasks(tasks.stream().filter(t -> t.getStatus() == Status.En_cours).count());
        stats.setPendingTasks(tasks.stream().filter(t ->
                t.getStatus() == Status.A_faire
                        || t.getStatus() == Status.Validation
                        || t.getStatus() == Status.A_revoir
        ).count());
        stats.setLateTasks(tasks.stream().filter(t -> t.getEstimatedEndDate() != null &&
                t.getEstimatedEndDate().isBefore(LocalDate.now()) &&
                t.getStatus() != null && !"Terminé".equals(t.getStatus().name())).count());
        stats.setCompletionRate(tasks.isEmpty() ? 0 : (stats.getCompletedTasks() * 100.0 / tasks.size()));
        return stats;
    }

    private List<ReportingDataDto.TaskSummaryDto> getCurrentTasks(User currentUser) {
        return getMemberTasksByStatuses(
                        currentUser,
                        Arrays.asList(Status.En_cours, Status.A_faire, Status.Validation, Status.A_revoir)
                )
                .stream().map(this::mapToTaskSummary).collect(Collectors.toList());
    }

    private List<ReportingDataDto.WeeklyEvolutionDto> getWeeklyEvolution(User currentUser) {
        List<ReportingDataDto.WeeklyEvolutionDto> evolution = new ArrayList<>();
        LocalDate today = LocalDate.now();
        String[] weekNames = {"Sem-4", "Sem-3", "Sem-2", "Sem-1", "Cette semaine"};

        for (int i = 4; i >= 0; i--) {
            LocalDate weekStart = today.minusWeeks(i).with(java.time.DayOfWeek.MONDAY);
            LocalDate weekEnd = weekStart.plusDays(6);
            List<Task> weekTasks = getMemberTasks(currentUser).stream()
                    .filter(task -> task.getCreatedAt() != null
                            && !task.getCreatedAt().isBefore(weekStart.atStartOfDay())
                            && !task.getCreatedAt().isAfter(weekEnd.atTime(23, 59, 59)))
                    .collect(Collectors.toList());
            long completed = weekTasks.stream().filter(t -> t.getStatus() != null && "Terminé".equals(t.getStatus().name())).count();

            ReportingDataDto.WeeklyEvolutionDto dto = new ReportingDataDto.WeeklyEvolutionDto();
            dto.setWeek(weekNames[4 - i]);
            dto.setTotal(weekTasks.size());
            dto.setCompleted((int) completed);
            dto.setCompletionRate(weekTasks.isEmpty() ? 0 : (completed * 100.0 / weekTasks.size()));
            evolution.add(dto);
        }
        return evolution;
    }

    private ReportingDataDto.PriorityDistributionDto getPriorityDistribution(User currentUser) {
        List<Task> tasks = getMemberTasks(currentUser);
        ReportingDataDto.PriorityDistributionDto distribution = new ReportingDataDto.PriorityDistributionDto();
        distribution.setHighPriorityCount(tasks.stream().filter(t -> t.getPriority() != null && "HAUTE".equals(t.getPriority().name())).count());
        distribution.setMediumPriorityCount(tasks.stream().filter(t -> t.getPriority() != null && "MOYENNE".equals(t.getPriority().name())).count());
        distribution.setLowPriorityCount(tasks.stream().filter(t -> t.getPriority() != null && "BASSE".equals(t.getPriority().name())).count());
        long total = tasks.size();
        distribution.setHighPriorityPercent(total > 0 ? (distribution.getHighPriorityCount() * 100.0 / total) : 0);
        distribution.setMediumPriorityPercent(total > 0 ? (distribution.getMediumPriorityCount() * 100.0 / total) : 0);
        distribution.setLowPriorityPercent(total > 0 ? (distribution.getLowPriorityCount() * 100.0 / total) : 0);
        return distribution;
    }

    private double calculatePersonalCompletionRate(User currentUser) {
        List<Task> tasks = getMemberTasks(currentUser);
        if (tasks.isEmpty()) return 0;
        long completed = tasks.stream().filter(t -> t.getStatus() != null && "Terminé".equals(t.getStatus().name())).count();
        return completed * 100.0 / tasks.size();
    }

    private List<Task> getMemberTasks(User currentUser) {
        if (currentUser == null || currentUser.getId() == null) {
            return Collections.emptyList();
        }

        if (currentUser.getTeam() != null && currentUser.getTeam().getId() != null) {
            return taskRepository.findAllByAssignedToIdAndProjectTeamIdAndDeletedFalse(
                    currentUser.getId(),
                    currentUser.getTeam().getId()
            );
        }

        return taskRepository.findAllByAssignedToIdAndDeletedFalse(currentUser.getId());
    }

    private List<Task> getMemberTasksByStatuses(User currentUser, List<Status> statuses) {
        return getMemberTasks(currentUser).stream()
                .filter(task -> task.getStatus() != null && statuses.contains(task.getStatus()))
                .collect(Collectors.toList());
    }

    // RESPONSABLE CONTRAT METHODS
    private ReportingDataDto.BillingStatsDto calculateBillingStats() {
        ReportingDataDto.BillingStatsDto stats = new ReportingDataDto.BillingStatsDto();
        BigDecimal tvaRate = new BigDecimal("0.20");
        Map<String, BigDecimal> totalsByProfile = new LinkedHashMap<>();
        Map<String, Long> countsByProfile = new LinkedHashMap<>();
        BigDecimal totalHT = BigDecimal.ZERO;

        for (Task task : taskRepository.findAll()) {
            if (task == null || task.isDeleted()) {
                continue;
            }

            BigDecimal taskAmount = calculateTaskEstimatedAmount(task);
            if (taskAmount.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }

            totalHT = totalHT.add(taskAmount);

            String profileName = getAssignedProfileName(task);
            totalsByProfile.merge(profileName, taskAmount, BigDecimal::add);
            countsByProfile.merge(profileName, 1L, Long::sum);
        }

        final BigDecimal finalTotalHT = totalHT;

        List<ReportingDataDto.ProfileBillingDto> byProfile = totalsByProfile.entrySet().stream()
                .map(entry -> {
                    ReportingDataDto.ProfileBillingDto dto = new ReportingDataDto.ProfileBillingDto();
                    dto.setProfile(entry.getKey());
                    dto.setTotal(entry.getValue());
                    dto.setCount(countsByProfile.getOrDefault(entry.getKey(), 0L));
                    dto.setPercentage(finalTotalHT.compareTo(BigDecimal.ZERO) > 0
                            ? entry.getValue().multiply(BigDecimal.valueOf(100)).doubleValue() / finalTotalHT.doubleValue()
                            : 0);
                    return dto;
                })
                .sorted(Comparator.comparing(ReportingDataDto.ProfileBillingDto::getTotal).reversed())
                .collect(Collectors.toList());

        stats.setTotalHT(totalHT);
        stats.setTotalTTC(totalHT.multiply(BigDecimal.ONE.add(tvaRate)));
        stats.setTvaRate(tvaRate);
        stats.setByProfile(byProfile);
        return stats;
    }

    private List<ReportingDataDto.ClientRevenueDto> getTopClientsByRevenue() {
        List<Project> projects = projectRepository.findAllByDeletedFalse();
        Map<String, List<Project>> projectsByClient = projects.stream()
                .filter(project -> project.getClient() != null && !project.getClient().isBlank())
                .collect(Collectors.groupingBy(project -> project.getClient().trim()));

        Set<String> clientNames = new LinkedHashSet<>();
        clientRepository.findAllByDeletedFalse().stream()
                .map(Client::getNom)
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(name -> !name.isBlank())
                .forEach(clientNames::add);
        projectsByClient.keySet().forEach(clientNames::add);

        long totalProjects = projects.size();

        return clientNames.stream()
                .map(clientName -> {
                    List<Project> clientProjects = projectsByClient.getOrDefault(clientName, Collections.emptyList());
                    BigDecimal clientTotalHT = clientProjects.stream()
                            .map(this::calculateProjectEstimatedAmount)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    ReportingDataDto.ClientRevenueDto dto = new ReportingDataDto.ClientRevenueDto();
                    dto.setClient(clientName);
                    dto.setProjectsCount(clientProjects.size());
                    dto.setTotalHT(clientTotalHT);
                    dto.setSharePercentage(totalProjects > 0 ? (clientProjects.size() * 100.0 / totalProjects) : 0);
                    return dto;
                })
                .sorted(Comparator
                        .comparing(ReportingDataDto.ClientRevenueDto::getTotalHT).reversed()
                        .thenComparing(ReportingDataDto.ClientRevenueDto::getProjectsCount, Comparator.reverseOrder())
                        .thenComparing(ReportingDataDto.ClientRevenueDto::getClient, String.CASE_INSENSITIVE_ORDER))
                .collect(Collectors.toList());
    }

    private List<ReportingDataDto.MonthlyProjectDto> getProjectsByMonth() {
        List<ReportingDataDto.MonthlyProjectDto> projectsByMonth = new ArrayList<>();
        String[] monthNames = {"Jan", "Fév", "Mar", "Avr", "Mai", "Juin", "Juil", "Août", "Sep", "Oct", "Nov", "Déc"};
        long yearlyTotal = 0;
        long[] monthlyCounts = new long[12];

        for (int i = 0; i < 12; i++) {
            monthlyCounts[i] = projectRepository.countByMonth(i + 1);
            yearlyTotal += monthlyCounts[i];
        }

        for (int i = 0; i < 12; i++) {
            ReportingDataDto.MonthlyProjectDto dto = new ReportingDataDto.MonthlyProjectDto();
            dto.setMonth(monthNames[i]);
            dto.setCount(monthlyCounts[i]);
            dto.setPercentage(yearlyTotal > 0 ? (monthlyCounts[i] * 100.0 / yearlyTotal) : 0);
            projectsByMonth.add(dto);
        }
        return projectsByMonth;
    }

    private long getActiveClientsCount() {
        List<Client> managedClients = clientRepository.findAllByDeletedFalse();
        if (!managedClients.isEmpty()) {
            return managedClients.size();
        }
        return projectRepository.countDistinctClient();
    }

    private BigDecimal calculateProjectEstimatedAmount(Project project) {
        if (project == null || project.getId() == null) {
            return BigDecimal.ZERO;
        }

        return taskRepository.findAllByProjectIdAndDeletedFalse(project.getId()).stream()
                .map(this::calculateTaskEstimatedAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal calculateTaskEstimatedAmount(Task task) {
        if (task == null) {
            return BigDecimal.ZERO;
        }

        Integer duration = task.getDureeEstimee();
        if (duration == null || duration <= 0) {
            return BigDecimal.ZERO;
        }

        User assignedUser = task.getAssignedTo();
        if (assignedUser == null || assignedUser.getProfile() == null || assignedUser.getProfile().getTjm() == null) {
            return BigDecimal.ZERO;
        }

        return assignedUser.getProfile().getTjm().multiply(BigDecimal.valueOf(duration));
    }

    private String getAssignedProfileName(Task task) {
        if (task == null || task.getAssignedTo() == null || task.getAssignedTo().getProfile() == null) {
            return "Non affecte";
        }

        String libelle = task.getAssignedTo().getProfile().getLibelle();
        return libelle == null || libelle.isBlank() ? "Non affecte" : libelle;
    }

    private double calculateContractValidationRate() {
        List<Project> allProjects = projectRepository.findAllByDeletedFalse();
        long total = allProjects.size();
        if (total == 0) return 0;
        long preValides = allProjects.stream().filter(p -> p.getStatus() == ProjectStatus.PRE_VALIDE).count();
        return preValides * 100.0 / total;
    }
}

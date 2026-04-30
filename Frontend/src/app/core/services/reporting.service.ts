import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface ReportingDataDto {
  currentUser: UserInfoDto;
  roles: string[];
  
  // Admin
  userStats?: UserStatsDto;
  activeRolesCount?: number;
  roleDistribution?: RoleDistributionDto[];
  profileDistribution?: ProfileDistributionDto[];
  topClients?: ClientActivityDto[];
  userEvolution?: EvolutionDataDto[];
  projectStats?: ProjectStatsDto;
  recentActivities?: RecentActivityDto[];
  
  // Manager
  pendingProjects?: ProjectReviewDto[];
  processedProjects?: ProjectReviewDto[];
  validationStats?: ValidationStatsDto;
  managerProjects?: ProjectSummaryDto[];
  projectDistribution?: ChartDataDto;
  
  // Chef Projet
  myProjects?: ProjectSummaryDto[];
  teamPerformance?: TeamPerformanceDto[];
  taskStats?: TaskStatsDto;
  recentTasks?: TaskSummaryDto[];
  
  // Membre Equipe
  personalTaskStats?: TaskStatsDto;
  currentTasks?: TaskSummaryDto[];
  weeklyEvolution?: WeeklyEvolutionDto[];
  priorityDistribution?: PriorityDistributionDto;
  personalCompletionRate?: number;
  
  // Responsable Contrat
  billingStats?: BillingStatsDto;
  topClientsByRevenue?: ClientRevenueDto[];
  projectsByMonth?: MonthlyProjectDto[];
  activeClientsCount?: number;
  contractValidationRate?: number;
  
  // Commun
  allProjects?: ProjectSummaryDto[];
  allTasks?: TaskSummaryDto[];
  
  // Pagination
  currentPage: number;
  itemsPerPage: number;
  totalPages: number;
  totalItems: number;
}

export interface UserInfoDto {
  id: number;
  prenom: string;
  nom: string;
  email: string;
  enabled: boolean;
  profileLibelle: string;
  roles: string[];
  createdAt: string;
}

export interface UserStatsDto {
  totalUsers: number;
  activeUsers: number;
  inactiveUsers: number;
  newUsersThisMonth: number;
  activePercentage: number;
}

export interface RoleDistributionDto {
  role: string;
  count: number;
  percentage: number;
  color: string;
}

export interface ProfileDistributionDto {
  profile: string;
  count: number;
  percentage: number;
  color: string;
}

export interface ClientActivityDto {
  client: string;
  projectsCount: number;
  averageProgress: number;
  latestActivity: string;
  representantsCount: number;
  sharePercentage: number;
}

export interface EvolutionDataDto {
  month: string;
  count: number;
}

export interface ProjectStatsDto {
  totalProjects: number;
  projectsEnCours: number;
  projectsEnValidation: number;
  projectsPreValides: number;
  projectsRejetes: number;
  completionRate: number;
  newProjectsThisMonth: number;
}

export interface RecentActivityDto {
  id: number;
  type: string;
  text: string;
  time: string;
  timestamp: string;
}

export interface ProjectReviewDto {
  id: number;
  name: string;
  client: string;
  status: string;
  createdAt: string;
  reviewedAt: string;
  managerComment: string;
  chefProjetName: string;
  managerName: string;
  progressPercentage: number;
}

export interface ValidationStatsDto {
  totalProjects: number;
  pendingCount: number;
  validatedCount: number;
  rejectedCount: number;
  validationRate: number;
}

export interface ProjectSummaryDto {
  id: number;
  name: string;
  client: string;
  status: string;
  progressPercentage: number;
  createdAt: string;
  updatedAt: string;
  managerName: string;
  chefProjetName: string;
  teamName: string;
  description: string;
  riskLevel: string;
  startDate: string;
  endDate: string;
}

export interface ChartDataDto {
  labels: string[];
  data: number[];
  colors: string[];
}

export interface TeamPerformanceDto {
  memberId: number;
  memberName: string;
  email: string;
  completedTasks: number;
  efficiency: number;
  averageDelay: number;
  status: string;
}

export interface TaskStatsDto {
  totalTasks: number;
  completedTasks: number;
  inProgressTasks: number;
  pendingTasks: number;
  lateTasks: number;
  completionRate: number;
}

export interface TaskSummaryDto {
  id: number;
  title: string;
  projectName: string;
  projectId: number;
  priority: string;
  status: string;
  statusLabel: string;
  estimatedEndDate: string;
  createdAt: string;
  isLate: boolean;
  priorityColor: string;
  statusColor: string;
}

export interface WeeklyEvolutionDto {
  week: string;
  total: number;
  completed: number;
  completionRate: number;
}

export interface PriorityDistributionDto {
  highPriorityCount: number;
  mediumPriorityCount: number;
  lowPriorityCount: number;
  highPriorityPercent: number;
  mediumPriorityPercent: number;
  lowPriorityPercent: number;
}

export interface BillingStatsDto {
  totalHT: number;
  totalTTC: number;
  tvaRate: number;
  byProfile: ProfileBillingDto[];
}

export interface ProfileBillingDto {
  profile: string;
  total: number;
  count: number;
  percentage: number;
}

export interface ClientRevenueDto {
  client: string;
  projectsCount: number;
  totalHT: number;
  sharePercentage: number;
}

export interface MonthlyProjectDto {
  month: string;
  count: number;
  percentage: number;
}

@Injectable({
  providedIn: 'root'
})
export class ReportingService {
  private apiUrl = 'http://localhost:8080/api/reporting';

  constructor(private http: HttpClient) {}

  getCompleteReporting(): Observable<ReportingDataDto> {
    return this.http.get<ReportingDataDto>(`${this.apiUrl}/complete`);
  }
}

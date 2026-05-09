import { Component, OnInit, inject, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, Router } from '@angular/router';
import { ReportingService, ReportingDataDto, ProjectSummaryDto, TaskSummaryDto } from '../../../core/services/reporting.service';
import { AuthService } from '../../../core/services/auth.service';
import { ProjectDto, ProjectService } from '../../../core/services/project.service';
import { HttpClient } from '@angular/common/http';
import { forkJoin, of } from 'rxjs';
import { catchError } from 'rxjs/operators';
import * as XLSX from 'xlsx';
import jsPDF from 'jspdf';
import autoTable from 'jspdf-autotable';

interface RcTeamMemberInfo {
  id: number;
  fullName: string;
  email: string;
  enabled: boolean;
  roleName: string;
  profileId?: number;
  profileLibelle?: string;
  tjm?: number;
}

interface RcTeamDto {
  id: number;
  name: string;
  description?: string | null;
  members: RcTeamMemberInfo[];
}

@Component({
  selector: 'app-reporting',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './reporting.html',
  styleUrls: ['./reporting.css']
})
export class Reporting implements OnInit {
  private reportingService = inject(ReportingService);
  private authService = inject(AuthService);
  private router = inject(Router);
  private projectService = inject(ProjectService);
  private http = inject(HttpClient);

  private readonly teamsApiUrl = 'http://localhost:8080/api/teams';

  loading = signal(false);
  reportingData = signal<ReportingDataDto | null>(null);
  currentUser = signal<any>(null);
  roles = signal<string[]>([]);
  rcProjects = signal<ProjectDto[]>([]);
  rcTeams = signal<RcTeamDto[]>([]);
  Math = Math;
  
  currentPage = 1;
  itemsPerPage = 10;
  memberTasksPage = 1;
  memberTasksItemsPerPage = 5;
  chefTeamPerformancePage = 1;
  chefTeamPerformanceItemsPerPage = 5;

  ngOnInit() {
    const user = this.authService.getUser();
    this.currentUser.set(user);
    this.roles.set((user?.roles || []).map((r: any) => r.nom || r));
    this.loadReportingData();
  }

  // Role helpers
  isAdmin = computed(() => this.roles().includes('ADMIN'));
  isChefProjet = computed(() => this.roles().includes('CHEF_PROJET'));
  isManager = computed(() => this.roles().includes('MANAGER'));
  isMembreEquipe = computed(() => this.roles().includes('MEMBRE_EQUIPE'));
  isResponsableContract = computed(() => this.roles().includes('RESPONSABLE_CONTRAT'));

  loadReportingData() {
    this.loading.set(true);
    this.reportingService.getCompleteReporting().subscribe({
      next: (data) => {
        this.reportingData.set(data);
        this.currentPage = 1;
        this.memberTasksPage = 1;
        if (this.isResponsableContract()) {
          this.loadResponsableContractFallbackData();
        } else {
          this.loading.set(false);
        }
      },
      error: (err) => {
        console.error('Erreur chargement reporting:', err);
        this.loading.set(false);
      }
    });
  }

  loadResponsableContractFallbackData() {
    forkJoin({
      projects: this.projectService.getAllProjects().pipe(
        catchError((err) => {
          console.error('Erreur fallback projets RC:', err);
          return of([] as ProjectDto[]);
        })
      ),
      teams: this.http.get<RcTeamDto[]>(this.teamsApiUrl).pipe(
        catchError((err) => {
          console.error('Erreur fallback equipes RC:', err);
          return of([] as RcTeamDto[]);
        })
      )
    }).subscribe({
      next: ({ projects, teams }) => {
        this.rcProjects.set(projects ?? []);
        this.rcTeams.set(teams ?? []);
        this.loading.set(false);
      },
      error: (err) => {
        console.error('Erreur fallback reporting RC:', err);
        this.rcProjects.set([]);
        this.rcTeams.set([]);
        this.loading.set(false);
      }
    });
  }

  // ================= MÉTHODES POUR ADMIN =================
  
  get totalUsers(): number {
    return this.reportingData()?.userStats?.totalUsers || 0;
  }
  
  get activeUsers(): number {
    return this.reportingData()?.userStats?.activeUsers || 0;
  }
  
  get inactiveUsers(): number {
    return this.reportingData()?.userStats?.inactiveUsers || 0;
  }
  
  get newUsersThisMonth(): number {
    return this.reportingData()?.userStats?.newUsersThisMonth || 0;
  }
  
  get activePercentage(): number {
    return this.reportingData()?.userStats?.activePercentage || 0;
  }
  
  get roleDistribution() {
    return this.reportingData()?.roleDistribution || [];
  }
  
  get profileDistribution() {
    if (this.shouldUseRcFallbackData()) {
      return this.getFallbackProfileDistribution();
    }
    return this.reportingData()?.profileDistribution || [];
  }
  
  get userEvolution() {
    return this.reportingData()?.userEvolution || [];
  }
  
  get projectStats() {
    return this.reportingData()?.projectStats;
  }
  
  get recentActivities() {
    return this.reportingData()?.recentActivities || [];
  }
  
  getTopClientsAdmin() {
    return this.reportingData()?.topClients || [];
  }
  
  getClientShare(projectsCount: number): number {
    const total = this.projectStats?.totalProjects || 1;
    return Math.round((projectsCount / total) * 100);
  }
  
  getClientRepresentantsCount(clientName: string): number {
    return this.topClientsList.find(client => client.client === clientName)?.representantsCount || 0;
  }
  
  formatAdminActivityDate(dateStr?: string): string {
    if (!dateStr) return 'Non disponible';
    return new Date(dateStr).toLocaleDateString('fr-FR');
  }

  formatPercent(value?: number): string {
    if (value == null || Number.isNaN(value)) return '0%';
    return `${Math.round(value * 10) / 10}%`;
  }
  
  getUserEvolutionPoints(): string {
    const data = this.userEvolution;
    if (data.length === 0) return '';
    const max = Math.max(...data.map(d => d.count), 1);
    const width = 600;
    const height = 200;
    const step = width / (data.length - 1);
    
    return data.map((point, index) => {
      const x = 50 + (index * step);
      const y = 180 - (point.count / max) * 160;
      return `${x},${y}`;
    }).join(' ');
  }
  
  getMaxUserCount(): number {
    const data = this.userEvolution;
    if (data.length === 0) return 1;
    return Math.max(...data.map(d => d.count), 1);
  }
  
  getRoleColor(role: string): string {
    const colors: Record<string, string> = {
      'ADMIN': '#7c3aed',
      'MANAGER': '#3b82f6',
      'CHEF_PROJET': '#10b981',
      'RESPONSABLE_CONTRAT': '#f59e0b',
      'MEMBRE_EQUIPE': '#ec4899'
    };
    return colors[role] || '#9ca3af';
  }
  
  getProfileColor(profile: string): string {
    const colors = ['#7c3aed', '#2563eb', '#0f766e', '#f59e0b', '#dc2626', '#db2777', '#0891b2', '#65a30d'];
    let hash = 0;
    for (let i = 0; i < profile.length; i++) {
      hash = profile.charCodeAt(i) + ((hash << 5) - hash);
    }
    return colors[Math.abs(hash) % colors.length];
  }
  
  setUserChartPeriod(period: any) {
    // À implémenter si besoin
  }

  // ================= MÉTHODES COMMUNES =================

  private shouldUseRcFallbackData(): boolean {
    if (!this.isResponsableContract()) {
      return false;
    }

    const data = this.reportingData();
    const fallbackProjects = this.rcProjects();
    if (fallbackProjects.length === 0) {
      return false;
    }

    const apiProjectsCount = data?.allProjects?.length ?? 0;
    const apiClientsCount = data?.activeClientsCount ?? 0;
    const apiBillingHT = data?.billingStats?.totalHT ?? 0;

    return apiProjectsCount === 0 || apiClientsCount === 0 || apiBillingHT === 0;
  }

  private mapRcProject(project: ProjectDto): ProjectSummaryDto {
    return {
      id: project.id ?? 0,
      name: project.name ?? '',
      client: project.client ?? '',
      status: project.status ?? '',
      progressPercentage: project.progressPercentage ?? 0,
      createdAt: project.createdAt ?? '',
      updatedAt: project.updatedAt ?? '',
      managerName: project.managerName ?? '',
      chefProjetName: project.chefProjetName ?? '',
      teamName: project.teamName ?? this.getTeamName(project.teamId),
      description: project.description ?? '',
      riskLevel: project.riskLevel ?? 'FAIBLE',
      startDate: project.startDate ?? '',
      endDate: project.endDate ?? ''
    };
  }

  private getTeamName(teamId?: number): string {
    if (!teamId) return '';
    return this.rcTeams().find(team => team.id === teamId)?.name || '';
  }
  
  get projets() {
    if (this.shouldUseRcFallbackData()) {
      return this.rcProjects().map(project => this.mapRcProject(project));
    }
    return this.reportingData()?.allProjects || [];
  }
  
  get totalProjects(): number {
    return this.projets.length;
  }
  
  get projetsEnCours(): number {
    return this.projets.filter(p => p.status === 'EN_COURS' || p.status === 'PRE_VALIDE').length;
  }
  
  get projetsEnValidation(): number {
    return this.projets.filter(p => p.status === 'EN_VALIDATION').length;
  }
  
  get projetsPreValides(): number {
    return this.projets.filter(p => p.status === 'PRE_VALIDE').length;
  }
  
  get projetsRejetes(): number {
    return this.projets.filter(p => p.status === 'REJETE').length;
  }
  
  get tauxValidation(): number {
    if (this.totalProjects === 0) return 0;
    return Math.round((this.projetsPreValides / this.totalProjects) * 100);
  }
  
  get allTeams() {
    if (this.shouldUseRcFallbackData()) {
      return this.rcTeams();
    }
    return [];
  }
  
  get activeClientsCount(): number {
    if (this.shouldUseRcFallbackData()) {
      const clients = new Set(
        this.rcProjects()
          .map(project => (project.client || '').trim())
          .filter(client => client.length > 0)
      );
      return clients.size;
    }
    return this.reportingData()?.activeClientsCount || 0;
  }
  
  get totalFactureHT(): number {
    if (this.shouldUseRcFallbackData()) {
      return this.getFallbackTotalFactureHT();
    }
    return this.reportingData()?.billingStats?.totalHT || 0;
  }
  
  get totalFactureTTC(): number {
    if (this.shouldUseRcFallbackData()) {
      return Math.round(this.getFallbackTotalFactureHT() * 1.2);
    }
    return this.reportingData()?.billingStats?.totalTTC || 0;
  }
  
  get topClients() {
    if (this.shouldUseRcFallbackData()) {
      return this.getFallbackTopClients();
    }
    return this.reportingData()?.topClientsByRevenue || [];
  }
  
  get projectsByMonth() {
    if (this.shouldUseRcFallbackData()) {
      return this.getFallbackProjectsByMonth();
    }
    return this.reportingData()?.projectsByMonth || [];
  }
  
  get allTasks() {
    return this.reportingData()?.allTasks || [];
  }
  
  getTachesTerminees(): number {
    return this.allTasks.filter(t => t.status === 'Terminé').length;
  }
  
  getTachesEnCours(): number {
    return this.allTasks.filter(t => t.status === 'En_cours').length;
  }
  
  getProgressionMoyenne(): number {
    if (this.projets.length === 0) return 0;
    const total = this.projets.reduce((sum, p) => sum + (p.progressPercentage || 0), 0);
    return Math.round(total / this.projets.length);
  }
  
  get teamPerformance() {
    return this.reportingData()?.teamPerformance || [];
  }

  private getFallbackTotalFactureHT(): number {
    return this.rcProjects()
      .reduce((sum, project) => sum + this.estimateProjectRevenueHT(project), 0);
  }

  private getFallbackTopClients() {
    const clientMap = new Map<string, { projectsCount: number; totalHT: number }>();
    const totalProjects = this.rcProjects().length;

    for (const project of this.rcProjects()) {
      const client = (project.client || 'Non défini').trim() || 'Non défini';
      const current = clientMap.get(client) || { projectsCount: 0, totalHT: 0 };
      current.projectsCount += 1;
      current.totalHT += this.estimateProjectRevenueHT(project);
      clientMap.set(client, current);
    }

    return Array.from(clientMap.entries())
      .map(([client, data]) => ({
        client,
        projectsCount: data.projectsCount,
        totalHT: Math.round(data.totalHT),
        sharePercentage: totalProjects > 0 ? (data.projectsCount * 100) / totalProjects : 0
      }))
      .sort((a, b) => b.totalHT - a.totalHT || b.projectsCount - a.projectsCount);
  }

  private getFallbackProfileDistribution() {
    const members = this.rcTeams().flatMap(team => team.members || []);
    const total = members.length;
    const profileMap = new Map<string, number>();

    for (const member of members) {
      const profile = (member.profileLibelle || 'Non défini').trim() || 'Non défini';
      profileMap.set(profile, (profileMap.get(profile) || 0) + 1);
    }

    return Array.from(profileMap.entries())
      .map(([profile, count]) => ({
        profile,
        count,
        percentage: total > 0 ? (count * 100) / total : 0,
        color: this.getColorForProfile(profile)
      }))
      .sort((a, b) => b.count - a.count || a.profile.localeCompare(b.profile));
  }

  private getFallbackProjectsByMonth() {
    const months = ['Jan', 'Fév', 'Mar', 'Avr', 'Mai', 'Juin', 'Juil', 'Août', 'Sep', 'Oct', 'Nov', 'Déc'];
    const monthCounts = new Array(12).fill(0);

    for (const project of this.rcProjects()) {
      if (!project.createdAt) continue;
      const date = new Date(project.createdAt);
      if (Number.isNaN(date.getTime())) continue;
      monthCounts[date.getMonth()] += 1;
    }

    const total = monthCounts.reduce((sum, count) => sum + count, 0);

    return months.map((month, index) => ({
      month,
      count: monthCounts[index],
      percentage: total > 0 ? (monthCounts[index] * 100) / total : 0
    }));
  }

  private estimateProjectRevenueHT(project: ProjectDto): number {
    const team = this.rcTeams().find(item => item.id === project.teamId);
    const members = team?.members || [];
    const teamDailyRate = members.reduce((sum, member) => sum + (member.tjm || 0), 0);

    if (teamDailyRate === 0) {
      return 0;
    }

    const businessDays = this.getBusinessDaysBetween(project.startDate, project.endDate);
    const progressRatio = Math.min(Math.max((project.progressPercentage || 0) / 100, 0), 1);
    const effectiveDays = Math.max(1, Math.round(businessDays * progressRatio));

    return teamDailyRate * effectiveDays;
  }

  private getBusinessDaysBetween(startDate?: string, endDate?: string): number {
    if (!startDate || !endDate) {
      return 1;
    }

    const start = new Date(startDate);
    const end = new Date(endDate);

    if (Number.isNaN(start.getTime()) || Number.isNaN(end.getTime()) || start > end) {
      return 1;
    }

    let count = 0;
    const current = new Date(start);

    while (current <= end) {
      const day = current.getDay();
      if (day !== 0 && day !== 6) {
        count++;
      }
      current.setDate(current.getDate() + 1);
    }

    return Math.max(count, 1);
  }

  // ================= MÉTHODES POUR CHEF PROJET =================

  get chefProjects() {
    return this.reportingData()?.myProjects || [];
  }

  get chefTaskStats() {
    return this.reportingData()?.taskStats;
  }

  get recentChefTasks() {
    return this.reportingData()?.recentTasks || [];
  }

  get chefTotalTasks(): number {
    return this.chefTaskStats?.totalTasks || 0;
  }

  get chefCompletedTasks(): number {
    return this.chefTaskStats?.completedTasks || 0;
  }

  get chefInProgressTasks(): number {
    return this.chefTaskStats?.inProgressTasks || 0;
  }

  get chefPendingTasks(): number {
    return this.chefTaskStats?.pendingTasks || 0;
  }

  get chefLateTasks(): number {
    return this.chefTaskStats?.lateTasks || 0;
  }

  get chefCompletionRate(): number {
    return this.chefTaskStats?.completionRate || 0;
  }

  get chefTeamMembersCount(): number {
    return this.teamPerformance.length;
  }

  get chefAverageEfficiency(): number {
    if (this.teamPerformance.length === 0) return 0;
    const total = this.teamPerformance.reduce((sum, member) => sum + (member.efficiency || 0), 0);
    return Math.round((total / this.teamPerformance.length) * 10) / 10;
  }

  getPaginatedChefProjects(): ProjectSummaryDto[] {
    const start = (this.currentPage - 1) * this.itemsPerPage;
    return this.chefProjects.slice(start, start + this.itemsPerPage);
  }

  get chefTotalPages(): number {
    return Math.max(1, Math.ceil(this.chefProjects.length / this.itemsPerPage));
  }

  getChefProjectCount(status: string): number {
    return this.chefProjects.filter(project => project.status === status).length;
  }

  getChefProjectPercentage(status: string): number {
    const total = this.chefProjects.length;
    if (total === 0) return 0;
    return (this.getChefProjectCount(status) / total) * 100;
  }

  get chefHighRiskProjects() {
    return this.chefProjects.filter(project => project.riskLevel === 'ELEVE');
  }

  get chefProjectsInValidation() {
    return this.chefProjects.filter(project => project.status === 'EN_VALIDATION');
  }

  get chefProjectsEndingSoon() {
    const today = new Date();
    const limit = new Date();
    limit.setDate(today.getDate() + 7);

    return this.chefProjects.filter(project => {
      if (!project.endDate) return false;
      const end = new Date(project.endDate);
      return end >= today && end <= limit;
    });
  }

  get chefClientPortfolio() {
    const grouped = new Map<string, { projects: number; averageProgress: number }>();

    for (const project of this.chefProjects) {
      const client = project.client || 'Non défini';
      const current = grouped.get(client) || { projects: 0, averageProgress: 0 };
      current.projects += 1;
      current.averageProgress += project.progressPercentage || 0;
      grouped.set(client, current);
    }

    return Array.from(grouped.entries())
      .map(([client, data]) => ({
        client,
        projects: data.projects,
        averageProgress: data.projects > 0 ? Math.round(data.averageProgress / data.projects) : 0
      }))
      .sort((a, b) => b.projects - a.projects);
  }
  
  // ================= MÉTHODES POUR MANAGER =================
  
  get managerProjects() {
    return this.reportingData()?.managerProjects || [];
  }

  get managerTotalProjects(): number {
    return this.managerProjects.length;
  }

  get managerPendingCount(): number {
    return this.getManagerPendingProjects().length;
  }

  get managerValidatedCount(): number {
    return this.getManagerProcessedProjects().filter(project => project.status === 'PRE_VALIDE').length;
  }

  get managerRejectedCount(): number {
    return this.getManagerProcessedProjects().filter(project => project.status === 'REJETE').length;
  }
  
  get managerValidationRate(): number {
    return this.reportingData()?.validationStats?.validationRate || 0;
  }
  
  getManagerProjectCount(status: string): number {
    return this.managerProjects.filter(p => p.status === status).length;
  }
  
  getProjectPercentage(status: string): number {
    const total = this.managerTotalProjects;
    if (total === 0) return 0;
    return (this.getManagerProjectCount(status) / total) * 100;
  }
  
  getManagerPendingProjects(): any[] {
    return this.reportingData()?.pendingProjects || [];
}
  
  getManagerProcessedProjects(): any[] {
    return this.reportingData()?.processedProjects || [];
  }
  
  // ================= MÉTHODES POUR MEMBRE EQUIPE =================
  
  get totalMyTasks(): number {
    return this.reportingData()?.personalTaskStats?.totalTasks || 0;
  }
  
  get myTasksCompleted(): number {
    return this.reportingData()?.personalTaskStats?.completedTasks || 0;
  }
  
  get myTasksInProgress(): number {
    return this.reportingData()?.personalTaskStats?.inProgressTasks || 0;
  }
  
  get myTasksLate(): number {
    return this.reportingData()?.personalTaskStats?.lateTasks || 0;
  }
  
  get myTasksRemaining(): number {
    return this.totalMyTasks - this.myTasksCompleted;
  }
  
  get myCompletionRate(): number {
    return this.reportingData()?.personalCompletionRate || 0;
  }
  
  get myHighPriorityCount(): number {
    return this.reportingData()?.priorityDistribution?.highPriorityCount || 0;
  }
  
  get myMediumPriorityCount(): number {
    return this.reportingData()?.priorityDistribution?.mediumPriorityCount || 0;
  }
  
  get myLowPriorityCount(): number {
    return this.reportingData()?.priorityDistribution?.lowPriorityCount || 0;
  }
  
  get myHighPriorityPercent(): number {
    return this.reportingData()?.priorityDistribution?.highPriorityPercent || 0;
  }
  
  get myMediumPriorityPercent(): number {
    return this.reportingData()?.priorityDistribution?.mediumPriorityPercent || 0;
  }
  
  get myLowPriorityPercent(): number {
    return this.reportingData()?.priorityDistribution?.lowPriorityPercent || 0;
  }
  
  get myCurrentTasks(): TaskSummaryDto[] {
    return this.reportingData()?.currentTasks || [];
  }

  get paginatedMyCurrentTasks(): TaskSummaryDto[] {
    const start = (this.memberTasksPage - 1) * this.memberTasksItemsPerPage;
    return this.myCurrentTasks.slice(start, start + this.memberTasksItemsPerPage);
  }

  get memberTasksTotalPages(): number {
    return Math.max(1, Math.ceil(this.myCurrentTasks.length / this.memberTasksItemsPerPage));
  }

  goToMemberTasksPage(page: number): void {
    if (page >= 1 && page <= this.memberTasksTotalPages) {
      this.memberTasksPage = page;
    }
  }

  get paginatedTeamPerformance() {
    const start = (this.chefTeamPerformancePage - 1) * this.chefTeamPerformanceItemsPerPage;
    return this.teamPerformance.slice(start, start + this.chefTeamPerformanceItemsPerPage);
  }

  get chefTeamPerformanceTotalPages(): number {
    return Math.max(1, Math.ceil(this.teamPerformance.length / this.chefTeamPerformanceItemsPerPage));
  }

  goToChefTeamPerformancePage(page: number): void {
    if (page >= 1 && page <= this.chefTeamPerformanceTotalPages) {
      this.chefTeamPerformancePage = page;
    }
  }
  
  get weeklyEvolution() {
    return this.reportingData()?.weeklyEvolution || [];
  }
  
  // ================= MÉTHODES UTILITAIRES =================
  
  getPaginatedProjects(): ProjectSummaryDto[] {
    const start = (this.currentPage - 1) * this.itemsPerPage;
    return this.projets.slice(start, start + this.itemsPerPage);
  }
  
  get totalPages(): number {
    if (this.isChefProjet()) {
      return this.chefTotalPages;
    }
    return Math.ceil(this.projets.length / this.itemsPerPage);
  }
  
  get pageNumbers(): number[] {
    return Array.from({ length: this.totalPages }, (_, i) => i + 1);
  }
  
  goToPage(page: number): void {
    if (page >= 1 && page <= this.totalPages) {
      this.currentPage = page;
    }
  }
  
  getStatusLabel(status: string | undefined): string {
    const labels: Record<string, string> = {
      'EN_VALIDATION': 'En validation',
      'PRE_VALIDE': 'Pré-validé',
      'EN_COURS': 'En cours',
      'REJETE': 'Rejeté',
      'VALIDE': 'Validé'
    };
    return status ? (labels[status] ?? status) : '-';
  }
  
  getTaskStatusLabel(status?: string): string {
    const labels: Record<string, string> = {
      'A_faire': 'À faire',
      'En_cours': 'En cours',
      'Terminé': 'Terminé',
      'Validation': 'Validation',
      'A_revoir': 'À revoir'
    };
    return status ? (labels[status] ?? status) : '-';
  }
  
  getPriorityLabel(priority?: string): string {
    const labels: Record<string, string> = {
      'HAUTE': 'Haute',
      'MOYENNE': 'Moyenne',
      'BASSE': 'Basse'
    };
    return priority ? (labels[priority] ?? priority) : '-';
  }
  
  getInitials(name: string): string {
    return name ? name.substring(0, 2).toUpperCase() : 'PR';
  }
  
  getAvatarColor(index: number): string {
    const colors = ['#7c3aed', '#3b82f6', '#10b981', '#f59e0b', '#ef4444'];
    return colors[index % colors.length];
  }
  
  getColorForProfile(profile: string): string {
    const colors = ['#7c3aed', '#3b82f6', '#10b981', '#f59e0b', '#ef4444', '#ec4899', '#06b6d4', '#84cc16'];
    let hash = 0;
    for (let i = 0; i < profile.length; i++) {
      hash = profile.charCodeAt(i) + ((hash << 5) - hash);
    }
    return colors[Math.abs(hash) % colors.length];
  }
  
  formatTaskDate(dateStr?: string): string {
    if (!dateStr) return 'Non définie';
    const date = new Date(dateStr);
    const today = new Date();
    today.setHours(0, 0, 0, 0);
    const dueDate = new Date(date);
    dueDate.setHours(0, 0, 0, 0);
    
    const diffDays = Math.ceil((dueDate.getTime() - today.getTime()) / (1000 * 3600 * 24));
    
    if (diffDays < 0) return `En retard (${Math.abs(diffDays)}j)`;
    if (diffDays === 0) return "Aujourd'hui";
    if (diffDays === 1) return "Demain";
    return date.toLocaleDateString('fr-FR', { day: '2-digit', month: 'short' });
  }
  
  isTaskLate(task: TaskSummaryDto): boolean {
    return task.isLate || false;
  }
  
  getReportingBreadcrumbLabel(): string {
    if (this.isAdmin()) return 'Reporting admin';
    if (this.isChefProjet()) return 'Reporting chef de projet';
    if (this.isResponsableContract()) return 'Reporting responsable contrat';
    if (this.isManager()) return 'Reporting manager';
    if (this.isMembreEquipe()) return 'Mon reporting';
    return 'Reporting';
  }
  
  goToDashboard(): void {
    if (this.isAdmin()) {
      this.router.navigate(['/admin/dashboard']);
    } else if (this.isChefProjet()) {
      this.router.navigate(['/chef-projet/dashboard']);
    } else if (this.isManager()) {
      this.router.navigate(['/manager/dashboard']);
    } else if (this.isResponsableContract()) {
      this.router.navigate(['/responsable-contrat/dashboard']);
    } else {
      this.router.navigate(['/chef-projet/dashboard']);
    }
  }
  
  exportReporting(): void {
    if (!this.isResponsableContract()) {
      console.log('Export disponible pour le reporting responsable contrat');
      return;
    }

    const fileName = `reporting_responsable_contrat_${new Date().toISOString().split('T')[0]}`;
    const wb = XLSX.utils.book_new();

    const summaryData = [
      ['Indicateur', 'Valeur'],
      ['Projets créés', this.projets.length],
      ['En validation', this.projetsEnValidation],
      ['Pré-validés', this.projetsPreValides],
      ['Rejetés', this.projetsRejetes],
      ['Clients actifs', this.activeClientsCount],
      ['CA estimé HT (MAD)', this.totalFactureHT],
      ['CA estimé TTC (MAD)', this.totalFactureTTC],
      ['Taux de validation', this.formatPercent(this.tauxValidation)]
    ];
    const summarySheet = XLSX.utils.aoa_to_sheet(summaryData);
    summarySheet['!cols'] = [{ wch: 28 }, { wch: 18 }];
    XLSX.utils.book_append_sheet(wb, summarySheet, 'Synthese');

    const profileRows = [
      ['Profil', 'Utilisateurs', 'Part'],
      ...this.profileDistribution.map(item => [
        item.profile,
        item.count,
        this.formatPercent(item.percentage)
      ])
    ];
    const profileSheet = XLSX.utils.aoa_to_sheet(profileRows);
    profileSheet['!cols'] = [{ wch: 24 }, { wch: 14 }, { wch: 14 }];
    XLSX.utils.book_append_sheet(wb, profileSheet, 'Profils');

    const clientRows = [
      ['Client', 'Projets', 'Part', 'CA estime HT (MAD)'],
      ...this.topClients.map(client => [
        client.client,
        client.projectsCount,
        this.projets.length > 0 ? this.formatPercent((client.projectsCount / this.projets.length) * 100) : '0%',
        client.totalHT || 0
      ])
    ];
    const clientSheet = XLSX.utils.aoa_to_sheet(clientRows);
    clientSheet['!cols'] = [{ wch: 28 }, { wch: 12 }, { wch: 12 }, { wch: 20 }];
    XLSX.utils.book_append_sheet(wb, clientSheet, 'Top_Clients');

    const monthRows = [
      ['Mois', 'Nombre de projets'],
      ...this.projectsByMonth.map(item => [item.month, item.count])
    ];
    const monthSheet = XLSX.utils.aoa_to_sheet(monthRows);
    monthSheet['!cols'] = [{ wch: 16 }, { wch: 18 }];
    XLSX.utils.book_append_sheet(wb, monthSheet, 'Projets_Par_Mois');

    XLSX.writeFile(wb, `${fileName}.xlsx`);
  }
  
  openAssignModalForProject(project: any): void {
    console.log('Assigner chef pour:', project.name);
  }
  
  openRejectModalForProject(project: any): void {
    console.log('Rejeter projet:', project.name);
  }
  // Dans reporting.ts, ajoutez cette propriété get
get getManagerPendingProjectsList(): any[] {
    return this.reportingData()?.pendingProjects || [];
}
// ================= AJOUTEZ CES PROPRIÉTÉS DANS LA CLASSE REPORTING =================

// Dans reporting.ts, ajoutez ces getters :

get activeRolesCount(): number {
    return this.reportingData()?.activeRolesCount || 0;
}

get totalPermissions(): number {
    // Retourne le nombre total de permissions
    return 0;
}

get topClientsList(): any[] {
    // Retourne la liste des top clients
    return this.reportingData()?.topClients || [];
}

get currentYear(): number {
    return new Date().getFullYear();
}
// Dans reporting.ts, remplacez exportAdminToPdf par :

exportAdminToPdf(): void {
  if (!this.isAdmin()) return;
  
  this.http.get('http://localhost:8080/api/reporting/export-pdf', {
    responseType: 'blob'
  }).subscribe({
    next: (blob) => {
      const url = window.URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.href = url;
      link.download = `rapport_admin_${new Date().toISOString().split('T')[0]}.pdf`;
      link.click();
      window.URL.revokeObjectURL(url);
    },
    error: (err) => {
      console.error('Erreur export PDF:', err);
    }
  });
}
// ================= EXPORT PDF POUR CHEF PROJET =================

exportChefProjetToPdf(): void {
  console.log('🔵 Export PDF Chef Projet appelé');
  
  if (!this.isChefProjet()) {
    console.log('❌ Utilisateur non autorisé (pas chef projet)');
    return;
  }
  
  this.http.get('http://localhost:8080/api/reporting/export-chef-pdf', {
    responseType: 'blob',
    headers: {
      'Accept': 'application/pdf'
    }
  }).subscribe({
    next: (blob: Blob) => {
      console.log('✅ PDF reçu, taille:', blob.size, 'bytes');
      
      if (blob.size === 0) {
        console.error('❌ Le PDF est vide');
        return;
      }
      
      const url = window.URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.href = url;
      const fileName = `rapport_chef_projet_${new Date().toISOString().split('T')[0]}.pdf`;
      link.download = fileName;
      document.body.appendChild(link);
      link.click();
      document.body.removeChild(link);
      window.URL.revokeObjectURL(url);
      
      console.log('✅ Téléchargement du PDF lancé:', fileName);
    },
    error: (err) => {
      console.error('❌ Erreur export PDF:', err);
      if (err.status === 404) {
        console.error('Endpoint non trouvé, vérifiez l\'URL: /api/reporting/export-chef-pdf');
      } else if (err.status === 403) {
        console.error('Accès non autorisé');
      } else if (err.status === 500) {
        console.error('Erreur serveur, vérifiez les logs backend');
      }
    }
  });
}
// ================= EXPORT PDF POUR MANAGER =================

exportManagerToPdf(): void {
  console.log('🔵 Export PDF Manager appelé');
  
  if (!this.isManager()) {
    console.log('❌ Utilisateur non autorisé');
    return;
  }
  
  this.http.get('http://localhost:8080/api/reporting/export-manager-pdf', {
    responseType: 'blob'
  }).subscribe({
    next: (blob) => {
      const url = window.URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.href = url;
      link.download = `rapport_manager_${new Date().toISOString().split('T')[0]}.pdf`;
      link.click();
      window.URL.revokeObjectURL(url);
      console.log('✅ PDF Manager téléchargé');
    },
    error: (err) => {
      console.error('❌ Erreur export PDF Manager:', err);
    }
  });
}
exportMembreEquipeToPdf(): void {
  console.log('🔵 Export PDF Membre Equipe appelé');
  
  if (!this.isMembreEquipe()) {
    console.log('❌ Utilisateur non autorisé');
    // Utilisez votre système existant
    if (this.showError) this.showError('Vous n\'êtes pas autorisé');
    return;
  }
  
  this.http.get('http://localhost:8080/api/reporting/export-membre-pdf', {
    responseType: 'blob'
  }).subscribe({
    next: (blob) => {
      const url = window.URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.href = url;
      link.download = `rapport_membre_equipe_${new Date().toISOString().split('T')[0]}.pdf`;
      link.click();
      window.URL.revokeObjectURL(url);
      // Utilisez votre système existant
      if (this.showSuccess) this.showSuccess('PDF exporté avec succès !');
    },
    error: (err) => {
      console.error('❌ Erreur export PDF:', err);
      // Utilisez votre système existant
      if (this.showError) this.showError('Erreur lors de l\'export PDF');
    }
  });
}
// Ajoutez ces méthodes après la méthode exportMembreEquipeToPdf()

showSuccess(msg: string): void {
  console.log('✅', msg);
  
  // Créer une notification toast temporaire
  const toast = document.createElement('div');
  toast.textContent = msg;
  toast.style.position = 'fixed';
  toast.style.bottom = '20px';
  toast.style.right = '20px';
  toast.style.backgroundColor = '#10b981';
  toast.style.color = 'white';
  toast.style.padding = '12px 20px';
  toast.style.borderRadius = '8px';
  toast.style.zIndex = '9999';
  toast.style.fontSize = '14px';
  toast.style.fontWeight = '600';
  toast.style.boxShadow = '0 4px 12px rgba(0,0,0,0.15)';
  toast.style.animation = 'slideIn 0.3s ease';
  document.body.appendChild(toast);
  
  setTimeout(() => {
    toast.style.opacity = '0';
    setTimeout(() => toast.remove(), 300);
  }, 3000);
}

showError(msg: string): void {
  console.error('❌', msg);
  
  // Créer une notification toast temporaire
  const toast = document.createElement('div');
  toast.textContent = msg;
  toast.style.position = 'fixed';
  toast.style.bottom = '20px';
  toast.style.right = '20px';
  toast.style.backgroundColor = '#ef4444';
  toast.style.color = 'white';
  toast.style.padding = '12px 20px';
  toast.style.borderRadius = '8px';
  toast.style.zIndex = '9999';
  toast.style.fontSize = '14px';
  toast.style.fontWeight = '600';
  toast.style.boxShadow = '0 4px 12px rgba(0,0,0,0.15)';
  toast.style.animation = 'slideIn 0.3s ease';
  document.body.appendChild(toast);
  
  setTimeout(() => {
    toast.style.opacity = '0';
    setTimeout(() => toast.remove(), 300);
  }, 3000);
}
// ================= EXPORT PDF POUR RESPONSABLE CONTRAT =================

exportResponsableContratToPdf(): void {
  console.log('🔵 Export PDF Responsable Contrat appelé');
  
  if (!this.isResponsableContract()) {
    console.log('❌ Utilisateur non autorisé');
    return;
  }
  
  this.http.get('http://localhost:8080/api/reporting/export-responsable-pdf', {
    responseType: 'blob'
  }).subscribe({
    next: (blob) => {
      const url = window.URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.href = url;
      link.download = `rapport_responsable_contrat_${new Date().toISOString().split('T')[0]}.pdf`;
      link.click();
      window.URL.revokeObjectURL(url);
      this.showSuccess('PDF exporté avec succès !');
    },
    error: (err) => {
      console.error('❌ Erreur export PDF:', err);
      this.showError('Erreur lors de l\'export PDF');
    }
  });
}
// Ajoutez ces méthodes dans la classe Reporting

getMemberInitials(name: string): string {
  if (!name) return '??';
  return name.substring(0, 2).toUpperCase();
}

// Si getAvatarColor n'existe pas ou prend un string, ajoutez cette version
getAvatarColorByName(name: string): string {
  const colors = ['#7c3aed', '#3b82f6', '#10b981', '#f59e0b', '#ef4444', '#ec4899', '#06b6d4', '#84cc16'];
  let hash = 0;
  for (let i = 0; i < name.length; i++) {
    hash = name.charCodeAt(i) + ((hash << 5) - hash);
  }
  return colors[Math.abs(hash) % colors.length];
}
}

import { Component, OnInit, inject, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { forkJoin, of, Observable } from 'rxjs';
import { catchError, map, switchMap } from 'rxjs/operators';

import { AdminService, PageResponse, UserResponse, ClientResponse } from '../../../core/services/admin.service';
import { AuthService } from '../../../core/services/auth.service';
import { ProjectService, ProjectDto } from '../../../core/services/project.service';
import { TaskService, TaskDto } from '../../../core/services/task.service';
import { RouterModule } from '@angular/router';
import { ManagerService, ManagerProjectItemDto } from '../../../core/services/manager.service';
import { NotificationBellComponent } from '../../../core/services/notification-bell.component';
import { NotificationBellChefComponent } from '../../../core/services/notification-bell-chef.component';
import { NotificationBellMembreComponent } from '../../../core/services/notification-bell-membre.component';
import { NotificationBellManagerComponent } from '../../../core/services/notification-bell-manager.component';
import { NotificationBellRcComponent } from '../../../core/services/notification-bell-rc.component';
import { PredictionService, RiskPredictionResult, TeamRecommendationResult } from '../../../core/services/prediction.service';

interface TeamMemberInfo {
  id: number;
  fullName: string;
  email: string;
  enabled: boolean;
  roleName: string;
  profileId?: number;
  profileLibelle?: string;
  tjm?: number;
  
}

interface TeamPerformance {
  id: number;
  name: string;
  email: string;
  tachesTerminees: number;
  efficacite: number;
  delaiMoyen: number;
  statut: 'EXCELLENT' | 'BON' | 'MOYEN' | 'À AMÉLIORER';
}

interface TeamDto {
  id: number;
  name: string;
  description?: string | null;
  projectManagerId?: number;
  projectManagerName?: string;
  members: TeamMemberInfo[];
  deleted?: boolean;
  createdAt?: string;
  updatedAt?: string;
}

interface DashboardProject {
  id: number;
  name: string;
  client: string;
  status: string;
  description: string;
  progressPercentage: number;
  riskLevel: string;
  startDate: Date;
  endDate: Date;
  deleted: boolean;
  teamId?: number | null;
  createdAt?: string;
  updatedAt?: string;
  managerId?: number;
  managerName?: string;
  chefProjetId?: number;
  chefProjetName?: string;
  createdById?: number;
  createdByName?: string;
  teamName?: string;
  managerComment?: string;
}

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule, RouterModule, NotificationBellComponent,NotificationBellChefComponent, NotificationBellMembreComponent,NotificationBellManagerComponent, NotificationBellRcComponent ],
  templateUrl: './dashboard.html',
  styleUrl: './dashboard.css'
})
export class Dashboard implements OnInit {
  private adminService = inject(AdminService);
  private authService = inject(AuthService);
  private projectService = inject(ProjectService);
  private taskService = inject(TaskService);
  private managerService = inject(ManagerService);
  private http = inject(HttpClient);
  private predictionService = inject(PredictionService);

  private readonly teamsApiUrl = 'http://localhost:8080/api/teams';

  currentUser = signal<any>(null);
  roles = signal<string[]>([]);
  loading = signal(false);
  // ================= AI PREDICTION =================
projectRisks = signal<Map<number, RiskPredictionResult>>(new Map());
projectRecommendations = signal<Map<number, TeamRecommendationResult>>(new Map());
riskLoading = signal<Map<number, boolean>>(new Map());

  // ================= ADMIN =================
  users = signal<UserResponse[]>([]);
  clients = signal<ClientResponse[]>([]);
  totalUsers = signal(0);
  activeUsers = signal(0);
  inactiveUsers = signal(0);
  chefTeamPerformancePage = 1;
chefTeamPerformanceItemsPerPage = 5;

  kpiData = computed(() => [
    { title: 'Total Utilisateurs', value: this.totalUsers().toString(), icon: '👥' },
    { title: 'Utilisateurs Actifs', value: this.activeUsers().toString(), icon: '⚡' },
    { title: 'Utilisateurs Inactifs', value: this.inactiveUsers().toString(), icon: '🚫' },
    { title: 'Taux de Validation', value: '94.2%', icon: '✓' },
    { title: 'Taux de Rejet', value: '5.8%', icon: '✕' },
    { title: 'Charge Moyenne', value: '72%', icon: '⚙️' }
  ]);

  monthlyData = [
    { month: 'JAN', users: 100 },
    { month: 'FEB', users: 150 },
    { month: 'MAR', users: 200 },
    { month: 'APR', users: 180 },
    { month: 'MAY', users: 220 },
    { month: 'JUN', users: 210 },
    { month: 'JUL', users: 280 }
  ];

  chartData = [
    { week: 'W1', valid: 85, reject: 15 },
    { week: 'W2', valid: 88, reject: 12 },
    { week: 'W3', valid: 82, reject: 18 },
    { week: 'W4', valid: 90, reject: 10 }
  ];

  // ================= COMMON PROJECT/TASK DATA =================
  projects = signal<DashboardProject[]>([]);
  tasks = signal<TaskDto[]>([]);
  allTeams = signal<TeamDto[]>([]);

  // ================= PAGINATION ADMIN =================
  // Pagination pour "Charge par Utilisateur"
  adminUserLoadCurrentPage = 1;
  adminUserLoadItemsPerPage = 5;

  // Pagination pour "Moniteur Haute Charge & Alertes de Validation"
  adminAlertsCurrentPage = 1;
  adminAlertsItemsPerPage = 5;

  // ================= PERFORMANCES MEMBRES (PAGINATION) =================
  performanceCurrentPage = 1;
  performanceItemsPerPage = 5;
  

    // ================= ADMIN (version enrichie) =================
  
  // Nouveaux getters pour le reporting admin
  get totalUsersCount(): number {
    return this.users().length;
  }

  get activeUsersCount(): number {
    return this.users().filter(u => !u.locked).length;
  }

  get inactiveUsersCount(): number {
    return this.users().filter(u => u.locked).length;
  }

  get newUsersThisMonth(): number {
    const now = new Date();
    const month = now.getMonth();
    const year = now.getFullYear();
    return this.users().filter(user => {
      const createdAt = (user as any).createdAt || (user as any).updatedAt;
      if (!createdAt) return false;
      const date = new Date(createdAt);
      return !isNaN(date.getTime()) && date.getFullYear() === year && date.getMonth() === month;
    }).length;
  }

  get activeUsersPercentage(): number {
    if (this.totalUsersCount === 0) return 0;
    return (this.activeUsersCount / this.totalUsersCount) * 100;
  }

  get activeRolesCount(): number {
    const roles = new Set(this.users().flatMap(u => u.roles?.map(r => r.nom) || []));
    return roles.size;
  }

  get roleDistributionAdmin(): { role: string; count: number; percentage: number }[] {
    const roleCount = new Map<string, number>();
    for (const user of this.users()) {
      for (const role of user.roles || []) {
        const roleName = role.nom;
        roleCount.set(roleName, (roleCount.get(roleName) || 0) + 1);
      }
    }
    return Array.from(roleCount.entries())
      .map(([role, count]) => ({
        role,
        count,
        percentage: (count / this.totalUsersCount) * 100
      }))
      .sort((a, b) => b.count - a.count);
  }

  get userEvolutionAdmin(): { month: string; count: number }[] {
    const months = ['Jan', 'Fév', 'Mar', 'Avr', 'Mai', 'Juin', 'Juil', 'Août', 'Sep', 'Oct', 'Nov', 'Déc'];
    const monthCounts = new Array(12).fill(0);

    const datedUsers = this.users().filter(user => {
      const createdAt = (user as any).createdAt || (user as any).updatedAt;
      return !!createdAt;
    });

    for (const user of datedUsers) {
      const createdAt = (user as any).createdAt || (user as any).updatedAt;
      const date = new Date(createdAt);
      if (isNaN(date.getTime())) continue;
      monthCounts[date.getMonth()] += 1;
    }

    const result = months.map((month, index) => ({ month, count: monthCounts[index] }));

    if (this.totalUsersCount > 0 && datedUsers.length === 0) {
      const approximatePerMonth = Math.ceil(this.totalUsersCount / 12);
      return months.map((month, index) => ({ month, count: Math.min(this.totalUsersCount, approximatePerMonth * (index + 1)) }));
    }

    return result;
  }

  getUserEvolutionPoints(): string {
    const data = this.userEvolutionAdmin;
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
    const data = this.userEvolutionAdmin;
    if (data.length === 0) return 1;
    return Math.max(...data.map(d => d.count), 1);
  }

  get profileDistributionAdmin(): { profile: string; count: number; percentage: number }[] {
    const profileCount = new Map<string, number>();
    for (const user of this.users()) {
      const profile = (user as any).profileLibelle?.trim() || 'Non défini';
      profileCount.set(profile, (profileCount.get(profile) || 0) + 1);
    }
    return Array.from(profileCount.entries())
      .map(([profile, count]) => ({
        profile,
        count,
        percentage: this.totalUsersCount > 0 ? (count / this.totalUsersCount) * 100 : 0
      }))
      .sort((a, b) => b.count - a.count);
  }

  get topClientsList(): any[] {
    return [...this.clients()]
      .sort((a, b) => b.id - a.id)
      .slice(0, 6)
      .map(client => ({
        client: client.nom,
        representantsCount: client.representantsCount,
        createdAt: (client as any).createdAt
      }));
  }

  getClientShare(projectsCount: number): number {
    const totalProjects = this.topClientsList.reduce((sum, client) => sum + (client.projectsCount || 0), 0);
    return totalProjects > 0 ? (projectsCount * 100) / totalProjects : 0;
  }

  getClientRepresentantsCount(clientName: string): number {
    const client = this.clients().find(c => (c.nom || '').trim() === (clientName || '').trim());
    if (client) {
      return client.representantsCount;
    }
    const relatedProjects = this.projets.filter(p => (p.client || '').trim() === (clientName || '').trim());
    const contacts = new Set<string>();
    relatedProjects.forEach(p => {
      if (p.managerName) {
        contacts.add(p.managerName.trim());
      }
      if (p.chefProjetName) {
        contacts.add(p.chefProjetName.trim());
      }
    });
    return contacts.size;
  }

  private getClientAverageProgress(clientName: string): number {
    const relatedProjects = this.projets.filter(p => (p.client || '').trim() === (clientName || '').trim());
    if (relatedProjects.length === 0) return 0;
    const totalProgress = relatedProjects.reduce((sum, project) => sum + (project.progressPercentage || 0), 0);
    return totalProgress / relatedProjects.length;
  }

  private getClientLatestActivity(clientName: string): string | null {
    const relatedProjects = this.projets
      .filter(p => (p.client || '').trim() === (clientName || '').trim() && p.updatedAt)
      .sort((a, b) => (new Date(b.updatedAt || '').getTime() || 0) - (new Date(a.updatedAt || '').getTime() || 0));
    return relatedProjects.length > 0 ? relatedProjects[0].updatedAt || null : null;
  }

  formatAdminActivityDate(dateStr?: string): string {
    if (!dateStr) return 'Non disponible';
    return new Date(dateStr).toLocaleDateString('fr-FR');
  }

  exportAdminToPdf(): void {
    console.log('🔵 Export PDF Admin appelé');
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
        this.showToast('PDF exporté avec succès !', 'success');
      },
      error: (err) => {
        console.error('❌ Erreur export PDF:', err);
        this.showToast('Erreur lors de l\'export PDF', 'error');
      }
    });
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

  getProfileColorForAdmin(profile: string): string {
    const colors = ['#7c3aed', '#3b82f6', '#10b981', '#f59e0b', '#ef4444', '#ec4899', '#06b6d4', '#84cc16'];
    let hash = 0;
    for (let i = 0; i < profile.length; i++) {
      hash = profile.charCodeAt(i) + ((hash << 5) - hash);
    }
    return colors[Math.abs(hash) % colors.length];
  }

  getAvatarColorForIndex(index: number): string {
    const colors = ['#7c3aed', '#3b82f6', '#10b981', '#f59e0b', '#ef4444'];
    return colors[index % colors.length];
  }
  // ================= GETTERS PAGINATION ADMIN =================
  
  get paginatedUserLoad(): UserResponse[] {
    const start = (this.adminUserLoadCurrentPage - 1) * this.adminUserLoadItemsPerPage;
    const end = start + this.adminUserLoadItemsPerPage;
    return this.users().slice(start, end);
  }

  get paginatedAlerts(): UserResponse[] {
    const start = (this.adminAlertsCurrentPage - 1) * this.adminAlertsItemsPerPage;
    const end = start + this.adminAlertsItemsPerPage;
    return this.users().slice(start, end);
  }

  get adminUserLoadTotalPages(): number {
    return Math.ceil(this.users().length / this.adminUserLoadItemsPerPage);
  }

  get adminAlertsTotalPages(): number {
    return Math.ceil(this.users().length / this.adminAlertsItemsPerPage);
  }

  get adminUserLoadRangeStart(): number {
    if (this.users().length === 0) return 0;
    return (this.adminUserLoadCurrentPage - 1) * this.adminUserLoadItemsPerPage + 1;
  }

  get adminUserLoadRangeEnd(): number {
    return Math.min(this.adminUserLoadCurrentPage * this.adminUserLoadItemsPerPage, this.users().length);
  }

  get adminAlertsRangeStart(): number {
    if (this.users().length === 0) return 0;
    return (this.adminAlertsCurrentPage - 1) * this.adminAlertsItemsPerPage + 1;
  }

  get adminAlertsRangeEnd(): number {
    return Math.min(this.adminAlertsCurrentPage * this.adminAlertsItemsPerPage, this.users().length);
  }

  // ================= MÉTHODES NAVIGATION ADMIN =================
  
  
  goToAdminUserLoadPage(page: number): void {
    if (page >= 1 && page <= this.adminUserLoadTotalPages) {
      this.adminUserLoadCurrentPage = page;
    }
  }

  previousAdminUserLoadPage(): void {
    if (this.adminUserLoadCurrentPage > 1) {
      this.adminUserLoadCurrentPage--;
    }
  }

  nextAdminUserLoadPage(): void {
    if (this.adminUserLoadCurrentPage < this.adminUserLoadTotalPages) {
      this.adminUserLoadCurrentPage++;
    }
  }

  goToAdminAlertsPage(page: number): void {
    if (page >= 1 && page <= this.adminAlertsTotalPages) {
      this.adminAlertsCurrentPage = page;
    }
  }

  previousAdminAlertsPage(): void {
    if (this.adminAlertsCurrentPage > 1) {
      this.adminAlertsCurrentPage--;
    }
  }

  nextAdminAlertsPage(): void {
    if (this.adminAlertsCurrentPage < this.adminAlertsTotalPages) {
      this.adminAlertsCurrentPage++;
    }
  }

  getAdminUserLoadPageNumbers(): number[] {
    const total = this.adminUserLoadTotalPages;
    const current = this.adminUserLoadCurrentPage;
    const pages: number[] = [];

    if (total <= 7) {
      for (let i = 1; i <= total; i++) pages.push(i);
    } else if (current <= 4) {
      pages.push(1, 2, 3, 4, 5, -1, total);
    } else if (current >= total - 3) {
      pages.push(1, -1, total - 4, total - 3, total - 2, total - 1, total);
    } else {
      pages.push(1, -1, current - 1, current, current + 1, -1, total);
    }
    return pages;
  }

  getAdminAlertsPageNumbers(): number[] {
    const total = this.adminAlertsTotalPages;
    const current = this.adminAlertsCurrentPage;
    const pages: number[] = [];

    if (total <= 7) {
      for (let i = 1; i <= total; i++) pages.push(i);
    } else if (current <= 4) {
      pages.push(1, 2, 3, 4, 5, -1, total);
    } else if (current >= total - 3) {
      pages.push(1, -1, total - 4, total - 3, total - 2, total - 1, total);
    } else {
      pages.push(1, -1, current - 1, current, current + 1, -1, total);
    }
    return pages;
  }

  ngOnInit() {
    const user = this.authService.getUser();
    this.currentUser.set(user);
    // ✅ NORMALISE LES RÔLES POUR GARANTIR LA COHÉRENCE
    const normalized = (user?.roles || []).map((r: any) => {
      let roleStr = typeof r === 'string' ? r : (r?.nom || r?.authority || '');
      roleStr = roleStr.trim().toUpperCase();
      // Supprimer le préfixe 'ROLE_' si présent
      if (roleStr.startsWith('ROLE_')) {
        roleStr = roleStr.substring(5);
      }
      return roleStr;
    });
    this.roles.set(normalized);
    console.log('[Dashboard] Normalized roles from user:', normalized);

    this.loadDashboardData();
  }

  // ================= ROLE HELPERS =================
  isAdmin = computed(() => this.roles().includes('ADMIN'));
  isChefProjet = computed(() => this.roles().includes('CHEF_PROJET'));
  isManager = computed(() => this.roles().includes('MANAGER'));
  isMembreEquipe = computed(() => this.roles().includes('MEMBRE_EQUIPE'));
  isResponsableContract = computed(() => this.roles().includes('RESPONSABLE_CONTRAT'));

  // ================= LOAD =================
  loadDashboardData() {
    this.loading.set(true);

    if (this.isAdmin()) {
      this.loadAdminDashboardData();
      return;
    }

    if (this.isChefProjet()) {
      this.loadChefProjetDashboardData();
      return;
    }

    if (this.isResponsableContract()) {
      this.loadResponsableContractDashboardData();
      return;
    }

    if (this.isManager()) {
      this.loadManagerDashboardData();
      return;
    }

    if (this.isMembreEquipe()) {
      this.loadMembreEquipeDashboardData();
      return;
    }

    this.loading.set(false);
  }

  loadAdminDashboardData() {
    this.loading.set(true);

    forkJoin({
      users: this.adminService.getUsers(1, 100).pipe(
        catchError((err) => {
          console.error('Erreur lors du chargement des utilisateurs:', err);
          return of({
            content: [] as UserResponse[],
            totalElements: 0,
            totalPages: 0,
            size: 0,
            number: 0
          } as PageResponse<UserResponse>);
        })
      ),
      projects: this.projectService.getAllProjects().pipe(
        catchError((err) => {
          console.error('Erreur lors du chargement des projets admin:', err);
          return of([] as ProjectDto[]);
        })
      ),
      clients: this.adminService.getClients(0, 10).pipe(
        catchError((err) => {
          console.error('Erreur lors du chargement des clients GST:', err);
          return of({
            content: [] as ClientResponse[],
            totalElements: 0,
            totalPages: 0,
            size: 0,
            number: 0
          } as PageResponse<ClientResponse>);
        })
      )
    }).subscribe({
      next: ({ users, projects, clients }) => {
        this.users.set(users.content);
        this.totalUsers.set(users.totalElements);
        const active = users.content.filter(u => !u.locked).length;
        const inactive = users.content.filter(u => u.locked).length;
        this.activeUsers.set(active);
        this.inactiveUsers.set(inactive);
        this.projects.set(projects.map((p) => ({
          ...p,
          startDate: new Date(p.startDate),
          endDate: new Date(p.endDate)
        })) as DashboardProject[]);
        this.clients.set(clients.content.sort((a,b) => b.id - a.id).slice(0, 6));
        this.loading.set(false);
      },
      error: (err) => {
        console.error('Erreur lors du chargement du tableau de bord admin:', err);
        this.loading.set(false);
      }
    });
  }

  loadChefProjetDashboardData() {
    forkJoin({
      projects: this.projectService.getMyProjects().pipe(
        catchError((err) => {
          console.error('Erreur projets chef projet:', err);
          return of([]);
        })
      ),
      teams: this.http.get<TeamDto[]>(this.teamsApiUrl).pipe(
        catchError((err) => {
          console.error('Erreur équipes chef projet:', err);
          return of([]);
        })
      )
    })
      .pipe(
        switchMap(({ projects, teams }) => {
          const currentUserId = this.currentUser()?.id;
          // Filter teams to only those managed by the current chef projet
          const filteredTeams = teams?.filter(team => team.projectManagerId === currentUserId) || [];
          
          const mappedProjects: DashboardProject[] = (projects || []).map(p => ({
            id: p.id ?? 0,
            name: p.name ?? '',
            client: p.client ?? '',
            status: p.status ?? '',
            description: p.description ?? '',
            progressPercentage: p.progressPercentage ?? 0,
            riskLevel: p.riskLevel ?? 'FAIBLE',
            startDate: p.startDate ? new Date(p.startDate) : new Date(),
            endDate: p.endDate ? new Date(p.endDate) : new Date(),
            deleted: p.deleted ?? false,
            teamId: p.teamId,
            teamName: filteredTeams.find(t => t.id === p.teamId)?.name,
            createdAt: p.createdAt,
            updatedAt: p.updatedAt,
            managerId: p.managerId,
            managerName: p.managerName,
            chefProjetId: p.chefProjetId,
            chefProjetName: p.chefProjetName
          }));
          
          this.projects.set(mappedProjects);
          this.allTeams.set(filteredTeams);

          const validProjects = mappedProjects.filter(p => p.id != null);
          if (!validProjects.length) return of([] as TaskDto[]);

          return forkJoin(
            validProjects.map(project =>
              this.taskService.getTasksByProject(project.id!).pipe(
                catchError((err) => {
                  console.error(`Erreur tâches projet ${project.id}:`, err);
                  return of([] as TaskDto[]);
                })
              )
            )
          ).pipe(map((lists) => lists.flat()));
        })
      )
      .subscribe({
        next: (tasks) => {
         // APRÈS
this.tasks.set(tasks ?? []);
this.loading.set(false);
setTimeout(() => this.loadAllChefProjectRisks(), 1500);
        },
        error: (err) => {
          console.error('Erreur dashboard chef projet:', err);
          this.loading.set(false);
        }
      });
  }

  loadResponsableContractDashboardData() {
    this.loading.set(true);
    console.log('🟢 Chargement dashboard RESPONSABLE CONTRAT...');
    
    forkJoin({
      projects: this.projectService.getAllProjects().pipe(
        catchError((err) => {
          console.error('❌ Erreur projets RC:', err);
          return of([]);
        })
      ),
      teams: this.http.get<TeamDto[]>(this.teamsApiUrl).pipe(
        catchError((err) => {
          console.error('❌ Erreur équipes RC:', err);
          return of([]);
        })
      )
    }).subscribe({
      next: ({ projects, teams }) => {
        console.log('📁 Projets reçus:', projects);
        console.log('👥 Équipes reçues:', teams);
        
        const mappedProjects: DashboardProject[] = (projects || []).map(p => ({
          id: p.id ?? 0,
          name: p.name ?? '',
          client: p.client ?? '',
          status: p.status ?? '',
          description: p.description ?? '',
          progressPercentage: p.progressPercentage ?? 0,
          riskLevel: p.riskLevel ?? 'FAIBLE',
          startDate: p.startDate ? new Date(p.startDate) : new Date(),
          endDate: p.endDate ? new Date(p.endDate) : new Date(),
          deleted: p.deleted ?? false,
          teamId: p.teamId,
          teamName: teams.find(t => t.id === p.teamId)?.name,
          createdAt: p.createdAt,
          updatedAt: p.updatedAt,
          managerId: p.managerId,
          managerName: p.managerName,
          chefProjetId: p.chefProjetId,
          chefProjetName: p.chefProjetName
        }));
        
        this.projects.set(mappedProjects);
        this.allTeams.set(teams ?? []);
        this.tasks.set([]);
        
        console.log('✅ Projets chargés:', this.projects().length);
        console.log('✅ Premiers projets:', this.projects().slice(0, 2));
        
        this.loading.set(false);
      },
      error: (err) => {
        console.error('❌ Erreur dashboard RC:', err);
        this.loading.set(false);
      }
    });
  }

  loadMembreEquipeDashboardData() {
    this.loading.set(true);
    console.log('🟢 Chargement dashboard Membre Équipe...');
    
    this.projectService.getMyAssignedProjects().subscribe({
      next: (projects: ProjectDto[]) => {
        console.log('📁 Projets reçus:', projects);
        
        const mappedProjects: DashboardProject[] = (projects || []).map(p => ({
          id: p.id ?? 0,
          name: p.name ?? '',
          client: p.client ?? '',
          status: p.status ?? '',
          description: p.description ?? '',
          progressPercentage: p.progressPercentage ?? 0,
          riskLevel: p.riskLevel ?? 'FAIBLE',
          startDate: p.startDate ? new Date(p.startDate) : new Date(),
          endDate: p.endDate ? new Date(p.endDate) : new Date(),
          deleted: p.deleted ?? false,
          teamId: p.teamId,
          teamName: p.teamName,
          createdAt: p.createdAt,
          updatedAt: p.updatedAt,
          managerId: p.managerId,
          managerName: p.managerName,
          chefProjetId: p.chefProjetId,
          chefProjetName: p.chefProjetName
        }));
        
        this.projects.set(mappedProjects);
        console.log('✅ Projets chargés:', this.projects().length);
        
        this.loadMyTasks();
        this.loading.set(false);
      },
      error: (err) => {
        console.error('❌ Erreur chargement projets membre:', err);
        this.projects.set([]);
        this.loading.set(false);
      }
    });
  }

  loadMyTasks() {
    const currentUserId = this.currentUser()?.id;
    if (!currentUserId) {
      console.warn('⚠️ Pas d\'utilisateur connecté');
      return;
    }
    
    this.taskService.getMyTasks().subscribe({
      next: (tasks: TaskDto[]) => {
        console.log('📋 Tâches reçues:', tasks.length);
        this.tasks.set(tasks || []);
      },
      error: (err) => {
        console.error('❌ Erreur chargement tâches:', err);
        this.tasks.set([]);
      }
    });
  }

  loadManagerDashboardData() {
    this.loading.set(true);
    console.log('Loading manager dashboard...');
    
    this.managerService.getManagerProjects().subscribe({
      next: (projects: ManagerProjectItemDto[]) => {
        console.log('Projets reçus du backend:', projects);
        
        if (!projects || projects.length === 0) {
          console.warn('Aucun projet trouvé pour ce manager');
          this.projects.set([]);
          this.loading.set(false);
          return;
        }
        
        const mappedProjects: DashboardProject[] = projects.map(p => ({
          id: p.id,
          name: p.projectName,
          client: p.client || 'Non défini',
          status: p.status,
          createdAt: p.createdAt,
          updatedAt: p.createdAt,
          managerId: this.currentUser()?.id,
          managerName: p.managerName ?? undefined,
          chefProjetId: p.chefProjetId ?? undefined,
          chefProjetName: p.chefProjetName ?? undefined,
          managerComment: p.managerComment ?? undefined,
          description: '',
          progressPercentage: 0,
          riskLevel: 'FAIBLE',
          startDate: new Date(),
          endDate: new Date(),
          deleted: false,
          teamId: undefined
        }));
        
        this.projects.set(mappedProjects);
        console.log('Projets transformés:', mappedProjects);
        this.loading.set(false);
      },
      error: (err) => {
        console.error('Erreur détaillée chargement projets manager:', err);
        this.loading.set(false);
      }
    });
  }

  // ================= ADMIN METHODS =================
  exportData() {
    console.log('Export data initiated - Total users:', this.totalUsers());
  }

  getStatusClass(status: string): string {
    switch (status.toLowerCase()) {
      case 'critical load': return 'status-critical';
      case 'warning load': return 'status-warning';
      case 'validation alert': return 'status-alert';
      case 'optimal': return 'status-optimal';
      default: return 'status-default';
    }
  }

  getChartHeight(valid: number): string {
    return `${valid * 2}px`;
  }

  getChartRejectHeight(reject: number): string {
    return `${reject * 2}px`;
  }

  getUserLoad(user: UserResponse): number {
    if (user.locked) return 95;
    if (user.failedAttempts >= 3) return 80;
    if (user.mustChangePassword) return 65;
    return 45;
  }

  getUserInitials(user: UserResponse): string {
    return `${user.prenom?.charAt(0) ?? ''}${user.nom?.charAt(0) ?? ''}`.toUpperCase();
  }

  getRoleNames(user: UserResponse): string {
    return user.roles?.map(r => r.nom).join(', ') || 'Sans rôle';
  }

  getUserStatus(user: UserResponse): string {
    if (user.locked) return 'Critical';
    if (user.failedAttempts >= 3) return 'Warning';
    return 'Optimal';
  }

  getUserStatusClass(user: UserResponse): string {
    if (user.locked) return 'status-critical';
    if (user.failedAttempts >= 3) return 'status-warning';
    return 'status-optimal';
  }

  // ================= HELPERS =================
  private parseDate(value?: string): Date | null {
    if (!value) return null;
    const date = new Date(value);
    return Number.isNaN(date.getTime()) ? null : date;
  }

  private todayStart(): Date {
    const now = new Date();
    return new Date(now.getFullYear(), now.getMonth(), now.getDate());
  }

  isTaskLate(task: TaskDto): boolean {
    const due = this.parseDate(task.estimatedEndDate);
    if (!due) return false;
    return due < this.todayStart() && task.status !== 'Terminé';
  }

  // ================= CHEF PROJET =================
  cpKpis = computed(() => [
    { 
      title: 'Projets en cours', 
      value: this.projects().filter(p => p.status === 'EN_COURS' || p.status === 'PRE_VALIDE').length 
    },
    { 
      title: 'Tâches urgentes', 
      value: this.tasks().filter(t => this.isTaskLate(t) || t.priority === 'HAUTE').length 
    },
    { 
      title: 'Tâches à valider', 
      value: this.tasks().filter(t => t.status === 'Validation').length 
    },
    { 
      title: 'Projets sans équipe', 
      value: this.projects().filter(p => p.teamId == null).length 
    }
  ]);

  projetsRecents = computed(() =>
    [...this.projects()]
      .sort((a, b) => (this.parseDate(b.updatedAt)?.getTime() ?? 0) - (this.parseDate(a.updatedAt)?.getTime() ?? 0))
      .slice(0, 4)
  );

  tachesCritiques = computed(() =>
    [...this.tasks()]
      .filter(t => t.status === 'Validation')
      .slice(0, 5)
  );
  // ================= CHEF PROJET REPORTING (comme dans reporting) =================
// Pagination spécifique pour les performances membres dans la vue chef projet


// Getter des projets du chef (filtrés par chefProjetId === currentUserId)
get chefProjects(): DashboardProject[] {
  const currentUserId = this.currentUser()?.id;
  return this.projects().filter(p => p.chefProjetId === currentUserId);
}

// Nombre de membres suivis (membres des équipes des projets du chef)
get chefTeamMembersCount(): number {
  const teamIds = new Set(this.chefProjects.map(p => p.teamId).filter(id => id != null));
  const members = this.allTeams().filter(t => teamIds.has(t.id)).flatMap(t => t.members || []);
  return members.length;
}

// Statistiques des tâches pour les projets du chef
get chefTaskStats() {
  const tasks = this.tasks().filter(t => {
    const project = this.projects().find(p => p.id === t.projectId);
    return project?.chefProjetId === this.currentUser()?.id;
  });
  const total = tasks.length;
  const completed = tasks.filter(t => t.status === 'Terminé').length;
  const inProgress = tasks.filter(t => t.status === 'En_cours').length;
  const pending = tasks.filter(t => t.status === 'A_faire').length;
  const late = tasks.filter(t => this.isTaskLate(t)).length;
  return { total, completed, inProgress, pending, late, completionRate: total ? (completed / total) * 100 : 0 };
}

get chefTotalTasks(): number { return this.chefTaskStats.total; }
get chefCompletedTasks(): number { return this.chefTaskStats.completed; }
get chefInProgressTasks(): number { return this.chefTaskStats.inProgress; }
get chefPendingTasks(): number { return this.chefTaskStats.pending; }
get chefLateTasks(): number { return this.chefTaskStats.late; }
get chefCompletionRate(): number { return this.chefTaskStats.completionRate; }

// Nombre de projets par statut pour le chef
getChefProjectCount(status: string): number {
  return this.chefProjects.filter(p => p.status === status).length;
}

// Portefeuille clients
get chefClientPortfolio(): { client: string; projects: number; averageProgress: number }[] {
  const map = new Map<string, { projects: number; sumProgress: number }>();
  for (const p of this.chefProjects) {
    const client = p.client || 'Non défini';
    const cur = map.get(client) || { projects: 0, sumProgress: 0 };
    cur.projects++;
    cur.sumProgress += p.progressPercentage;
    map.set(client, cur);
  }
  return Array.from(map.entries()).map(([client, data]) => ({
    client,
    projects: data.projects,
    averageProgress: Math.round(data.sumProgress / data.projects)
  })).sort((a, b) => b.projects - a.projects);
}

// Alertes
get chefHighRiskProjects(): DashboardProject[] {
  return this.chefProjects.filter(p => p.riskLevel === 'ELEVE');
}
get chefProjectsInValidation(): DashboardProject[] {
  return this.chefProjects.filter(p => p.status === 'EN_VALIDATION');
}
get chefProjectsEndingSoon(): DashboardProject[] {
  const today = new Date();
  const limit = new Date();
  limit.setDate(today.getDate() + 7);
  return this.chefProjects.filter(p => {
    if (!p.endDate) return false;
    const end = new Date(p.endDate);
    return end >= today && end <= limit;
  });
}

// Performances des membres (pour le tableau)
get chefTeamPerformance(): any[] {
  const teamIds = new Set(this.chefProjects.map(p => p.teamId).filter(id => id != null));
  const members = this.allTeams().filter(t => teamIds.has(t.id)).flatMap(t => t.members || []);
  const tasks = this.tasks();
  return members.map(member => {
    const memberTasks = tasks.filter(t => t.assignedToId === member.id);
    const completed = memberTasks.filter(t => t.status === 'Terminé').length;
    const efficiency = memberTasks.length ? (completed / memberTasks.length) * 100 : 0;
    let status = 'À améliorer';
    if (efficiency >= 90) status = 'EXCELLENT';
    else if (efficiency >= 70) status = 'BON';
    else if (efficiency >= 50) status = 'MOYEN';
    return {
      memberId: member.id,
      memberName: member.fullName,
      email: member.email,
      completedTasks: completed,
      efficiency,
      status
    };
  }).sort((a,b) => b.efficiency - a.efficiency);
}

// Pagination pour le tableau des performances
get paginatedTeamPerformance(): any[] {
  const start = (this.chefTeamPerformancePage - 1) * this.chefTeamPerformanceItemsPerPage;
  return this.chefTeamPerformance.slice(start, start + this.chefTeamPerformanceItemsPerPage);
}
get chefTeamPerformanceTotalPages(): number {
  return Math.max(1, Math.ceil(this.chefTeamPerformance.length / this.chefTeamPerformanceItemsPerPage));
}
goToChefTeamPerformancePage(page: number): void {
  if (page >= 1 && page <= this.chefTeamPerformanceTotalPages) {
    this.chefTeamPerformancePage = page;
  }
}
get chefAverageEfficiency(): number {
  if (this.chefTeamPerformance.length === 0) return 0;
  const total = this.chefTeamPerformance.reduce((sum, m) => sum + m.efficiency, 0);
  return total / this.chefTeamPerformance.length;
}

// Méthode utilitaire pour formater les pourcentages
formatPercent(value: number): string {
  return `${Math.round(value * 10) / 10}%`;
}

// Export PDF pour chef projet
exportChefProjetToPdf(): void {
  console.log('🔵 Export PDF Chef Projet appelé');
  this.http.get('http://localhost:8080/api/reporting/export-chef-pdf', {
    responseType: 'blob'
  }).subscribe({
    next: (blob: Blob) => {
      const url = window.URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.href = url;
      link.download = `rapport_chef_projet_${new Date().toISOString().split('T')[0]}.pdf`;
      link.click();
      window.URL.revokeObjectURL(url);
    },
    error: (err) => {
      console.error('❌ Erreur export PDF chef projet:', err);
    }
  });
}

// Couleur pour les clients (identique à getProfileColor)
getProfileColor(client: string): string {
  const colors = ['#7c3aed', '#3b82f6', '#10b981', '#f59e0b', '#ef4444', '#ec4899', '#06b6d4', '#84cc16'];
  let hash = 0;
  for (let i = 0; i < client.length; i++) {
    hash = client.charCodeAt(i) + ((hash << 5) - hash);
  }
  return colors[Math.abs(hash) % colors.length];
}

  

  // ================= PERFORMANCES MEMBRES =================
  teamPerformance = computed(() => {
    const teamMembers = this.allTeams().flatMap(team => team.members || []);
    
    const performances: TeamPerformance[] = [];
    
    for (const member of teamMembers) {
      const tachesTerminees = this.tasks().filter(t => 
        t.assignedToId === member.id && t.status === 'Terminé'
      ).length;
      
      const totalTaches = this.tasks().filter(t => t.assignedToId === member.id).length;
      const efficacite = totalTaches > 0 ? Math.round((tachesTerminees / totalTaches) * 100) : 0;
      const delaiMoyen = this.calculateAverageDelay(member.id);
      
      let statut: 'EXCELLENT' | 'BON' | 'MOYEN' | 'À AMÉLIORER' = 'À AMÉLIORER';
      if (efficacite >= 90) statut = 'EXCELLENT';
      else if (efficacite >= 70) statut = 'BON';
      else if (efficacite >= 50) statut = 'MOYEN';
      
      performances.push({
        id: member.id,
        name: member.fullName,
        email: member.email,
        tachesTerminees,
        efficacite,
        delaiMoyen,
        statut
      });
    }
    
    return performances.sort((a, b) => b.efficacite - a.efficacite);
  });

 
  totalPerformancePages = computed(() => 
    Math.max(1, Math.ceil(this.teamPerformance().length / this.performanceItemsPerPage))
  );

  performanceRangeStart = computed(() => {
    if (this.teamPerformance().length === 0) return 0;
    return (this.performanceCurrentPage - 1) * this.performanceItemsPerPage + 1;
  });

  performanceRangeEnd = computed(() => {
    return Math.min(this.performanceCurrentPage * this.performanceItemsPerPage, this.teamPerformance().length);
  });

  private calculateAverageDelay(memberId: number): number {
    const tasks = this.tasks().filter(t => t.assignedToId === memberId && t.status === 'Terminé');
    if (tasks.length === 0) return 0;
    
    let totalDelay = 0;
    for (const task of tasks) {
      if (task.priority === 'HAUTE') totalDelay += 1.5;
      else if (task.priority === 'MOYENNE') totalDelay += 2.5;
      else totalDelay += 3.5;
    }
    
    return Math.round((totalDelay / tasks.length) * 10) / 10;
  }

  shareReport(): void {
    window.location.href = 'mailto:?subject=Rapport de performances équipe&body=Consultez le rapport complet dans votre dashboard.';
    this.showToast('📧 Client email ouvert', 'success');
  }

  exportPerformanceReport(): void {
    const headers = ['Membre', 'Email', 'Tâches terminées', 'Efficacité', 'Délai moyen', 'Statut'];
    const rows = this.teamPerformance().map(m => [
      m.name,
      m.email,
      m.tachesTerminees,
      `${m.efficacite}%`,
      `${m.delaiMoyen} jours`,
      m.statut
    ]);
    
    const csv = [headers, ...rows].map(row => row.join(';')).join('\n');
    const blob = new Blob(['\uFEFF' + csv], { type: 'text/csv;charset=utf-8;' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = `performances_equipe_${new Date().toISOString().split('T')[0]}.csv`;
    a.click();
    URL.revokeObjectURL(url);
    this.showToast('📊 Rapport exporté avec succès', 'success');
  }

  goToPerformancePage(page: number): void {
    if (page >= 1 && page <= this.totalPerformancePages()) {
      this.performanceCurrentPage = page;
    }
  }

  previousPerformancePage(): void {
    if (this.performanceCurrentPage > 1) {
      this.performanceCurrentPage--;
    }
  }

  nextPerformancePage(): void {
    if (this.performanceCurrentPage < this.totalPerformancePages()) {
      this.performanceCurrentPage++;
    }
  }

  getPerformancePageNumbers(): number[] {
    const total = this.totalPerformancePages();
    const current = this.performanceCurrentPage;
    const pages: number[] = [];
    
    if (total <= 7) {
      for (let i = 1; i <= total; i++) pages.push(i);
    } else if (current <= 4) {
      pages.push(1, 2, 3, 4, 5, -1, total);
    } else if (current >= total - 3) {
      pages.push(1, -1, total - 4, total - 3, total - 2, total - 1, total);
    } else {
      pages.push(1, -1, current - 1, current, current + 1, -1, total);
    }
    return pages;
  }

  showToast(message: string, type: 'success' | 'error' = 'success'): void {
    console.log(`${type}: ${message}`);
  }

  getMemberInitials(name: string): string {
    if (!name) return '??';
    return name.substring(0, 2).toUpperCase();
  }

  getAvatarColor(name: string): string {
    const colors = [
      '#8b5cf6', '#10b981', '#f59e0b', '#ef4444', 
      '#3b82f6', '#ec4899', '#14b8a6', '#f97316',
      '#6366f1', '#06b6d4', '#84cc16', '#d946ef'
    ];
    let hash = 0;
    for (let i = 0; i < name.length; i++) {
      hash = name.charCodeAt(i) + ((hash << 5) - hash);
    }
    return colors[Math.abs(hash) % colors.length];
  }

  // ================= MANAGER =================
  
get managerTotalProjects(): number {
  return this.managerProjects().length;
}

get managerPendingCount(): number {
  return this.getManagerProjectCount('EN_VALIDATION');
}

get managerValidatedCount(): number {
  return this.getManagerProjectCount('PRE_VALIDE');
}

get managerRejectedCount(): number {
  return this.getManagerProjectCount('REJETE');
}

get managerValidationRate(): number {
  const total = this.managerTotalProjects;
  if (total === 0) return 0;
  return (this.managerValidatedCount / total) * 100;
}

// Alias pour faciliter l'utilisation dans le template
getProjectPercentage(status: string): number {
  return this.getManagerProjectPercentage(status);
}

// Retourne la liste des projets déjà traités (validés ou rejetés)
getManagerProcessedProjects(): DashboardProject[] {
  return this.managerProjects().filter(p => p.status === 'PRE_VALIDE' || p.status === 'REJETE');
}

// Export PDF pour le manager
exportManagerToPdf(): void {
  console.log('🔵 Export PDF Manager appelé');
  if (!this.isManager()) return;
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
    },
    error: (err) => console.error('Erreur export PDF manager:', err)
  });
}
  managerProjects = computed(() => {
    const currentUserId = this.currentUser()?.id;
    if (!currentUserId) return [];
    return this.projects().filter(p => p.managerId === currentUserId);
  });

  managerKpis = computed(() => [
    {
      title: 'Projets en validation',
      value: this.managerProjects().filter(p => p.status === 'EN_VALIDATION').length
    },
    {
      title: 'Projets pré-validés',
      value: this.managerProjects().filter(p => p.status === 'PRE_VALIDE').length
    },
    {
      title: 'Projets en cours',
      value: this.managerProjects().filter(p => p.status === 'EN_COURS').length
    },
    {
      title: 'Projets rejetés',
      value: this.managerProjects().filter(p => p.status === 'REJETE').length
    }
  ]);

  managerRecentProjects = computed(() =>
    [...this.managerProjects()]
      .sort((a, b) => (this.parseDate(b.updatedAt)?.getTime() ?? 0) - (this.parseDate(a.updatedAt)?.getTime() ?? 0))
      .slice(0, 5)
  );

  getStatusLabel(status: string | undefined): string {
    switch (status) {
      case 'EN_VALIDATION': return 'En validation';
      case 'PRE_VALIDE': return 'Pré-validé';
      case 'EN_COURS': return 'En cours';
      case 'REJETE': return 'Rejeté';
      default: return status || 'Inconnu';
    }
  }

  getManagerProjectCount(status: string): number {
    return this.managerProjects().filter(p => p.status === status).length;
  }

  getManagerProjectPercentage(status: string): number {
    const total = this.managerProjects().length;
    if (total === 0) return 0;
    const count = this.getManagerProjectCount(status);
    return (count / total) * 100;
  }

  // ================= RESPONSABLE CONTRAT =================
  rcProjects = computed(() => this.projects());
    // ================= RESPONSABLE CONTRAT (version enrichie) =================

  // Alias pour faciliter l'accès dans le template
  get projets(): DashboardProject[] {
    return this.projects();
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
    const total = this.projets.length;
    if (total === 0) return 0;
    return Math.round((this.projetsPreValides / total) * 100);
  }

  get activeClientsCount(): number {
    const clients = new Set(this.projets.map(p => p.client).filter(Boolean));
    return clients.size;
  }

  get totalFactureHT(): number {
    // Calcul du CA estimé HT basé sur les TJM des équipes
    return this.projets.reduce((sum, project) => sum + this.estimateProjectRevenueHT(project), 0);
  }

  get totalFactureTTC(): number {
    return Math.round(this.totalFactureHT * 1.2);
  }

  get topClients(): any[] {
    const clientMap = new Map<string, { projectsCount: number; totalHT: number }>();
    const totalProjects = this.projets.length;

    for (const project of this.projets) {
      const client = (project.client || 'Non défini').trim() || 'Non défini';
      const current = clientMap.get(client) || { projectsCount: 0, totalHT: 0 };
      current.projectsCount++;
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

  get projectsByMonth(): { month: string; count: number; percentage: number }[] {
    const months = ['Jan', 'Fév', 'Mar', 'Avr', 'Mai', 'Juin', 'Juil', 'Août', 'Sep', 'Oct', 'Nov', 'Déc'];
    const monthCounts = new Array(12).fill(0);

    for (const project of this.projets) {
      if (!project.createdAt) continue;
      const date = new Date(project.createdAt);
      if (isNaN(date.getTime())) continue;
      monthCounts[date.getMonth()] += 1;
    }

    const total = monthCounts.reduce((sum, count) => sum + count, 0);

    return months.map((month, index) => ({
      month,
      count: monthCounts[index],
      percentage: total > 0 ? (monthCounts[index] * 100) / total : 0
    }));
  }

  get profileDistribution(): { profile: string; count: number; percentage: number; color: string }[] {
    const members = this.allTeams().flatMap(team => team.members || []);
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

  

  get currentYear(): number {
    return new Date().getFullYear();
  }

  // Méthodes de calcul du CA estimé
  private estimateProjectRevenueHT(project: DashboardProject): number {
    const team = this.allTeams().find(t => t.id === project.teamId);
    const members = team?.members || [];
    const teamDailyRate = members.reduce((sum, member) => sum + (member.tjm || 0), 0);

    if (teamDailyRate === 0) return 0;

    const businessDays = this.getBusinessDaysBetween(
      project.startDate instanceof Date ? project.startDate.toISOString() : project.startDate,
      project.endDate instanceof Date ? project.endDate.toISOString() : project.endDate
    );
    const progressRatio = Math.min(Math.max((project.progressPercentage || 0) / 100, 0), 1);
    const effectiveDays = Math.max(1, Math.round(businessDays * progressRatio));

    return teamDailyRate * effectiveDays;
  }

  private getBusinessDaysBetween(startDate?: string | Date, endDate?: string | Date): number {
    if (!startDate || !endDate) return 1;
    const start = new Date(startDate);
    const end = new Date(endDate);
    if (isNaN(start.getTime()) || isNaN(end.getTime()) || start > end) return 1;

    let count = 0;
    const current = new Date(start);
    while (current <= end) {
      const day = current.getDay();
      if (day !== 0 && day !== 6) count++;
      current.setDate(current.getDate() + 1);
    }
    return Math.max(count, 1);
  }

  // Couleur pour un profil (identique à reporting)
  getColorForProfile(profile: string): string {
    const colors = ['#7c3aed', '#3b82f6', '#10b981', '#f59e0b', '#ef4444', '#ec4899', '#06b6d4', '#84cc16'];
    let hash = 0;
    for (let i = 0; i < profile.length; i++) {
      hash = profile.charCodeAt(i) + ((hash << 5) - hash);
    }
    return colors[Math.abs(hash) % colors.length];
  }

  // Utilitaire pour le template : avatar par index
  getAvatarColorIndex(index: number): string {
    const colors = ['#7c3aed', '#3b82f6', '#10b981', '#f59e0b', '#ef4444'];
    return colors[index % colors.length];
  }

  // Export PDF pour Responsable Contrat (optionnel)
  exportResponsableContratToPdf(): void {
    console.log('🔵 Export PDF Responsable Contrat appelé');
    if (!this.isResponsableContract()) return;
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
        this.showToast('PDF exporté avec succès !', 'success');
      },
      error: (err) => {
        console.error('❌ Erreur export PDF:', err);
        this.showToast('Erreur lors de l\'export PDF', 'error');
      }
    });
  }

  rcKpis = computed(() => [
    {
      title: 'Projets créés',
      value: this.rcProjects().length
    },
    {
      title: 'En validation',
      value: this.rcProjects().filter(p => p.status === 'EN_VALIDATION').length
    },
    {
      title: 'Pré-validés',
      value: this.rcProjects().filter(p => p.status === 'PRE_VALIDE').length
    },
    {
      title: 'Clients actifs',
      value: new Set(this.rcProjects().map(p => p.client).filter(Boolean)).size
    }
  ]);

  rcRecentProjects = computed(() =>
    [...this.rcProjects()]
      .sort((a, b) => (this.parseDate(b.createdAt)?.getTime() ?? 0) - (this.parseDate(a.createdAt)?.getTime() ?? 0))
      .slice(0, 5)
  );

  getRCProjectCount(status: string): number {
    return this.rcProjects().filter(p => p.status === status).length;
  }

  getRCProjectPercentage(status: string): number {
    const total = this.rcProjects().length;
    if (total === 0) return 0;
    const count = this.getRCProjectCount(status);
    return (count / total) * 100;
  }

  // ================= MEMBRE EQUIPE =================
    // ================= MEMBRE EQUIPE (version enrichie) =================
  // Statistiques personnelles (basées sur myTasks)
  get totalMyTasks(): number {
    return this.myTasks().length;
  }

  get myTasksCompleted(): number {
    return this.myTasks().filter(t => t.status === 'Terminé').length;
  }

  get myTasksInProgress(): number {
    return this.myTasks().filter(t => t.status === 'En_cours').length;
  }

  get myTasksLate(): number {
    return this.myTasks().filter(t => this.isTaskLate(t)).length;
  }

  get myTasksRemaining(): number {
    return this.totalMyTasks - this.myTasksCompleted;
  }

  get myCompletionRate(): number {
    if (this.totalMyTasks === 0) return 0;
    return Math.round((this.myTasksCompleted / this.totalMyTasks) * 100);
  }

  // Distribution par priorité
  get myHighPriorityCount(): number {
    return this.myTasks().filter(t => t.priority === 'HAUTE').length;
  }

  get myMediumPriorityCount(): number {
    return this.myTasks().filter(t => t.priority === 'MOYENNE').length;
  }

  get myLowPriorityCount(): number {
    return this.myTasks().filter(t => t.priority === 'BASSE').length;
  }

  get myHighPriorityPercent(): number {
    if (this.totalMyTasks === 0) return 0;
    return (this.myHighPriorityCount / this.totalMyTasks) * 100;
  }

  get myMediumPriorityPercent(): number {
    if (this.totalMyTasks === 0) return 0;
    return (this.myMediumPriorityCount / this.totalMyTasks) * 100;
  }

  get myLowPriorityPercent(): number {
    if (this.totalMyTasks === 0) return 0;
    return (this.myLowPriorityCount / this.totalMyTasks) * 100;
  }

  // Tâches en cours ou à faire (pour le tableau)
  get myCurrentTasks(): TaskDto[] {
    return this.myTasks().filter(t => t.status !== 'Terminé');
  }

  // Pagination pour le tableau des tâches
  memberTasksPage = 1;
  memberTasksItemsPerPage = 5;

  get paginatedMyCurrentTasks(): TaskDto[] {
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

  // Évolution hebdomadaire (simulée – vous pouvez la remplacer par des données réelles)
  get weeklyEvolution(): { week: string; total: number; completed: number }[] {
    const weeks = ['Sem 1', 'Sem 2', 'Sem 3', 'Sem 4'];
    // Simulation : à adapter selon vos données réelles (par ex. depuis le backend)
    return weeks.map(week => ({
      week,
      total: Math.floor(Math.random() * 10) + 5,
      completed: Math.floor(Math.random() * 8) + 2
    }));
  }

  // Formatage de date pour l'affichage dans le tableau
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

  // Libellé du statut d'une tâche
  getTaskStatusLabel(status?: string): string {
    switch (status) {
      case 'A_faire': return 'À faire';
      case 'En_cours': return 'En cours';
      case 'Terminé': return 'Terminé';
      case 'Validation': return 'Validation';
      case 'A_revoir': return 'À revoir';
      default: return status || '-';
    }
  }

  // Export PDF pour membre équipe (appelé par le bouton)
  exportMembreEquipeToPdf(): void {
    console.log('🔵 Export PDF Membre Équipe appelé');
    if (!this.isMembreEquipe()) return;
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
        this.showToast('PDF exporté avec succès !', 'success');
      },
      error: (err) => {
        console.error('❌ Erreur export PDF:', err);
        this.showToast('Erreur lors de l\'export PDF', 'error');
      }
    });
  }
  myTasks = computed(() => {
    const currentUserId = this.currentUser()?.id;
    if (!currentUserId) return [];
    return this.tasks().filter(t => t.assignedToId === currentUserId);
  });

  myProjects = computed(() => {
    const ids = new Set(this.myTasks().map(t => t.projectId).filter(Boolean));
    return this.projects().filter(p => p.id != null && ids.has(p.id));
  });

  membreKpis = computed(() => [
    {
      title: 'Mes tâches',
      value: this.myTasks().length
    },
    {
      title: 'En cours',
      value: this.myTasks().filter(t => t.status === 'En_cours').length
    },
    {
      title: 'À valider',
      value: this.myTasks().filter(t => t.status === 'Validation').length
    },
    {
      title: 'En retard',
      value: this.myTasks().filter(t => this.isTaskLate(t)).length
    }
  ]);

  myRecentTasks = computed(() =>
    [...this.myTasks()]
  .filter(task => task.priority === 'HAUTE')
      .sort((a, b) => {
        const priorityOrder = { 'HAUTE': 0, 'MOYENNE': 1, 'BASSE': 2 };
        const aPriority = priorityOrder[a.priority as keyof typeof priorityOrder] ?? 1;
        const bPriority = priorityOrder[b.priority as keyof typeof priorityOrder] ?? 1;
        if (aPriority !== bPriority) return aPriority - bPriority;
        const aDate = this.parseDate(a.estimatedEndDate)?.getTime() ?? Number.MAX_SAFE_INTEGER;
        const bDate = this.parseDate(b.estimatedEndDate)?.getTime() ?? Number.MAX_SAFE_INTEGER;
        return aDate - bDate;
      })
      .slice(0, 5)
  );

  getPriorityLabel(priority?: string): string {
    switch (priority) {
      case 'HAUTE': return 'Haute';
      case 'MOYENNE': return 'Moyenne';
      case 'BASSE': return 'Basse';
      default: return 'Moyenne';
    }
  }

  getProjectStatusLabel(status?: string): string {
    switch (status) {
      case 'EN_COURS': return 'En cours';
      case 'PRE_VALIDE': return 'Pré-validé';
      case 'EN_VALIDATION': return 'En validation';
      case 'REJETE': return 'Rejeté';
      default: return status || 'Inconnu';
    }
  }

  formatDate(dateStr?: string): string {
    if (!dateStr) return 'Non définie';
    const date = new Date(dateStr);
    if (isNaN(date.getTime())) return 'Non définie';
    return date.toLocaleDateString('fr-FR', { day: '2-digit', month: 'short' });
  }


// ================= AI PREDICTION METHODS =================

private analysisQueue: number[] = [];
private isAnalysing = false;

// "Actualiser" → lit le cache pour tous les projets (rapide, pas de 429)
loadAllChefProjectRisks(): void {
  const enCoursProjects = this.chefProjects.filter(p => p.status === 'EN_COURS');
  enCoursProjects.forEach((project, index) => {
    // Délai léger juste pour ne pas saturer l'UI
    setTimeout(() => this.loadRiskForProject(project.id), index * 300);
  });
}

// Bouton "Analyser" sur un projet → force le refresh IA
forceRefreshProject(projectId: number): void {
  if (this.isRiskLoading(projectId)) return;
  this.riskLoading.set(new Map([...this.riskLoading(), [projectId, true]]));

  this.predictionService.forceRefreshRisk(projectId).subscribe({
    next: (result) => {
      this.projectRisks.set(new Map([...this.projectRisks(), [projectId, result]]));
      this.riskLoading.set(new Map([...this.riskLoading(), [projectId, false]]));
      if (result.level === 'ELEVE') {
        this.loadRecommendationForProject(projectId);
      }
    },
    error: (err) => {
      console.error('Erreur force refresh:', err);
      this.riskLoading.set(new Map([...this.riskLoading(), [projectId, false]]));
    }
  });
}

private processNextInQueue(): void {
  if (this.isAnalysing || this.analysisQueue.length === 0) return;

  const projectId = this.analysisQueue.shift()!;
  this.isAnalysing = true;
  this.analyseOneProject(projectId);
}

private analyseOneProject(projectId: number): void {
  this.riskLoading.set(new Map([...this.riskLoading(), [projectId, true]]));

  this.predictionService.getProjectRisk(projectId).subscribe({
    next: (result) => {
      this.projectRisks.set(new Map([...this.projectRisks(), [projectId, result]]));
      this.riskLoading.set(new Map([...this.riskLoading(), [projectId, false]]));

      // ✅ NE PAS charger les recommandations automatiquement
      // Elles seront chargées à la demande (bouton "Voir recommandations")

      this.isAnalysing = false;
      setTimeout(() => this.processNextInQueue(), 4000);
    },
    error: (err) => {
      console.error(`Erreur prédiction projet ${projectId}:`, err);
      this.riskLoading.set(new Map([...this.riskLoading(), [projectId, false]]));
      this.isAnalysing = false;

      if (err.status === 503 || err.status === 429) {
        this.analysisQueue.unshift(projectId);
        setTimeout(() => this.processNextInQueue(), 10000);
      } else {
        setTimeout(() => this.processNextInQueue(), 2000);
      }
    }
  });
}

// Bouton "Analyser" d'un seul projet
loadRiskForProject(projectId: number): void {
  if (this.riskLoading().get(projectId)) return;

  // Si déjà une analyse en cours, mettre en queue
  if (this.isAnalysing) {
    if (!this.analysisQueue.includes(projectId)) {
      this.analysisQueue.push(projectId);
      console.log(`📋 Projet ${projectId} mis en file d'attente`);
    }
    return;
  }

  this.isAnalysing = true;
  this.analyseOneProject(projectId);
}

recommendationLoading = signal<Map<number, boolean>>(new Map());

loadRecommendationForProject(projectId: number): void {
  if (this.recommendationLoading().get(projectId)) return;

  this.recommendationLoading.set(
    new Map([...this.recommendationLoading(), [projectId, true]]));

  this.predictionService.getTeamRecommendation(projectId).subscribe({
    next: (result) => {
      this.projectRecommendations.set(
        new Map([...this.projectRecommendations(), [projectId, result]]));
      this.recommendationLoading.set(
        new Map([...this.recommendationLoading(), [projectId, false]]));
    },
    error: (err) => {
      console.error(`Erreur recommandation projet ${projectId}:`, err);
      this.recommendationLoading.set(
        new Map([...this.recommendationLoading(), [projectId, false]]));
    }
  });
}

isRecommendationLoading(projectId: number): boolean {
  return this.recommendationLoading().get(projectId) ?? false;
}

getRiskForProject(projectId: number): RiskPredictionResult | null {
  return this.projectRisks().get(projectId) ?? null;
}

getRecommendationForProject(projectId: number): TeamRecommendationResult | null {
  return this.projectRecommendations().get(projectId) ?? null;
}

isRiskLoading(projectId: number): boolean {
  return this.riskLoading().get(projectId) ?? false;
}

getRiskLevelLabel(level: string): string {
  return { 'FAIBLE': 'Faible', 'MOYEN': 'Moyen', 'ELEVE': 'Élevé' }[level] ?? level;
}

getRiskLevelClass(level: string): string {
  return { 'FAIBLE': 'risk-faible', 'MOYEN': 'risk-moyen', 'ELEVE': 'risk-eleve' }[level] ?? '';
}

getRiskScorePercent(score: number): number {
  return Math.round(score * 100);
}

// la déclaration de projectRisks signal
chefHighRiskProjectsCount = computed(() => {
  let count = 0;
  this.projectRisks().forEach(r => { if (r.level === 'ELEVE') count++; });
  return count;
});

chefMediumRiskProjectsCount = computed(() => {
  let count = 0;
  this.projectRisks().forEach(r => { if (r.level === 'MOYEN') count++; });
  return count;
});
}
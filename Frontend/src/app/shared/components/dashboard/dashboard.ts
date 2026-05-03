import { Component, OnInit, inject, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { forkJoin, of, Observable } from 'rxjs';
import { catchError, map, switchMap } from 'rxjs/operators';

import { AdminService, PageResponse, UserResponse } from '../../../core/services/admin.service';
import { AuthService } from '../../../core/services/auth.service';
import { ProjectService, ProjectDto } from '../../../core/services/project.service';
import { TaskService, TaskDto } from '../../../core/services/task.service';
import { RouterModule } from '@angular/router';
import { ManagerService, ManagerProjectItemDto } from '../../../core/services/manager.service';

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
}

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule, RouterModule],
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

  private readonly teamsApiUrl = 'http://localhost:8080/api/teams';

  currentUser = signal<any>(null);
  roles = signal<string[]>([]);
  loading = signal(false);

  // ================= ADMIN =================
  users = signal<UserResponse[]>([]);
  totalUsers = signal(0);
  activeUsers = signal(0);
  inactiveUsers = signal(0);

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
    this.roles.set((user?.roles || []).map((r: any) => r.nom || r));

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
    this.adminService.getUsers(1, 100).subscribe({
      next: (response: PageResponse<UserResponse>) => {
        this.users.set(response.content);
        this.totalUsers.set(response.totalElements);
        const active = response.content.filter(u => !u.locked).length;
        const inactive = response.content.filter(u => u.locked).length;
        this.activeUsers.set(active);
        this.inactiveUsers.set(inactive);
        this.loading.set(false);
      },
      error: (err) => {
        console.error('Erreur lors du chargement des utilisateurs:', err);
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
          this.tasks.set(tasks ?? []);
          this.loading.set(false);
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

  private isTaskLate(task: TaskDto): boolean {
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
      .filter(t => this.isTaskLate(t) || t.priority === 'HAUTE' || t.status === 'Validation')
      .slice(0, 5)
  );

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

  paginatedTeamPerformance = computed(() => {
    const start = (this.performanceCurrentPage - 1) * this.performanceItemsPerPage;
    const end = start + this.performanceItemsPerPage;
    return this.teamPerformance().slice(start, end);
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
}
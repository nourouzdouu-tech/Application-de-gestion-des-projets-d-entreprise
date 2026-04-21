import { Component, OnInit, inject, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { forkJoin, of, Observable } from 'rxjs';
import { catchError, map, switchMap } from 'rxjs/operators';

import { AdminService, PageResponse, UserResponse } from '../../../core/services/admin.service';
import { AuthService } from '../../../core/services/auth.service';
import { ProjectService, ProjectDto } from '../../../core/services/project.service';
import { TaskService, TaskDto } from '../../../core/services/task.service';

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

interface DashboardProject extends ProjectDto {
  managerId?: number;
  managerName?: string;
  chefProjetId?: number;
  chefProjetName?: string;
  createdById?: number;
  createdByName?: string;
}

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './dashboard.html',
  styleUrl: './dashboard.css'
})
export class Dashboard implements OnInit {
  private adminService = inject(AdminService);
  private authService = inject(AuthService);
  private projectService = inject(ProjectService);
  private taskService = inject(TaskService);
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
  isResponsableContract = computed(() => this.roles().includes('RESPONSABLE_CONTRACT'));

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
          return of([] as DashboardProject[]);
        })
      ),
      teams: this.http.get<TeamDto[]>(this.teamsApiUrl).pipe(
        catchError((err) => {
          console.error('Erreur équipes chef projet:', err);
          return of([] as TeamDto[]);
        })
      )
    })
      .pipe(
        switchMap(({ projects, teams }) => {
          this.projects.set(projects ?? []);
          this.allTeams.set(teams ?? []);

          const validProjects = (projects ?? []).filter(p => p.id != null);
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
    forkJoin({
      projects: this.projectService.getAllProjects().pipe(
        catchError((err) => {
          console.error('Erreur projets RC:', err);
          return of([] as DashboardProject[]);
        })
      ),
      teams: this.http.get<TeamDto[]>(this.teamsApiUrl).pipe(
        catchError((err) => {
          console.error('Erreur équipes RC:', err);
          return of([] as TeamDto[]);
        })
      )
    }).subscribe({
      next: ({ projects, teams }) => {
        this.projects.set(projects ?? []);
        this.allTeams.set(teams ?? []);
        this.tasks.set([]);
        this.loading.set(false);
      },
      error: (err) => {
        console.error('Erreur dashboard RC:', err);
        this.loading.set(false);
      }
    });
  }

  loadManagerDashboardData() {
    forkJoin({
      projects: this.projectService.getAllProjects().pipe(
        catchError((err) => {
          console.error('Erreur projets manager:', err);
          return of([] as DashboardProject[]);
        })
      ),
      teams: this.http.get<TeamDto[]>(this.teamsApiUrl).pipe(
        catchError((err) => {
          console.error('Erreur équipes manager:', err);
          return of([] as TeamDto[]);
        })
      )
    }).subscribe({
      next: ({ projects, teams }) => {
        this.projects.set(projects ?? []);
        this.allTeams.set(teams ?? []);
        this.tasks.set([]);
        this.loading.set(false);
      },
      error: (err) => {
        console.error('Erreur dashboard manager:', err);
        this.loading.set(false);
      }
    });
  }

  loadMembreEquipeDashboardData() {
    forkJoin({
      projects: this.projectService.getAllProjects().pipe(
        catchError((err) => {
          console.error('Erreur projets membre équipe:', err);
          return of([] as DashboardProject[]);
        })
      ),
      teams: this.http.get<TeamDto[]>(this.teamsApiUrl).pipe(
        catchError((err) => {
          console.error('Erreur équipes membre équipe:', err);
          return of([] as TeamDto[]);
        })
      )
    })
      .pipe(
        switchMap(({ projects, teams }) => {
          this.projects.set(projects ?? []);
          this.allTeams.set(teams ?? []);

          const validProjects = (projects ?? []).filter(p => p.id != null);
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
          console.error('Erreur dashboard membre équipe:', err);
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
    { title: 'Projets en cours', value: this.projects().filter(p => p.status === 'EN_COURS').length },
    { title: 'Tâches urgentes', value: this.tasks().filter(t => this.isTaskLate(t) || t.priority === 'HAUTE').length },
    { title: 'Tâches à valider', value: this.tasks().filter(t => t.status === 'Validation').length },
    { title: 'Projets sans équipe', value: this.projects().filter(p => p.teamId == null).length }
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
      .sort((a, b) => (this.parseDate(a.estimatedEndDate)?.getTime() ?? Number.MAX_SAFE_INTEGER) -
       (this.parseDate(b.estimatedEndDate)?.getTime() ?? Number.MAX_SAFE_INTEGER))
      .slice(0, 5)
  );
}
import { Component, OnInit, signal, WritableSignal, HostListener, inject, Input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClient, HttpParams } from '@angular/common/http';
import * as XLSX from 'xlsx';
import { AuthService } from '../../../core/services/auth.service';
import { Router, ActivatedRoute } from '@angular/router';

// ─── Interfaces Security ───────────────────────────────────
export interface AuditLog {
  id: number;
  timestamp: Date;
  userPrenom: string;
  userNom: string;
  userEmail: string;
  action: string;
  category: 'auth' | 'users' | 'security';
  details: string;
  ipAddress: string;
  level: 'INFO' | 'SUCCESS' | 'WARNING' | 'ERROR' | 'CRITICAL';
  success: boolean;
}

export interface AuditStats {
  loginsToday: number;
  loginsTrend: number;
  activeAccounts: number;
  failedLogins: number;
  lockedAccounts: number;
  passwordResets: number;
  adminActions: number;
}

// ─── Interfaces Workflow ───────────────────────────────────
export interface WorkflowLog {
  id: number;
  timestamp: Date;
  actorName: string;
  actorEmail: string;
  actorRole: string;
  phase: 'creation' | 'validation' | 'assignation' | 'gestion' | 'execution';
  action: string;
  projectName: string;
  details: string;
  statusType: 'success' | 'rejected' | 'pending';
}

export interface WorkflowStats {
  projetsCreated: number;
  enAttente: number;
  valides: number;
  rejetes: number;
  chefsAssigned: number;
  tachesCreated: number;
}

interface AuditLogDto {
  id: number;
  action: string;
  entityType: string;
  entityId: number;
  details: string;
  performedBy: string;
  performedAt: string;
  ipAddress: string;
}

interface BackendPage {
  content: AuditLogDto[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
}

const PHASE_MAP: Record<string, WorkflowLog['phase']> = {
  CREATE_PROJECT: 'creation',
  UPDATE_PROJECT: 'creation',
  VALIDATE_PROJECT: 'validation',
  REJECT_PROJECT: 'validation',
  ASSIGN_CHEF: 'assignation',
  CREATE_TEAM: 'gestion',
  UPDATE_TEAM: 'gestion',
  DELETE_TEAM: 'gestion',
  ASSIGN_MEMBER_TO_TEAM: 'gestion',
  REMOVE_MEMBER_FROM_TEAM: 'gestion',
  ASSIGN_TEAM_TO_PROJECT: 'gestion',
  CREATE_TASK: 'gestion',
  UPDATE_TASK: 'gestion',
  DELETE_TASK: 'gestion',
  VALIDATE_TASK: 'execution',
  REJECT_TASK: 'execution',
  UPDATE_TASK_STATUS: 'execution',
  SUBMIT_TASK: 'execution',
};

const ROLE_MAP: Record<string, string> = {
  CREATE_PROJECT: 'RESPONSABLE_CONTRAT',
  UPDATE_PROJECT: 'RESPONSABLE_CONTRAT',
  VALIDATE_PROJECT: 'MANAGER',
  REJECT_PROJECT: 'MANAGER',
  ASSIGN_CHEF: 'MANAGER',
  CREATE_TEAM: 'CHEF_PROJET',
  UPDATE_TEAM: 'CHEF_PROJET',
  DELETE_TEAM: 'CHEF_PROJET',
  ASSIGN_MEMBER_TO_TEAM: 'CHEF_PROJET',
  REMOVE_MEMBER_FROM_TEAM: 'CHEF_PROJET',
  ASSIGN_TEAM_TO_PROJECT: 'CHEF_PROJET',
  CREATE_TASK: 'CHEF_PROJET',
  UPDATE_TASK: 'CHEF_PROJET',
  DELETE_TASK: 'CHEF_PROJET',
  VALIDATE_TASK: 'CHEF_PROJET',
  REJECT_TASK: 'CHEF_PROJET',
  UPDATE_TASK_STATUS: 'MEMBRE_EQUIPE',
  SUBMIT_TASK: 'MEMBRE_EQUIPE',
};

@Component({
  selector: 'app-audit',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './audit.html',
  styleUrls: ['./audit.css']
})
export class AuditComponent implements OnInit {
  // Type d'audit actuel (security ou workflow)
  currentAuditType: 'security' | 'workflow' = 'security';

  // ─── State Security ────────────────────────────────
  loading: WritableSignal<boolean> = signal(false);
  allLogs: AuditLog[] = [];
  filteredLogs: AuditLog[] = [];
  selectedLog: AuditLog | WorkflowLog | null = null;
  dropdownOpen: boolean = false;

  // Filters Security
  searchTerm = '';
  filterAction = '';
  filterLevel = '';
  filterDateFrom = '';
  filterDateTo = '';
  activeTab: 'all' | 'auth' | 'users' | 'security' = 'all';

  // Pagination Security
  currentPage = 1;
  itemsPerPage = 15;

  // Stats Security
  stats: AuditStats = {
    loginsToday: 0,
    loginsTrend: 0,
    activeAccounts: 0,
    failedLogins: 0,
    lockedAccounts: 0,
    passwordResets: 0,
    adminActions: 0
  };

  // ─── State Workflow ────────────────────────────────
  allWorkflowLogs: WorkflowLog[] = [];
  filteredWorkflowLogs: WorkflowLog[] = [];

  // Filters Workflow
  workflowSearchTerm = '';
  filterPhase = '';
  filterRole = '';
  filterStatus = '';
  filterWorkflowDateFrom = '';
  filterWorkflowDateTo = '';
  workflowActiveTab: WorkflowLog['phase'] | 'all' = 'all';

  workflowCurrentPage = 1;
  workflowItemsPerPage = 12;

  workflowStats: WorkflowStats = {
    projetsCreated: 0, enAttente: 0, valides: 0,
    rejetes: 0, chefsAssigned: 0, tachesCreated: 0
  };

  // ─── Shared ────────────────────────────────────────
  showToast = false;
  toastMessage = '';
  toastType: 'success' | 'error' = 'success';

  private colors = [
    '#7c3aed', '#2563eb', '#10b981', '#f59e0b',
    '#ef4444', '#8b5cf6', '#06b6d4', '#ec4899'
  ];
  private authService = inject(AuthService);
  get hasAdminRole(): boolean {
  return this.authService.getRoles().includes('ADMIN');
}

get hasManagerRole(): boolean {
  return this.authService.getRoles().includes('MANAGER');
}

get hasBothRoles(): boolean {
  return this.hasAdminRole && this.hasManagerRole;
}
  private router = inject(Router);
  private route = inject(ActivatedRoute);

  constructor(private http: HttpClient) {}
ngOnInit(): void {
  const roles = this.authService.getRoles();
  const isAdmin = roles.includes('ADMIN');
  const isManager = roles.includes('MANAGER');

  if (isAdmin && !isManager) {
    this.currentAuditType = 'security';
  } else if (isManager && !isAdmin) {
    this.currentAuditType = 'workflow';
  }
  // Si les deux : on garde 'security' par défaut, les boutons seront visibles

  this.loadAll();
}

  // Changer le type d'audit
  setAuditType(type: 'security' | 'workflow'): void {
    this.currentAuditType = type;
    this.loadAll();
  }

  // ══════════════════════════════════════════════════
  // DROPDOWN EXPORT
  // ══════════════════════════════════════════════════

  toggleDropdown(event: Event): void {
    event.stopPropagation();
    this.dropdownOpen = !this.dropdownOpen;
  }

  closeDropdown(): void {
    this.dropdownOpen = false;
  }

  @HostListener('document:click')
  onDocumentClick(): void {
    this.dropdownOpen = false;
  }

  // ══════════════════════════════════════════════════
  // DATA LOADING
  // ══════════════════════════════════════════════════

  loadAll(): void {
    if (this.currentAuditType === 'security') {
      this.loadSecurityAudit();
    } else {
      this.loadWorkflowAudit();
    }
  }

  private loadSecurityAudit(): void {
    this.loading.set(true);
    const params = new HttpParams().set('page', '0').set('size', '100');

    this.http.get<any>('http://localhost:8080/api/admin/audit', { params }).subscribe({
      next: (response) => {
        const logs = response.content.map((l: any) => ({
          id: l.id,
          timestamp: new Date(l.performedAt),
          userPrenom: l.performedBy.split('@')[0],
          userNom: '',
          userEmail: l.performedBy,
          action: l.action,
          category: this.getCategoryFromAction(l.action),
          details: this.cleanDetails(l.details),
          ipAddress: l.ipAddress || '-',
          level: this.getLevelFromAction(l.action),
          success: this.isSuccessAction(l.action)
        }));
        this.allLogs = logs;
        this.computeStats();
        this.applyFilters();
        this.loading.set(false);
      },
      error: (err) => {
        console.error('Erreur chargement audit', err);
        this.allLogs = [];
        this.computeStats();
        this.applyFilters();
        this.loading.set(false);
        this.triggerToast('Erreur de chargement des logs d\'audit', 'error');
      }
    });
  }

  private loadWorkflowAudit(): void {
    this.loading.set(true);
    const params = new HttpParams().set('page', '0').set('size', '500');

    this.http.get<BackendPage>('http://localhost:8080/api/manager/audit', { params }).subscribe({
      next: (response) => {
        const raw: AuditLogDto[] = response?.content ?? (Array.isArray(response) ? response as any : []);
        this.allWorkflowLogs = this.mapToWorkflowViewModel(raw);
        this.computeWorkflowStats();
        this.applyWorkflowFilters();
        this.loading.set(false);
      },
      error: () => {
        this.allWorkflowLogs = this.generateMockWorkflowLogs();
        this.computeWorkflowStats();
        this.applyWorkflowFilters();
        this.loading.set(false);
      }
    });
  }

  private getCategoryFromAction(action: string): 'auth' | 'users' | 'security' {
    if (action === 'LOGIN_SUCCESS' || action === 'LOGIN_FAILED' || action === 'LOGOUT') {
      return 'auth';
    }
    if (action === 'CREATE_USER' || action === 'UPDATE_USER' || action === 'DELETE_USER') {
      return 'users';
    }
    if (action === 'ACCOUNT_LOCKED' || action === 'ACCOUNT_UNLOCKED' || action === 'DISABLE_USER') {
      return 'security';
    }
    return 'users';
  }

  private getLevelFromAction(action: string): 'INFO' | 'SUCCESS' | 'WARNING' | 'ERROR' | 'CRITICAL' {
    if (action === 'ACCOUNT_UNLOCKED') return 'SUCCESS';
    if (action === 'LOGIN_FAILED') return 'WARNING';
    if (action === 'DISABLE_USER') return 'WARNING';
    if (action === 'DELETE_USER') return 'WARNING';
    if (action === 'ACCOUNT_LOCKED') return 'CRITICAL';
    return 'INFO';
  }

  private isSuccessAction(action: string): boolean {
    const successActions = [
      'LOGIN_SUCCESS', 'CREATE_USER', 'UPDATE_USER', 'ENABLE_USER',
      'RESET_PASSWORD', 'CREATE_ROLE', 'UPDATE_ROLE', 'ACTIVATE_ROLE',
      'CREATE_PROJECT', 'UPDATE_PROJECT', 'CREATE_TASK', 'UPDATE_TASK',
      'ACCOUNT_UNLOCKED'
    ];
    return successActions.includes(action);
  }

  private cleanDetails(details: string): string {
    if (!details) return '';
    return details.replace(/ depuis l'IP:\s*[0-9a-f:.]+/i, '');
  }

  private mapToWorkflowViewModel(raw: AuditLogDto[]): WorkflowLog[] {
    return raw
      .filter(l => PHASE_MAP[l.action])
      .map(l => ({
        id: l.id,
        timestamp: new Date(l.performedAt),
        actorName: this.formatActorName(l.performedBy),
        actorEmail: l.performedBy,
        actorRole: ROLE_MAP[l.action] ?? 'INCONNU',
        phase: PHASE_MAP[l.action],
        action: l.action,
        projectName: this.extractProjectName(l.details),
        details: l.details ?? '',
        statusType: l.action.includes('REJECT') ? 'rejected' : 'success'
      } as WorkflowLog))
      .sort((a, b) => b.timestamp.getTime() - a.timestamp.getTime());
  }

  private extractProjectName(details: string): string {
    if (!details) return '';
    const patterns = [
      { regex: /dans le projet\s+['"]?([^'"→\n]+?)['"]?/i },
      { regex: /dans\s+['"]?([A-Z][a-zA-Z0-9\s\-éèêëàâôûç]{2,50}?)['"]?(?:\s+[—–-]|\s*$|\.|,)/i },
      { regex: /projet\s+['"]?([^'"→\n]+?)['"]?\s*(?:→|$| avec| par)/i },
    ];
    for (const pattern of patterns) {
      const match = details.match(pattern.regex);
      if (match && match[1]) {
        let projectName = match[1].trim();
        projectName = projectName.replace(/[→'"`]/g, '');
        if (projectName.length > 2 && projectName.length < 80) {
          return projectName;
        }
      }
    }
    return '';
  }

  private formatActorName(email: string): string {
    if (!email) return 'Inconnu';
    return email.split('@')[0].split(/[._-]/)
      .map(p => p.charAt(0).toUpperCase() + p.slice(1)).join(' ');
  }

  private generateMockWorkflowLogs(): WorkflowLog[] {
    const actors = [
      { name: 'Fatima Zahra', email: 'fz@dxc.ma', role: 'RESPONSABLE_CONTRAT' },
      { name: 'Hanan Hafidi', email: 'hanan@dxc.ma', role: 'MANAGER' },
      { name: 'Omar Tazi', email: 'omar@dxc.ma', role: 'CHEF_PROJET' },
    ];
    const projects = ['Portail Client DXC', 'Refonte Infrastructure', 'App Mobile RH'];
    const events = ['CREATE_PROJECT', 'VALIDATE_PROJECT', 'ASSIGN_CHEF', 'CREATE_TASK'];
    
    const logs: WorkflowLog[] = [];
    const now = new Date();
    let id = 1;

    for (let i = 0; i < 20; i++) {
      const ev = events[Math.floor(Math.random() * events.length)];
      const proj = projects[Math.floor(Math.random() * projects.length)];
      const role = ROLE_MAP[ev] ?? 'RESPONSABLE_CONTRAT';
      const actor = actors.find(a => a.role === role) ?? actors[0];
      const ts = new Date(now);
      ts.setDate(ts.getDate() - i);
      
      logs.push({
        id: id++,
        timestamp: ts,
        actorName: actor.name,
        actorEmail: actor.email,
        actorRole: role,
        phase: PHASE_MAP[ev],
        action: ev,
        projectName: proj,
        details: `${ev} sur le projet ${proj}`,
        statusType: ev.includes('REJECT') ? 'rejected' : 'success'
      });
    }
    return logs.sort((a, b) => b.timestamp.getTime() - a.timestamp.getTime());
  }

  // ══════════════════════════════════════════════════
  // STATS SECURITY
  // ══════════════════════════════════════════════════

  computeStats(): void {
    const today = new Date();
    today.setHours(0, 0, 0, 0);

    const todayLogs = this.allLogs.filter(l => {
      const d = new Date(l.timestamp); d.setHours(0, 0, 0, 0);
      return d.getTime() === today.getTime();
    });

    const todayLogins = todayLogs.filter(l => l.action === 'LOGIN_SUCCESS').length;

    this.stats = {
      loginsToday: todayLogins,
      loginsTrend: 5,
      activeAccounts: todayLogs.filter(l => l.action === 'LOGIN_SUCCESS').length,
      failedLogins: this.allLogs.filter(l => l.action === 'LOGIN_FAILED').length,
      lockedAccounts: this.allLogs.filter(l => l.action === 'ACCOUNT_LOCKED').length,
      passwordResets: this.allLogs.filter(l => l.action === 'RESET_PASSWORD').length,
      adminActions: this.allLogs.filter(l => ['CREATE_USER', 'UPDATE_USER', 'DELETE_USER'].includes(l.action)).length,
    };
  }

  computeWorkflowStats(): void {
    this.workflowStats = {
      projetsCreated: this.allWorkflowLogs.filter(l => l.action === 'CREATE_PROJECT').length,
      enAttente: this.allWorkflowLogs.filter(l => l.phase === 'creation').length,
      valides: this.allWorkflowLogs.filter(l => l.action === 'VALIDATE_PROJECT').length,
      rejetes: this.allWorkflowLogs.filter(l => l.action === 'REJECT_PROJECT').length,
      chefsAssigned: this.allWorkflowLogs.filter(l => l.action === 'ASSIGN_CHEF').length,
      tachesCreated: this.allWorkflowLogs.filter(l => l.action === 'CREATE_TASK').length,
    };
  }

  // ══════════════════════════════════════════════════
  // FILTERS SECURITY
  // ══════════════════════════════════════════════════

  setTab(tab: 'all' | 'auth' | 'users' | 'security'): void {
    this.activeTab = tab;
    this.currentPage = 1;
    this.applyFilters();
  }

  applyFilters(): void {
    let logs = [...this.allLogs];

    if (this.activeTab !== 'all') {
      logs = logs.filter(l => l.category === this.activeTab);
    }

    if (this.searchTerm.trim()) {
      const s = this.searchTerm.toLowerCase();
      logs = logs.filter(l =>
        l.userPrenom.toLowerCase().includes(s) ||
        l.userEmail.toLowerCase().includes(s) ||
        l.details.toLowerCase().includes(s) ||
        l.ipAddress.includes(s)
      );
    }

    if (this.filterAction) {
      logs = logs.filter(l => l.action === this.filterAction);
    }

    if (this.filterLevel) {
      logs = logs.filter(l => l.level === this.filterLevel);
    }

    if (this.filterDateFrom) {
      const from = new Date(this.filterDateFrom);
      logs = logs.filter(l => new Date(l.timestamp) >= from);
    }

    if (this.filterDateTo) {
      const to = new Date(this.filterDateTo);
      to.setHours(23, 59, 59);
      logs = logs.filter(l => new Date(l.timestamp) <= to);
    }

    this.filteredLogs = logs;
    this.currentPage = 1;
  }

  clearFilters(): void {
    if (this.currentAuditType === 'security') {
      this.searchTerm = '';
      this.filterAction = '';
      this.filterLevel = '';
      this.filterDateFrom = '';
      this.filterDateTo = '';
      this.activeTab = 'all';
      this.applyFilters();
    } else {
      this.workflowSearchTerm = '';
      this.filterPhase = '';
      this.filterRole = '';
      this.filterStatus = '';
      this.filterWorkflowDateFrom = '';
      this.filterWorkflowDateTo = '';
      this.workflowActiveTab = 'all';
      this.applyWorkflowFilters();
    }
  }

  getCountByCategory(cat: string): number {
    return this.allLogs.filter(l => {
      if (cat === 'auth') return l.action === 'LOGIN_SUCCESS' || l.action === 'LOGIN_FAILED';
      if (cat === 'users') return l.action === 'CREATE_USER' || l.action === 'UPDATE_USER' || l.action === 'DELETE_USER';
      if (cat === 'security') return l.action === 'ACCOUNT_LOCKED' || l.action === 'DISABLE_USER';
      return false;
    }).length;
  }

  // ══════════════════════════════════════════════════
  // FILTERS WORKFLOW
  // ══════════════════════════════════════════════════

  setWorkflowTab(tab: typeof this.workflowActiveTab): void {
    this.workflowActiveTab = tab;
    this.workflowCurrentPage = 1;
    this.applyWorkflowFilters();
  }

  applyWorkflowFilters(): void {
    let logs = [...this.allWorkflowLogs];

    if (this.workflowActiveTab !== 'all') {
      logs = logs.filter(l => l.phase === this.workflowActiveTab);
    }

    if (this.workflowSearchTerm.trim()) {
      const s = this.workflowSearchTerm.toLowerCase();
      logs = logs.filter(l =>
        l.actorName.toLowerCase().includes(s) ||
        l.actorEmail.toLowerCase().includes(s) ||
        l.projectName.toLowerCase().includes(s) ||
        l.details.toLowerCase().includes(s)
      );
    }

    if (this.filterPhase) logs = logs.filter(l => l.phase === this.filterPhase);
    if (this.filterRole) logs = logs.filter(l => l.actorRole === this.filterRole);
    if (this.filterStatus === 'success') logs = logs.filter(l => l.statusType === 'success');
    if (this.filterStatus === 'rejected') logs = logs.filter(l => l.statusType === 'rejected');
    if (this.filterWorkflowDateFrom) {
      const from = new Date(this.filterWorkflowDateFrom);
      logs = logs.filter(l => new Date(l.timestamp) >= from);
    }
    if (this.filterWorkflowDateTo) {
      const to = new Date(this.filterWorkflowDateTo);
      to.setHours(23, 59, 59);
      logs = logs.filter(l => new Date(l.timestamp) <= to);
    }

    this.filteredWorkflowLogs = logs;
    this.workflowCurrentPage = 1;
  }

  getCountByPhase(phase: string): number {
    return this.allWorkflowLogs.filter(l => l.phase === phase).length;
  }

  // ══════════════════════════════════════════════════
  // PAGINATION SECURITY
  // ══════════════════════════════════════════════════

  get paginatedLogs(): AuditLog[] {
    const start = (this.currentPage - 1) * this.itemsPerPage;
    return this.filteredLogs.slice(start, start + this.itemsPerPage);
  }

  get totalPages(): number {
    return Math.ceil(this.filteredLogs.length / this.itemsPerPage);
  }

  setPage(page: number): void {
    if (page >= 1 && page <= this.totalPages) this.currentPage = page;
  }

  getPageNumbers(): number[] {
    const total = this.totalPages;
    const current = this.currentPage;
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

  getRangeStart(): number {
    return this.filteredLogs.length === 0 ? 0 : (this.currentPage - 1) * this.itemsPerPage + 1;
  }

  getRangeEnd(): number {
    return Math.min(this.currentPage * this.itemsPerPage, this.filteredLogs.length);
  }

  // ══════════════════════════════════════════════════
  // PAGINATION WORKFLOW
  // ══════════════════════════════════════════════════

  get paginatedWorkflowLogs(): WorkflowLog[] {
    const s = (this.workflowCurrentPage - 1) * this.workflowItemsPerPage;
    return this.filteredWorkflowLogs.slice(s, s + this.workflowItemsPerPage);
  }

  get workflowTotalPages(): number {
    return Math.max(1, Math.ceil(this.filteredWorkflowLogs.length / this.workflowItemsPerPage));
  }

  setWorkflowPage(p: number): void {
    if (p >= 1 && p <= this.workflowTotalPages) this.workflowCurrentPage = p;
  }

  getWorkflowPageNumbers(): number[] {
    const total = this.workflowTotalPages;
    const cur = this.workflowCurrentPage;
    const pages: number[] = [];
    if (total <= 7) {
      for (let i = 1; i <= total; i++) pages.push(i);
    } else if (cur <= 4) {
      pages.push(1, 2, 3, 4, 5, -1, total);
    } else if (cur >= total - 3) {
      pages.push(1, -1, total - 4, total - 3, total - 2, total - 1, total);
    } else {
      pages.push(1, -1, cur - 1, cur, cur + 1, -1, total);
    }
    return pages;
  }

  getWorkflowRangeStart(): number {
    return this.filteredWorkflowLogs.length === 0 ? 0 : (this.workflowCurrentPage - 1) * this.workflowItemsPerPage + 1;
  }

  getWorkflowRangeEnd(): number {
    return Math.min(this.workflowCurrentPage * this.workflowItemsPerPage, this.filteredWorkflowLogs.length);
  }

  // ══════════════════════════════════════════════════
  // LABEL HELPERS
  // ══════════════════════════════════════════════════

  getActionLabel(action: string): string {
    const m: Record<string, string> = {
      LOGIN_SUCCESS: 'Connexion réussie', LOGIN_FAILED: 'Connexion échouée',
      CREATE_USER: 'Utilisateur créé', UPDATE_USER: 'Utilisateur modifié',
      DELETE_USER: 'Utilisateur supprimé', ACCOUNT_LOCKED: 'Compte verrouillé',
      ACCOUNT_UNLOCKED: 'Compte déverrouillé'
    };
    return m[action] ?? action;
  }

  getWorkflowActionLabel(action: string): string {
    const m: Record<string, string> = {
      CREATE_PROJECT: 'Projet créé', VALIDATE_PROJECT: 'Projet validé',
      REJECT_PROJECT: 'Projet rejeté', ASSIGN_CHEF: 'Chef assigné',
      CREATE_TASK: 'Tâche créée', VALIDATE_TASK: 'Tâche validée'
    };
    return m[action] ?? action;
  }

  getActionClass(action: string): string {
    const m: Record<string, string> = {
      LOGIN_SUCCESS: 'action-login-success', LOGIN_FAILED: 'action-login-failed',
      CREATE_USER: 'action-user-create', CREATE_PROJECT: 'action-create-project',
      VALIDATE_PROJECT: 'action-validate', REJECT_PROJECT: 'action-reject'
    };
    return m[action] ?? 'action-default';
  }

  getLevelClass(level: string): string {
    return `level-${level}`;
  }

  getRoleLabel(role: string): string {
    const m: Record<string, string> = {
      RESPONSABLE_CONTRAT: 'Resp. Contrat', MANAGER: 'Manager',
      CHEF_PROJET: 'Chef Projet', MEMBRE_EQUIPE: 'Membre Équipe'
    };
    return m[role] ?? role;
  }

  getRoleClass(role: string): string {
    const m: Record<string, string> = {
      RESPONSABLE_CONTRAT: 'role-responsable', MANAGER: 'role-manager',
      CHEF_PROJET: 'role-chef', MEMBRE_EQUIPE: 'role-membre'
    };
    return m[role] ?? 'role-membre';
  }

  getPhaseLabel(phase: string): string {
    const m: Record<string, string> = {
      creation: 'Création', validation: 'Validation',
      assignation: 'Assignation', gestion: 'Gestion', execution: 'Exécution'
    };
    return m[phase] ?? phase;
  }

  getPhaseClass(phase: string): string {
    return `phase-${phase}`;
  }

  getStatusLabel(s: string): string {
    return s === 'success' ? 'Succès' : 'Rejeté';
  }

  getStatusClass(s: string): string {
    return s === 'success' ? 'status-ok' : 'status-rejected';
  }

  getInitials(prenom: string, nom: string): string {
    return ((prenom?.charAt(0) || '') + (nom?.charAt(0) || '')).toUpperCase();
  }

  getWorkflowInitials(name: string): string {
    return (name ?? '?').trim().split(' ').map(p => p.charAt(0)).slice(0, 2).join('').toUpperCase();
  }

  getAvatarColor(index: number): string {
    return this.colors[index % this.colors.length];
  }

  buildCode(name: string): string {
    if (!name) return 'PR';
    return name.split(' ').filter(Boolean).slice(0, 2).map(w => w.charAt(0).toUpperCase()).join('') || 'PR';
  }

  // ══════════════════════════════════════════════════
  // MODAL
  // ══════════════════════════════════════════════════

  openDetail(log: AuditLog | WorkflowLog): void {
    this.selectedLog = log;
    document.body.style.overflow = 'hidden';
  }

  closeDetail(): void {
    this.selectedLog = null;
    document.body.style.overflow = '';
  }

  getDetailIconClass(categoryOrPhase: string): string {
    return `icon-modal-${categoryOrPhase}`;
  }

  // ══════════════════════════════════════════════════
  // EXPORT
  // ══════════════════════════════════════════════════

  exportLogs(format: 'csv' | 'excel'): void {
    let headers: string[];
    let rows: any[][];

    if (this.currentAuditType === 'security') {
      headers = ['Date', 'Heure', 'Utilisateur', 'Email', 'Action', 'Détails', 'IP', 'Niveau', 'Statut'];
      rows = this.filteredLogs.map(l => [
        new Date(l.timestamp).toLocaleDateString('fr-FR'),
        new Date(l.timestamp).toLocaleTimeString('fr-FR'),
        `${l.userPrenom} ${l.userNom}`,
        l.userEmail,
        this.getActionLabel(l.action),
        l.details,
        l.ipAddress,
        l.level,
        l.success ? 'Succès' : 'Échec'
      ]);
    } else {
      headers = ['Date', 'Heure', 'Acteur', 'Email', 'Rôle', 'Phase', 'Action', 'Projet', 'Détails', 'Statut'];
      rows = this.filteredWorkflowLogs.map(l => [
        new Date(l.timestamp).toLocaleDateString('fr-FR'),
        new Date(l.timestamp).toLocaleTimeString('fr-FR'),
        l.actorName,
        l.actorEmail,
        this.getRoleLabel(l.actorRole),
        this.getPhaseLabel(l.phase),
        this.getWorkflowActionLabel(l.action),
        l.projectName || '-',
        l.details,
        this.getStatusLabel(l.statusType)
      ]);
    }

    const data = [headers, ...rows];
    const fileName = `audit_${this.currentAuditType}_${new Date().toISOString().split('T')[0]}`;

    if (format === 'excel') {
      const ws = XLSX.utils.aoa_to_sheet(data);
      const wb = XLSX.utils.book_new();
      XLSX.utils.book_append_sheet(wb, ws, 'Audit_Logs');
      XLSX.writeFile(wb, `${fileName}.xlsx`);
      this.triggerToast(`Export ${format.toUpperCase()} téléchargé`, 'success');
    } else {
      const csv = data.map(row =>
        row.map(cell => `"${String(cell).replace(/"/g, '""')}"`).join(';')
      ).join('\n');
      const blob = new Blob(['\uFEFF' + csv], { type: 'text/csv;charset=utf-8;' });
      const url = URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = `${fileName}.csv`;
      a.click();
      URL.revokeObjectURL(url);
      this.triggerToast(`Export ${format.toUpperCase()} téléchargé`, 'success');
    }
    this.closeDropdown();
  }

  // ══════════════════════════════════════════════════
  // TOAST & NAVIGATION
  // ══════════════════════════════════════════════════

  triggerToast(msg: string, type: 'success' | 'error'): void {
    this.toastMessage = msg;
    this.toastType = type;
    this.showToast = true;
    setTimeout(() => {
      this.showToast = false;
    }, 3000);
  }

  goToDashboard(): void {
    const roles = this.authService.getRoles();
    if (roles.includes('ADMIN')) {
      this.router.navigate(['/admin/dashboard']);
    } else if (roles.includes('MANAGER')) {
      this.router.navigate(['/manager/dashboard']);
    } else if (roles.includes('CHEF_PROJET')) {
      this.router.navigate(['/chef-projet/dashboard']);
    } else if (roles.includes('RESPONSABLE_CONTRAT')) {
      this.router.navigate(['/responsable-contrat/dashboard']);
    } else if (roles.includes('MEMBRE_EQUIPE')) {
      this.router.navigate(['/membre-equipe/dashboard']);
    } else {
      this.router.navigate(['/login']);
    }
  }
}
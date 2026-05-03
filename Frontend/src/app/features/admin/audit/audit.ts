import { Component, OnInit, signal, WritableSignal, HostListener, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClient, HttpParams } from '@angular/common/http';
import * as XLSX from 'xlsx';
import { AuthService } from '../../../core/services/auth.service'; 
import { Router } from '@angular/router'; 

// ─── Interfaces ───────────────────────────────────
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

@Component({
  selector: 'app-audit',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './audit.html',
  styleUrl: './audit.css',
})
export class AuditComponent implements OnInit {

  // ─── State ────────────────────────────────────────
  loading: WritableSignal<boolean> = signal(false);
  allLogs: AuditLog[] = [];
  filteredLogs: AuditLog[] = [];
  selectedLog: AuditLog | null = null;
  dropdownOpen: boolean = false;

  // Filters
  searchTerm = '';
  filterAction = '';
  filterLevel = '';
  filterDateFrom = '';
  filterDateTo = '';
  activeTab: 'all' | 'auth' | 'users' | 'security' = 'all';

  // Pagination
  currentPage = 1;
  itemsPerPage = 15;

  // Stats
  stats: AuditStats = {
    loginsToday: 0,
    loginsTrend: 0,
    activeAccounts: 0,
    failedLogins: 0,
    lockedAccounts: 0,
    passwordResets: 0,
    adminActions: 0
  };

  // Toast
  showToast = false;
  toastMessage = '';
  toastType: 'success' | 'error' = 'success';

  private colors = [
    '#7c3aed', '#2563eb', '#10b981', '#f59e0b',
    '#ef4444', '#8b5cf6', '#06b6d4', '#ec4899'
  ];
private authService = inject(AuthService); 
 private router = inject(Router);
  constructor(private http: HttpClient) {}

  ngOnInit(): void {
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
    this.loading.set(true);
    const params = new HttpParams()
      .set('page', '0')
      .set('size', '100');

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

  // ══════════════════════════════════════════════════
  // MOCK DATA
  // ══════════════════════════════════════════════════

  private generateMockLogs(): AuditLog[] {
    // ... votre code mock existant ...
    return [];
  }

  // ══════════════════════════════════════════════════
  // STATS
  // ══════════════════════════════════════════════════

  computeStats(): void {
    const today = new Date();
    today.setHours(0, 0, 0, 0);

    const yesterday = new Date(today);
    yesterday.setDate(yesterday.getDate() - 1);

    const todayLogs = this.allLogs.filter(l => {
      const d = new Date(l.timestamp); d.setHours(0, 0, 0, 0);
      return d.getTime() === today.getTime();
    });

    const yesterdayLogs = this.allLogs.filter(l => {
      const d = new Date(l.timestamp); d.setHours(0, 0, 0, 0);
      return d.getTime() === yesterday.getTime();
    });

    const todayLogins = todayLogs.filter(l => l.action === 'LOGIN_SUCCESS').length;
    const yestLogins = yesterdayLogs.filter(l => l.action === 'LOGIN_SUCCESS').length || 1;

    const thisMonth = this.allLogs.filter(l => {
      const d = new Date(l.timestamp);
      return d.getMonth() === today.getMonth() && d.getFullYear() === today.getFullYear();
    });

    this.stats = {
      loginsToday: todayLogins,
      loginsTrend: Math.round(((todayLogins - yestLogins) / yestLogins) * 100),
      activeAccounts: todayLogs.filter(l => l.action === 'LOGIN_SUCCESS').length,
      failedLogins: this.allLogs.filter(l => l.action === 'LOGIN_FAILED').length,
      lockedAccounts: this.allLogs.filter(l => l.action === 'ACCOUNT_LOCKED').length,
      passwordResets: thisMonth.filter(l => l.action === 'PASSWORD_RESET').length,
      adminActions: thisMonth.filter(l => ['USER_CREATED', 'USER_UPDATED', 'USER_DELETED', 'ROLE_ASSIGNED'].includes(l.action)).length,
    };
  }

  // ══════════════════════════════════════════════════
  // FILTERS & TABS
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
        l.userNom.toLowerCase().includes(s) ||
        l.userEmail.toLowerCase().includes(s) ||
        l.details.toLowerCase().includes(s) ||
        l.ipAddress.includes(s) ||
        this.getActionLabel(l.action).toLowerCase().includes(s)
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
    this.searchTerm = '';
    this.filterAction = '';
    this.filterLevel = '';
    this.filterDateFrom = '';
    this.filterDateTo = '';
    this.activeTab = 'all';
    this.applyFilters();
  }

  getCountByCategory(cat: string): number {
    return this.allLogs.filter(l => {
      if (cat === 'auth') {
        return l.action === 'LOGIN_SUCCESS' || l.action === 'LOGIN_FAILED' || l.action === 'LOGOUT';
      }
      if (cat === 'users') {
        return l.action === 'CREATE_USER' || l.action === 'UPDATE_USER' || l.action === 'DELETE_USER';
      }
      if (cat === 'security') {
        return l.action === 'ACCOUNT_LOCKED' || l.action === 'ACCOUNT_UNLOCKED' || l.action === 'DISABLE_USER';
      }
      return false;
    }).length;
  }

  // ══════════════════════════════════════════════════
  // PAGINATION
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
  // LABEL / CLASS HELPERS
  // ══════════════════════════════════════════════════

  getActionLabel(action: string): string {
    const m: Record<string, string> = {
      LOGIN_SUCCESS: 'Connexion réussie',
      LOGIN_FAILED: 'Connexion échouée',
      LOGOUT: 'Déconnexion',
      CREATE_USER: 'Utilisateur créé',
      UPDATE_USER: 'Utilisateur modifié',
      DELETE_USER: 'Utilisateur supprimé',
      ENABLE_USER: 'Utilisateur activé',
      DISABLE_USER: 'Utilisateur désactivé',
      RESET_PASSWORD: 'Mot de passe réinitialisé',
      ACCOUNT_LOCKED: 'Compte verrouillé',
      ACCOUNT_UNLOCKED: 'Compte déverrouillé',
      CREATE_TEAM: 'Équipe créée',
      UPDATE_TEAM: 'Équipe modifiée',
      DELETE_TEAM: 'Équipe supprimée',
      ASSIGN_MEMBER_TO_TEAM: 'Membre assigné',
      REMOVE_MEMBER_FROM_TEAM: 'Membre retiré',
      CREATE_ROLE: 'Rôle créé',
      UPDATE_ROLE: 'Rôle modifié',
      DELETE_ROLE: 'Rôle supprimé',
      CREATE_PROJECT: 'Projet créé',
      UPDATE_PROJECT: 'Projet modifié',
      ASSIGN_TEAM_TO_PROJECT: 'Équipe assignée au projet'
    };
    return m[action] ?? action;
  }

  getActionClass(action: string): string {
    const m: Record<string, string> = {
      LOGIN_SUCCESS: 'action-login-success',
      LOGIN_FAILED: 'action-login-failed',
      LOGOUT: 'action-logout',
      PASSWORD_RESET: 'action-password',
      ACCOUNT_LOCKED: 'action-locked',
      ACCOUNT_UNLOCKED: 'action-unlocked',
      USER_CREATED: 'action-user-create',
      USER_UPDATED: 'action-user-update',
      USER_DELETED: 'action-user-delete',
      ROLE_ASSIGNED: 'action-role',
    };
    return m[action] ?? 'action-default';
  }

  getLevelClass(level: string): string {
    return `level-${level}`;
  }

  getInitials(prenom: string, nom: string): string {
    return ((prenom?.charAt(0) || '') + (nom?.charAt(0) || '')).toUpperCase();
  }

  getAvatarColor(index: number): string {
    return this.colors[index % this.colors.length];
  }

  // ══════════════════════════════════════════════════
  // EXPORT (CSV ou EXCEL)
  // ══════════════════════════════════════════════════

  exportLogs(format: 'csv' | 'excel'): void {
    const headers = [ 'Date', 'Heure', 'Utilisateur', 'Email', 'Action', 'Détails',  'Niveau', 'Statut'];
    
    const rows = this.filteredLogs.map(l => [
      
      new Date(l.timestamp).toLocaleDateString('fr-FR'),
      new Date(l.timestamp).toLocaleTimeString('fr-FR'),
      `${l.userPrenom} ${l.userNom}`,
      l.userEmail,
      this.getActionLabel(l.action),
      l.details,
      
      l.level,
      l.success ? 'Succès' : 'Échec'
    ]);

    const data = [headers, ...rows];
    const fileName = `audit_${new Date().toISOString().split('T')[0]}`;

    if (format === 'excel') {
      const ws = XLSX.utils.aoa_to_sheet(data);
      ws['!cols'] = [
        { wch: 8 }, { wch: 12 }, { wch: 8 }, { wch: 20 }, { wch: 25 },
        { wch: 25 }, { wch: 50 }, { wch: 15 }, { wch: 10 }, { wch: 10 }
      ];
      const wb = XLSX.utils.book_new();
      XLSX.utils.book_append_sheet(wb, ws, 'Audit_Logs');
      XLSX.writeFile(wb, `${fileName}.xlsx`);
      this.triggerToast('Export Excel téléchargé avec succès', 'success');
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
      this.triggerToast('Export CSV téléchargé avec succès', 'success');
    }
    this.closeDropdown();
  }

  // ══════════════════════════════════════════════════
  // MODAL DÉTAIL
  // ══════════════════════════════════════════════════

  openDetail(log: AuditLog): void {
    this.selectedLog = log;
    document.body.style.overflow = 'hidden';
  }

  closeDetail(): void {
    this.selectedLog = null;
    document.body.style.overflow = '';
  }

  getDetailIconClass(category: string): string {
    const m: Record<string, string> = {
      auth: 'icon-modal-auth',
      users: 'icon-modal-users',
      security: 'icon-modal-security',
    };
    return m[category] ?? 'icon-modal-auth';
  }

  // ══════════════════════════════════════════════════
  // TOAST
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
    } else if (roles.includes('CHEF_PROJET')) {
      this.router.navigate(['/chef-projet/dashboard']);
    } else if (roles.includes('MANAGER')) {
      this.router.navigate(['/manager/dashboard']);
    } else if (roles.includes('RESPONSABLE_CONTRAT')) {
      this.router.navigate(['/responsable-contrat/dashboard']);
    } else if (roles.includes('MEMBRE_EQUIPE')) {
      this.router.navigate(['/membre-equipe/dashboard']);
    } else {
      // Par défaut, essayer de rediriger selon l'URL actuelle
      const currentUrl = this.router.url;
      if (currentUrl.includes('/admin')) {
        this.router.navigate(['/admin/dashboard']);
      } else {
        this.router.navigate(['/login']);
      }
    }
}}

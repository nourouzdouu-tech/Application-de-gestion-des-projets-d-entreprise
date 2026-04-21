import { Component, OnInit, signal, WritableSignal, HostListener } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClient, HttpParams } from '@angular/common/http';
import * as XLSX from 'xlsx';

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
  selector: 'app-manager-audit',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './audit.html',
  styleUrl: './audit.css',
})
export class ManagerAuditComponent implements OnInit {

  loading: WritableSignal<boolean> = signal(false);
  allLogs: WorkflowLog[] = [];
  filteredLogs: WorkflowLog[] = [];

  // ── Modal détail ──
  selectedLog: WorkflowLog | null = null;

  // Filters
  searchTerm = '';
  filterPhase = '';
  filterRole = '';
  filterStatus = '';
  filterDateFrom = '';
  filterDateTo = '';
  activeTab: WorkflowLog['phase'] | 'all' = 'all';

  currentPage = 1;
  itemsPerPage = 12;

  stats: WorkflowStats = {
    projetsCreated: 0, enAttente: 0, valides: 0,
    rejetes: 0, chefsAssigned: 0, tachesCreated: 0
  };

  dropdownOpen: boolean = false;
  showToast = false;
  toastMessage = '';
  toastType: 'success' | 'error' = 'success';

  private colors = ['#7c3aed', '#2563eb', '#10b981', '#f59e0b', '#ef4444', '#8b5cf6', '#06b6d4', '#ec4899'];

  constructor(private http: HttpClient) { }

  ngOnInit(): void { this.loadAll(); }

  // ══════════════════════════════════════════════════
  // MODAL DÉTAIL — ouvert par le bouton œil
  // ══════════════════════════════════════════════════

  openDetail(log: WorkflowLog): void {
    this.selectedLog = log;
    document.body.style.overflow = 'hidden';
  }

  closeDetail(): void {
    this.selectedLog = null;
    document.body.style.overflow = '';
  }

  // ══════════════════════════════════════════════════
  // Gestion du menu déroulant
  // ══════════════════════════════════════════════════

// Gestion du menu déroulant
toggleDropdown(event: Event): void {
  event.stopPropagation();
  this.dropdownOpen = !this.dropdownOpen;
}

closeDropdown(): void {
  this.dropdownOpen = false;
}

// Fermer le dropdown quand on clique ailleurs
@HostListener('document:click')
onDocumentClick(): void {
  this.dropdownOpen = false;
}

// Export
exportLogs(format: 'csv' | 'excel'): void {
  const headers = ['ID', 'Date', 'Heure', 'Acteur', 'Email', 'Rôle', 'Phase', 'Action', 'Projet', 'Détails', 'Statut'];
  
  const rows = this.filteredLogs.map(l => [
    l.id,
    new Date(l.timestamp).toLocaleDateString('fr-FR'),
    new Date(l.timestamp).toLocaleTimeString('fr-FR'),
    l.actorName,
    l.actorEmail,
    this.getRoleLabel(l.actorRole),
    this.getPhaseLabel(l.phase),
    this.getActionLabel(l.action),
    l.projectName || '-',
    l.details,
    this.getStatusLabel(l.statusType)
  ]);

  const data = [headers, ...rows];
  const fileName = `audit-workflow_${new Date().toISOString().split('T')[0]}`;

  if (format === 'excel') {
    const ws = XLSX.utils.aoa_to_sheet(data);
    ws['!cols'] = [
      { wch: 8 }, { wch: 12 }, { wch: 8 }, { wch: 20 }, { wch: 25 },
      { wch: 15 }, { wch: 12 }, { wch: 20 }, { wch: 25 }, { wch: 50 }, { wch: 10 }
    ];
    const wb = XLSX.utils.book_new();
    XLSX.utils.book_append_sheet(wb, ws, 'Audit_Workflow');
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
  
  // Fermer le dropdown après l'export
  this.closeDropdown();
}

  getDetailIconClass(phase: string): string {
    const m: Record<string, string> = {
      creation: 'icon-modal-creation',
      validation: 'icon-modal-validation',
      assignation: 'icon-modal-assignation',
      gestion: 'icon-modal-gestion',
      execution: 'icon-modal-execution',
    };
    return m[phase] ?? 'icon-modal-gestion';
  }

  // ══════════════════════════════════════════════════
  // LOADING
  // ══════════════════════════════════════════════════

  loadAll(): void {
    this.loading.set(true);
    const params = new HttpParams().set('page', '0').set('size', '500');

    this.http.get<BackendPage>('http://localhost:8080/api/manager/audit', { params }).subscribe({
      next: (response) => {
        const raw: AuditLogDto[] = response?.content ?? (Array.isArray(response) ? response as any : []);
        this.allLogs = this.mapToViewModel(raw);
        this.computeStats();
        this.applyFilters();
        this.loading.set(false);
      },
      error: () => {
        this.allLogs = this.generateMockLogs();
        this.computeStats();
        this.applyFilters();
        this.loading.set(false);
      }
    });
  }

  private mapToViewModel(raw: AuditLogDto[]): WorkflowLog[] {
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
      { regex: /dans le projet\s+['"]?([^'"→\n]+?)['"]?/i, desc: 'dans le projet' },
      { regex: /dans\s+['"]?([A-Z][a-zA-Z0-9\s\-éèêëàâôûç]{2,50}?)['"]?(?:\s+[—–-]|\s*$|\.|,)/i, desc: 'dans X' },
      { regex: /rejetée dans\s+['"]?([^'"→\n]+?)['"]?/i, desc: 'rejetée dans' },
      { regex: /validée dans\s+['"]?([^'"→\n]+?)['"]?/i, desc: 'validée dans' },
      { regex: /soumise pour validation dans\s+['"]?([^'"→\n]+?)['"]?/i, desc: 'soumise dans' },
      { regex: /mis à jour.*?dans\s+['"]?([^'"→\n]+?)['"]?/i, desc: 'mis à jour dans' },
      { regex: /projet\s+['"]?([^'"→\n]+?)['"]?\s*(?:→|$| avec| par)/i, desc: 'projet' },
      { regex: /au projet\s+['"]?([^'"→\n]+?)['"]?\s*$/i, desc: 'au projet' },
      { regex: /du projet\s+['"]?([^'"→\n]+?)['"]?\s*(?:→|$)/i, desc: 'du projet' },
      { regex: /pour le projet\s+['"]?([^'"→\n]+?)['"]?/i, desc: 'pour le projet' },
      { regex: /Projet\s+['"]?([^'"→\n]+?)['"]?\s+rejeté/i, desc: 'Projet rejeté' },
    ];

    for (const pattern of patterns) {
      const match = details.match(pattern.regex);
      if (match && match[1]) {
        let projectName = match[1].trim();
        projectName = projectName.replace(/[→'"`]/g, '');
        if (projectName.length > 2 && projectName.length < 80 &&
          !projectName.match(/^(en|la|le|les|un|une|ce|cette|validation|statut|cours)$/i)) {
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

  // ══════════════════════════════════════════════════
  // MOCK DATA
  // ══════════════════════════════════════════════════

  private generateMockLogs(): WorkflowLog[] {
    const actors = [
      { name: 'Fatima Zahra', email: 'fz@dxc.ma', role: 'RESPONSABLE_CONTRAT' },
      { name: 'Hanan Hafidi', email: 'hanan@dxc.ma', role: 'MANAGER' },
      { name: 'Omar Tazi', email: 'omar@dxc.ma', role: 'CHEF_PROJET' },
      { name: 'Sara Benali', email: 'sara@dxc.ma', role: 'CHEF_PROJET' },
      { name: 'Aya Ibrahimi', email: 'aya@dxc.ma', role: 'MEMBRE_EQUIPE' },
    ];
    const projects = ['Portail Client DXC', 'Refonte Infrastructure', 'App Mobile RH', 'Dashboard Analytics'];
    const events: Array<{ action: string; detailsFn: (p: string) => string }> = [
      { action: 'CREATE_PROJECT', detailsFn: p => `Création du projet ${p} avec client DXC Technology` },
      { action: 'UPDATE_PROJECT', detailsFn: p => `Modification du projet ${p}` },
      { action: 'VALIDATE_PROJECT', detailsFn: p => `Validation du projet ${p} par le manager` },
      { action: 'REJECT_PROJECT', detailsFn: p => `Rejet du projet ${p} — budget insuffisant` },
      { action: 'ASSIGN_CHEF', detailsFn: p => `Chef de projet assigné au projet ${p}` },
      { action: 'CREATE_TEAM', detailsFn: p => `Création de l'équipe pour le projet ${p}` },
      { action: 'ASSIGN_MEMBER_TO_TEAM', detailsFn: p => `Membre assigné à l'équipe du projet ${p}` },
      { action: 'REMOVE_MEMBER_FROM_TEAM', detailsFn: p => `Membre retiré de l'équipe du projet ${p}` },
      { action: 'ASSIGN_TEAM_TO_PROJECT', detailsFn: p => `Assignation de l'équipe au projet ${p}` },
      { action: 'CREATE_TASK', detailsFn: p => `Création d'une tâche dans le projet ${p}` },
      { action: 'UPDATE_TASK', detailsFn: p => `Modification de la tâche "Développer l'API" dans ${p}` },
      { action: 'VALIDATE_TASK', detailsFn: p => `Tâche "Développer l'API" validée dans le projet ${p}` },
      { action: 'REJECT_TASK', detailsFn: p => `Tâche "Tests unitaires" rejetée dans ${p} — corrections requises` },
      { action: 'UPDATE_TASK_STATUS', detailsFn: p => `Statut tâche "Maquettes UI" mis à jour en "En validation" dans ${p}` },
      { action: 'SUBMIT_TASK', detailsFn: p => `Tâche "Rapport final" soumise pour validation dans ${p}` },
    ];

    const logs: WorkflowLog[] = [];
    const now = new Date();
    let id = 1;

    for (let d = 0; d < 30; d++) {
      for (let i = 0; i < Math.floor(Math.random() * 5) + 2; i++) {
        const ev = events[Math.floor(Math.random() * events.length)];
        const proj = projects[Math.floor(Math.random() * projects.length)];
        const role = ROLE_MAP[ev.action] ?? 'RESPONSABLE_CONTRAT';
        const actor = actors.find(a => a.role === role) ?? actors[0];
        const ts = new Date(now);
        ts.setDate(ts.getDate() - d);
        ts.setHours(Math.floor(Math.random() * 22) + 1, Math.floor(Math.random() * 59), 0);
        logs.push({
          id: id++,
          timestamp: ts,
          actorName: actor.name,
          actorEmail: actor.email,
          actorRole: role,
          phase: PHASE_MAP[ev.action],
          action: ev.action,
          projectName: proj,
          details: ev.detailsFn(proj),
          statusType: ev.action.includes('REJECT') ? 'rejected' : 'success'
        });
      }
    }
    return logs.sort((a, b) => b.timestamp.getTime() - a.timestamp.getTime());
  }

  // ══════════════════════════════════════════════════
  // STATS
  // ══════════════════════════════════════════════════

  computeStats(): void {
    this.stats = {
      projetsCreated: this.allLogs.filter(l => l.action === 'CREATE_PROJECT').length,
      enAttente: this.allLogs.filter(l => l.phase === 'creation').length,
      valides: this.allLogs.filter(l => l.action === 'VALIDATE_PROJECT').length,
      rejetes: this.allLogs.filter(l => l.action === 'REJECT_PROJECT').length,
      chefsAssigned: this.allLogs.filter(l => l.action === 'ASSIGN_CHEF').length,
      tachesCreated: this.allLogs.filter(l => l.action === 'CREATE_TASK').length,
    };
  }

  // ══════════════════════════════════════════════════
  // FILTERS
  // ══════════════════════════════════════════════════

  setTab(tab: typeof this.activeTab): void { this.activeTab = tab; this.currentPage = 1; this.applyFilters(); }

  applyFilters(): void {
    let logs = [...this.allLogs];
    if (this.activeTab !== 'all') logs = logs.filter(l => l.phase === this.activeTab);
    if (this.searchTerm.trim()) {
      const s = this.searchTerm.toLowerCase();
      logs = logs.filter(l =>
        l.actorName.toLowerCase().includes(s) ||
        l.actorEmail.toLowerCase().includes(s) ||
        l.projectName.toLowerCase().includes(s) ||
        l.details.toLowerCase().includes(s) ||
        this.getActionLabel(l.action).toLowerCase().includes(s)
      );
    }
    if (this.filterPhase) logs = logs.filter(l => l.phase === this.filterPhase);
    if (this.filterRole) logs = logs.filter(l => l.actorRole === this.filterRole);
    if (this.filterStatus === 'success') logs = logs.filter(l => l.statusType === 'success');
    if (this.filterStatus === 'rejected') logs = logs.filter(l => l.statusType === 'rejected');
    if (this.filterDateFrom) { const from = new Date(this.filterDateFrom); logs = logs.filter(l => new Date(l.timestamp) >= from); }
    if (this.filterDateTo) { const to = new Date(this.filterDateTo); to.setHours(23, 59, 59); logs = logs.filter(l => new Date(l.timestamp) <= to); }
    this.filteredLogs = logs;
    this.currentPage = 1;
  }

  clearFilters(): void {
    this.searchTerm = ''; this.filterPhase = ''; this.filterRole = '';
    this.filterStatus = ''; this.filterDateFrom = ''; this.filterDateTo = '';
    this.activeTab = 'all'; this.applyFilters();
  }

  getCountByPhase(phase: string): number { return this.allLogs.filter(l => l.phase === phase).length; }

  // ══════════════════════════════════════════════════
  // PAGINATION
  // ══════════════════════════════════════════════════

  get paginatedLogs(): WorkflowLog[] { const s = (this.currentPage - 1) * this.itemsPerPage; return this.filteredLogs.slice(s, s + this.itemsPerPage); }
  get totalPages(): number { return Math.max(1, Math.ceil(this.filteredLogs.length / this.itemsPerPage)); }
  setPage(p: number): void { if (p >= 1 && p <= this.totalPages) this.currentPage = p; }
  getPageNumbers(): number[] {
    const total = this.totalPages, cur = this.currentPage, pages: number[] = [];
    if (total <= 7) { for (let i = 1; i <= total; i++) pages.push(i); }
    else if (cur <= 4) { pages.push(1, 2, 3, 4, 5, -1, total); }
    else if (cur >= total - 3) { pages.push(1, -1, total - 4, total - 3, total - 2, total - 1, total); }
    else { pages.push(1, -1, cur - 1, cur, cur + 1, -1, total); }
    return pages;
  }
  getRangeStart(): number { return this.filteredLogs.length === 0 ? 0 : (this.currentPage - 1) * this.itemsPerPage + 1; }
  getRangeEnd(): number { return Math.min(this.currentPage * this.itemsPerPage, this.filteredLogs.length); }

  // ══════════════════════════════════════════════════
  // LABEL / CLASS HELPERS
  // ══════════════════════════════════════════════════

  getActionLabel(action: string): string {
    const m: Record<string, string> = {
      CREATE_PROJECT: 'Projet créé', UPDATE_PROJECT: 'Projet modifié',
      VALIDATE_PROJECT: 'Projet validé', REJECT_PROJECT: 'Projet rejeté',
      ASSIGN_CHEF: 'Chef assigné',
      CREATE_TEAM: 'Équipe créée', UPDATE_TEAM: 'Équipe modifiée', DELETE_TEAM: 'Équipe supprimée',
      ASSIGN_MEMBER_TO_TEAM: 'Membre assigné', REMOVE_MEMBER_FROM_TEAM: 'Membre retiré',
      ASSIGN_TEAM_TO_PROJECT: 'Équipe assignée au projet',
      CREATE_TASK: 'Tâche créée', UPDATE_TASK: 'Tâche modifiée', DELETE_TASK: 'Tâche supprimée',
      VALIDATE_TASK: 'Tâche validée', REJECT_TASK: 'Tâche rejetée',
      UPDATE_TASK_STATUS: 'Statut mis à jour', SUBMIT_TASK: 'Tâche soumise',
    };
    return m[action] ?? action;
  }

  getActionClass(action: string): string {
    const m: Record<string, string> = {
      CREATE_PROJECT: 'action-create-project', UPDATE_PROJECT: 'action-update-status',
      VALIDATE_PROJECT: 'action-validate', REJECT_PROJECT: 'action-reject',
      ASSIGN_CHEF: 'action-assign-chef',
      CREATE_TEAM: 'action-create-team', UPDATE_TEAM: 'action-update-status', DELETE_TEAM: 'action-reject',
      ASSIGN_MEMBER_TO_TEAM: 'action-assign-team', REMOVE_MEMBER_FROM_TEAM: 'action-reject',
      ASSIGN_TEAM_TO_PROJECT: 'action-assign-team',
      CREATE_TASK: 'action-create-task', UPDATE_TASK: 'action-update-task', DELETE_TASK: 'action-reject',
      VALIDATE_TASK: 'action-validate-task', REJECT_TASK: 'action-reject-task',
      UPDATE_TASK_STATUS: 'action-update-status', SUBMIT_TASK: 'action-validate',
    };
    return m[action] ?? 'action-default';
  }

  getRoleLabel(role: string): string {
    const m: Record<string, string> = { RESPONSABLE_CONTRAT: 'Resp. Contrat', MANAGER: 'Manager', CHEF_PROJET: 'Chef Projet', MEMBRE_EQUIPE: 'Membre Équipe' };
    return m[role] ?? role;
  }
  getRoleClass(role: string): string {
    const m: Record<string, string> = { RESPONSABLE_CONTRAT: 'role-responsable', MANAGER: 'role-manager', CHEF_PROJET: 'role-chef', MEMBRE_EQUIPE: 'role-membre' };
    return m[role] ?? 'role-membre';
  }
  getPhaseLabel(phase: string): string {
    const m: Record<string, string> = { creation: 'Création', validation: 'Validation', assignation: 'Assignation', gestion: 'Gestion', execution: 'Exécution' };
    return m[phase] ?? phase;
  }
  getPhaseClass(phase: string): string { return `phase-${phase}`; }
  getStatusLabel(s: string): string { return s === 'success' ? 'Succès' : 'Rejeté'; }
  getStatusClass(s: string): string { return s === 'success' ? 'status-ok' : 'status-rejected'; }
  getInitials(name: string): string { return (name ?? '?').trim().split(' ').map(p => p.charAt(0)).slice(0, 2).join('').toUpperCase(); }
  getAvatarColor(index: number): string { return this.colors[index % this.colors.length]; }
  buildCode(name: string): string {
    if (!name) return 'PR';
    return name.split(' ').filter(Boolean).slice(0, 2).map(w => w.charAt(0).toUpperCase()).join('') || 'PR';
  }

  // ══════════════════════════════════════════════════
  // EXPORT (CSV ou EXCEL)
  // ══════════════════════════════════════════════════


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
}
import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import {
  ReportingService,
  ProjectReportDto,
  TaskReportDto,
  UserReportDto,
  UserStatusReportDto,
  SelectOption,
  ManagerProjectDto
} from '../../../core/services/reporting.service';

type ReportType =
  | 'projects-global'
  | 'projects-overdue'
  | 'tasks-overdue'
  | 'users-no-profile'
  | 'users-by-status'
  | 'projects-by-manager';

@Component({
  selector: 'app-reporting',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './reporting.html',
  styleUrls: ['./reporting.css']
})
export class ReportingComponent implements OnInit {

  activeReport: ReportType | null = null;
  loading = false;

  // Pagination
  currentPage = 1;
  itemsPerPage = 5;

  // Filters
  filterYear: number | undefined;
  filterStatus: string | undefined;
  filterTeamId: number | undefined;
  filterProjectId: number | undefined;
  filterUserStatus: boolean | undefined = undefined;

  // Filters pour projets par manager
  filterFacture: 'all' | 'factured' | 'not-factured' = 'all';
  selectedManager: string = 'all';
  managersList: string[] = [];

  // Select options
  projects: SelectOption[] = [];
  teams: SelectOption[] = [];
  years: number[] = [];

  projectStatuses = [
    { value: 'PRE_VALIDE', label: 'Pré-validé' },
    { value: 'EN_VALIDATION', label: 'En validation' },
    { value: 'EN_COURS', label: 'En cours' },
    { value: 'CLOTURE', label: 'Clôturé' },
    { value: 'REJETE', label: 'Rejeté' },
  ];

  // Data
  projectsData: ProjectReportDto[] = [];
  overdueProjectsData: ProjectReportDto[] = [];
  overdueTasksData: TaskReportDto[] = [];
  usersNoProfileData: UserReportDto[] = [];
  userStatusData: UserStatusReportDto | null = null;
  projectsByManager: ManagerProjectDto[] = [];

  constructor(private reportingService: ReportingService) {
    const currentYear = new Date().getFullYear();
    for (let y = currentYear; y >= currentYear - 5; y--) {
      this.years.push(y);
    }
  }

  ngOnInit(): void {
    this.reportingService.getProjectsForSelect().subscribe(p => this.projects = p);
    this.reportingService.getTeamsForSelect().subscribe(t => this.teams = t);
  }

  // Pagination getters
  get paginatedProjectsData(): ProjectReportDto[] {
    const start = (this.currentPage - 1) * this.itemsPerPage;
    return this.projectsData.slice(start, start + this.itemsPerPage);
  }

  get paginatedOverdueProjectsData(): ProjectReportDto[] {
    const start = (this.currentPage - 1) * this.itemsPerPage;
    return this.overdueProjectsData.slice(start, start + this.itemsPerPage);
  }

  get paginatedOverdueTasksData(): TaskReportDto[] {
    const start = (this.currentPage - 1) * this.itemsPerPage;
    return this.overdueTasksData.slice(start, start + this.itemsPerPage);
  }

  get paginatedUsersNoProfileData(): UserReportDto[] {
    const start = (this.currentPage - 1) * this.itemsPerPage;
    return this.usersNoProfileData.slice(start, start + this.itemsPerPage);
  }

  get paginatedUserStatusUsers(): UserReportDto[] {
    if (!this.userStatusData) return [];
    const start = (this.currentPage - 1) * this.itemsPerPage;
    return this.userStatusData.users.slice(start, start + this.itemsPerPage);
  }

  // Getters pour projets par manager
  get filteredProjectsByManager(): ManagerProjectDto[] {
    let filtered = [...this.projectsByManager];
    
    if (this.filterFacture === 'factured') {
      filtered = filtered.filter(p => p.factured === true);
    } else if (this.filterFacture === 'not-factured') {
      filtered = filtered.filter(p => p.factured === false);
    }
    
    if (this.selectedManager !== 'all') {
      filtered = filtered.filter(p => p.managerName === this.selectedManager);
    }
    
    return filtered;
  }

  get paginatedProjectsByManager(): ManagerProjectDto[] {
    const start = (this.currentPage - 1) * this.itemsPerPage;
    return this.filteredProjectsByManager.slice(start, start + this.itemsPerPage);
  }

  get totalPages(): number {
    let totalItems = 0;
    switch (this.activeReport) {
      case 'projects-global': totalItems = this.projectsData.length; break;
      case 'projects-overdue': totalItems = this.overdueProjectsData.length; break;
      case 'tasks-overdue': totalItems = this.overdueTasksData.length; break;
      case 'users-no-profile': totalItems = this.usersNoProfileData.length; break;
      case 'users-by-status': totalItems = this.userStatusData?.users.length || 0; break;
      case 'projects-by-manager': totalItems = this.filteredProjectsByManager.length; break;
      default: return 1;
    }
    return Math.ceil(totalItems / this.itemsPerPage);
  }

  changePage(page: number): void {
    if (page >= 1 && page <= this.totalPages) {
      this.currentPage = page;
    }
  }

  openReport(type: ReportType): void {
    this.activeReport = type;
    this.currentPage = 1;
    this.loadReport(type);
  }

  back(): void {
    this.activeReport = null;
    this.currentPage = 1;
    this.resetFilters();
  }

  resetFilters(): void {
    this.filterYear = undefined;
    this.filterStatus = undefined;
    this.filterTeamId = undefined;
    this.filterProjectId = undefined;
    this.filterUserStatus = undefined;
    this.filterFacture = 'all';
    this.selectedManager = 'all';
    this.projectsData = [];
    this.overdueProjectsData = [];
    this.overdueTasksData = [];
    this.usersNoProfileData = [];
    this.userStatusData = null;
    this.projectsByManager = [];
  }

  loadReport(type: ReportType): void {
    this.loading = true;
    this.currentPage = 1;
    
    switch (type) {
      case 'projects-global':
        this.reportingService.getProjectsReport(
          this.filterYear,
          this.filterStatus,
          this.filterTeamId
        ).subscribe({
          next: data => { this.projectsData = data; this.loading = false; },
          error: () => this.loading = false
        });
        break;

      case 'projects-overdue':
        this.reportingService.getOverdueProjects().subscribe({
          next: data => { this.overdueProjectsData = data; this.loading = false; },
          error: () => this.loading = false
        });
        break;

      case 'tasks-overdue':
        this.reportingService.getOverdueTasks(this.filterProjectId).subscribe({
          next: data => { this.overdueTasksData = data; this.loading = false; },
          error: () => this.loading = false
        });
        break;

      case 'users-no-profile':
        this.reportingService.getUsersWithoutProfile().subscribe({
          next: data => { this.usersNoProfileData = data; this.loading = false; },
          error: () => this.loading = false
        });
        break;

      case 'users-by-status':
        this.reportingService.getUsersByStatus(this.filterUserStatus).subscribe({
          next: data => { this.userStatusData = data; this.loading = false; },
          error: () => this.loading = false
        });
        break;

      case 'projects-by-manager':
        this.loadProjectsByManager();
        break;
    }
  }

  loadProjectsByManager(): void {
  this.reportingService.getProjectsByManager().subscribe({
    next: (data) => {
      this.projectsByManager = data;
      this.managersList = [...new Set(data.map(p => p.managerName).filter((name): name is string => name !== undefined && name !== null && name !== ''))];
      this.loading = false;
    },
    error: (err) => {
      console.error('Erreur chargement projets par manager:', err);
      this.loading = false;
    }
  });
}

  applyFilters(): void {
    if (this.activeReport === 'projects-by-manager') {
      this.currentPage = 1;
    } else if (this.activeReport) {
      this.loadReport(this.activeReport);
    }
  }

  onExportTypeChange(event: Event): void {
    const select = event.target as HTMLSelectElement;
    const format = select.value;
    if (!format) return;

    if (format === 'excel') {
      this.exportExcel();
    } else if (format === 'pdf') {
      this.exportPdf();
    }
    select.value = '';
  }
  private exportExcel(): void {
  switch (this.activeReport) {
    case 'projects-global':
      this.reportingService.exportProjectsExcel(
        this.filterYear,
        this.filterStatus,
        this.filterTeamId
      ).subscribe(blob => this.downloadBlob(blob, 'projects.xlsx'));
      break;
    case 'projects-overdue':
      this.reportingService.exportOverdueProjectsExcel().subscribe(blob => 
        this.downloadBlob(blob, 'overdue_projects.xlsx'));
      break;
    case 'tasks-overdue':
      this.reportingService.exportOverdueTasksExcel(this.filterProjectId).subscribe(blob => 
        this.downloadBlob(blob, 'overdue_tasks.xlsx'));
      break;
    case 'users-no-profile':
      this.reportingService.exportUsersNoProfileExcel().subscribe(blob => 
        this.downloadBlob(blob, 'users_no_profile.xlsx'));
      break;
    case 'users-by-status':
      this.reportingService.exportUsersByStatusExcel(this.filterUserStatus).subscribe(blob => 
        this.downloadBlob(blob, 'users_by_status.xlsx'));
      break;
    case 'projects-by-manager':
      this.exportProjectsByManagerExcel();
      break;
  }
}
  private exportPdf(): void {
  switch (this.activeReport) {
    case 'projects-global':
      this.reportingService.exportProjectsPdf(
        this.filterYear,
        this.filterStatus,
        this.filterTeamId
      ).subscribe(blob => this.downloadBlob(blob, 'projects.pdf'));
      break;
    case 'projects-overdue':
      this.reportingService.exportOverdueProjectsPdf().subscribe(blob => 
        this.downloadBlob(blob, 'overdue_projects.pdf'));
      break;
    case 'tasks-overdue':
      this.reportingService.exportOverdueTasksPdf(this.filterProjectId).subscribe(blob => 
        this.downloadBlob(blob, 'overdue_tasks.pdf'));
      break;
    case 'users-no-profile':
      this.reportingService.exportUsersNoProfilePdf().subscribe(blob => 
        this.downloadBlob(blob, 'users_no_profile.pdf'));
      break;
    case 'users-by-status':
      this.reportingService.exportUsersByStatusPdf(this.filterUserStatus).subscribe(blob => 
        this.downloadBlob(blob, 'users_by_status.pdf'));
      break;
    case 'projects-by-manager':
      this.exportProjectsByManagerPdf();
      break;
  }
}


  private downloadBlob(blob: Blob, filename: string): void {
    const url = window.URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = filename;
    a.click();
    window.URL.revokeObjectURL(url);
  }

  getReportTitle(type: ReportType): string {
    const titles: Record<ReportType, string> = {
      'projects-global': 'Liste globale des projets',
      'projects-overdue': 'Projets hors délai',
      'tasks-overdue': 'Tâches en retard',
      'users-no-profile': 'Utilisateurs sans profil',
      'users-by-status': 'Utilisateurs par statut',
      'projects-by-manager': 'Projets par Manager',
    };
    return titles[type];
  }

  getStatusClass(status: string): string {
    const map: Record<string, string> = {
      'PRE_VALIDE': 'badge-warning',
      'EN_VALIDATION': 'badge-info',
      'EN_COURS': 'badge-success',
      'CLOTURE': 'badge-secondary',
      'REJETE': 'badge-danger',
    };
    return map[status] || 'badge-secondary';
  }

  getStatusLabel(status: string): string {
    const found = this.projectStatuses.find(s => s.value === status);
    return found ? found.label : status;
  }

  getPriorityClass(priority: string): string {
    const map: Record<string, string> = {
      'HAUTE': 'badge-danger',
      'MOYENNE': 'badge-warning',
      'BASSE': 'badge-success',
    };
    return map[priority] || 'badge-secondary';
  }

  getTaskStatusLabel(status: string): string {
    const map: Record<string, string> = {
      'A_FAIRE': 'À faire',
      'EN_COURS': 'En cours',
      'EN_REVISION': 'En révision',
      'TERMINE': 'Terminé',
      'VALIDE': 'Validé',
      'REJETE': 'Rejeté',
    };
    return map[status] || status;
  }

  getRoleLabel(role: string): string {
    const map: Record<string, string> = {
      'ROLE_ADMIN': 'Admin',
      'ROLE_MANAGER': 'Manager',
      'ROLE_CHEF_PROJET': 'Chef de projet',
      'ROLE_MEMBRE': 'Membre',
    };
    return map[role] || role;
  }

  // Utilitaires pour les projets par manager
  getInitials(name: string): string {
    if (!name) return '??';
    return name.split(' ').map(n => n[0]).join('').toUpperCase().substring(0, 2);
  }

  getAvatarColor(name: string): string {
    const colors = ['#7c3aed', '#3b82f6', '#10b981', '#f59e0b', '#ef4444', '#ec4899', '#06b6d4'];
    let hash = 0;
    for (let i = 0; i < name.length; i++) {
      hash = name.charCodeAt(i) + ((hash << 5) - hash);
    }
    return colors[Math.abs(hash) % colors.length];
  }
  // Export Excel pour Projets par Manager
private exportProjectsByManagerExcel(): void {
  const headers = [
    'Projet', 
    'Client', 
    'Manager', 
    'Chef de projet', 
    'Équipe', 
    'Date début', 
    'Date fin', 
    'Facturé', 
    'Nombre membres', 
    'Membres'
  ];
  
  const rows = this.filteredProjectsByManager.map(p => [
    p.name,
    p.client,
    p.managerName || '—',
    p.chefProjetName || '—',
    p.teamName || '—',
    p.startDate ? new Date(p.startDate).toLocaleDateString('fr-FR') : '—',
    p.endDate ? new Date(p.endDate).toLocaleDateString('fr-FR') : '—',
    p.factured ? 'Facturé' : 'Non facturé',
    p.members.length,
    p.members.map(m => `${m.fullName} (${m.profile})`).join(', ')
  ]);
  
  const csv = [headers, ...rows].map(row => row.join(';')).join('\n');
  const blob = new Blob(['\uFEFF' + csv], { type: 'text/csv;charset=utf-8;' });
  this.downloadBlob(blob, `projets_manager_${new Date().toISOString().split('T')[0]}.csv`);
}
// Export PDF pour Projets par Manager (téléchargement direct - sans popup)
private exportProjectsByManagerPdf(): void {
  // Créer un contenu HTML pour le PDF
  const content = `
    <!DOCTYPE html>
    <html>
    <head>
      <meta charset="UTF-8">
      <title>Projets_par_Manager_${new Date().toISOString().split('T')[0]}</title>
      <style>
        * { margin: 0; padding: 0; box-sizing: border-box; }
        body { 
          font-family: 'Segoe UI', Arial, sans-serif; 
          margin: 20px; 
          padding: 20px;
          background: white;
        }
        h1 { 
          color: #4f46e5; 
          border-bottom: 3px solid #4f46e5; 
          padding-bottom: 10px; 
          margin-bottom: 15px;
          font-size: 24px;
        }
        .header-info {
          margin: 20px 0;
          padding: 15px;
          background: #f8fafc;
          border-radius: 8px;
          border: 1px solid #e5e7eb;
        }
        .header-info p {
          margin: 5px 0;
          color: #374151;
        }
        .filters { 
          margin: 20px 0; 
          padding: 12px 15px; 
          background: #f3f4f6; 
          border-radius: 8px; 
          border-left: 4px solid #4f46e5;
        }
        .filter-title {
          font-weight: 600;
          margin-bottom: 8px;
          color: #374151;
        }
        .filter-badge { 
          display: inline-block; 
          padding: 4px 12px; 
          background: #e5e7eb; 
          border-radius: 20px; 
          margin-right: 10px; 
          font-size: 12px;
          color: #1f2937;
        }
        table { 
          width: 100%; 
          border-collapse: collapse; 
          margin-top: 20px; 
          font-size: 13px;
        }
        th { 
          background: #4f46e5; 
          color: white; 
          padding: 12px 8px; 
          text-align: left; 
          font-weight: 600;
        }
        td { 
          padding: 10px 8px; 
          border-bottom: 1px solid #e5e7eb; 
          vertical-align: top;
        }
        tr:hover { background: #f9fafb; }
        .factured { color: #10b981; font-weight: bold; }
        .not-factured { color: #f59e0b; }
        .members-list {
          font-size: 11px;
          color: #6b7280;
          max-width: 250px;
        }
        .footer { 
          margin-top: 30px; 
          padding-top: 15px;
          text-align: center; 
          font-size: 11px; 
          color: #9ca3af; 
          border-top: 1px solid #e5e7eb;
        }
        @media print {
          body { margin: 0; padding: 15px; }
          th { background: #4f46e5; color: white; }
        }
      </style>
    </head>
    <body>
      <h1>📊 Projets par Manager</h1>
      
      <div class="header-info">
        <p><strong>Date de génération :</strong> ${new Date().toLocaleDateString('fr-FR')} à ${new Date().toLocaleTimeString('fr-FR')}</p>
        <p><strong>Nombre de projets :</strong> ${this.filteredProjectsByManager.length}</p>
      </div>
      
      <div class="filters">
        <div class="filter-title">Filtres appliqués :</div>
        <span class="filter-badge">💰 ${this.filterFacture === 'factured' ? 'Facturés uniquement' : this.filterFacture === 'not-factured' ? 'Non facturés uniquement' : 'Tous les projets'}</span>
        <span class="filter-badge">👔 ${this.selectedManager === 'all' ? 'Tous les managers' : this.selectedManager}</span>
      </div>
      
      <table>
        <thead>
          <tr>
            <th>Projet</th>
            <th>Client</th>
            <th>Manager</th>
            <th>Chef de projet</th>
            <th>Équipe</th>
            <th>Période</th>
            <th>Facturé</th>
            <th>Membres</th>
          </tr>
        </thead>
        <tbody>
          ${this.filteredProjectsByManager.map(p => `
            <tr>
              <td><strong>${this.escapeHtml(p.name)}</strong>${p.description ? '<br><span style="font-size:10px;color:#9ca3af;">' + this.escapeHtml(p.description.substring(0, 60)) + (p.description.length > 60 ? '...' : '') + '</span>' : ''}</td>
              <td>${this.escapeHtml(p.client)}</td>
              <td>${this.escapeHtml(p.managerName || '—')}</td>
              <td>${this.escapeHtml(p.chefProjetName || '—')}</td>
              <td>${this.escapeHtml(p.teamName || '—')}</td>
              <td style="font-size:11px;">${p.startDate ? new Date(p.startDate).toLocaleDateString('fr-FR') : '—'}<br>→<br>${p.endDate ? new Date(p.endDate).toLocaleDateString('fr-FR') : '—'}</td>
              <td class="${p.factured ? 'factured' : 'not-factured'}">${p.factured ? '✓ Facturé' : '✗ Non facturé'}</td>
              <td class="members-list">
                ${p.members.map(m => `<div>• ${this.escapeHtml(m.fullName)}<br><span style="font-size:10px;">${this.escapeHtml(m.profile)}</span></div>`).join('')}
              </td>
            </tr>
          `).join('')}
        </tbody>
      </table>
      
      <div class="footer">
        <p>Document généré automatiquement - DXC Platform - Rapport confidentiel</p>
      </div>
    </body>
    </html>
  `;
  
  // Télécharger directement en HTML (l'utilisateur pourra imprimer en PDF)
  const blob = new Blob([content], { type: 'text/html' });
  const url = URL.createObjectURL(blob);
  const a = document.createElement('a');
  a.href = url;
  a.download = `projets_manager_${new Date().toISOString().split('T')[0]}.html`;
  a.click();
  URL.revokeObjectURL(url);
  
  // Message d'information
  console.log('Fichier HTML téléchargé. Ouvrez-le et utilisez "Fichier > Imprimer" pour sauvegarder en PDF.');
}

// Helper pour échapper les caractères HTML
private escapeHtml(text: string): string {
  if (!text) return '';
  const div = document.createElement('div');
  div.textContent = text;
  return div.innerHTML;
}
}
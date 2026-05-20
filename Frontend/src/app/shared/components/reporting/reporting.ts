import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import {
  ReportingService,
  ProjectReportDto,
  TaskReportDto,
  UserReportDto,
  UserStatusReportDto,
  SelectOption
} from '../../../core/services/reporting.service';

type ReportType =
  | 'projects-global'
  | 'projects-overdue'
  | 'tasks-overdue'
  | 'users-no-profile'
  | 'users-by-status';

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

  get totalPages(): number {
    let totalItems = 0;
    switch (this.activeReport) {
      case 'projects-global': totalItems = this.projectsData.length; break;
      case 'projects-overdue': totalItems = this.overdueProjectsData.length; break;
      case 'tasks-overdue': totalItems = this.overdueTasksData.length; break;
      case 'users-no-profile': totalItems = this.usersNoProfileData.length; break;
      case 'users-by-status': totalItems = this.userStatusData?.users.length || 0; break;
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
    this.projectsData = [];
    this.overdueProjectsData = [];
    this.overdueTasksData = [];
    this.usersNoProfileData = [];
    this.userStatusData = null;
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
    }
  }

  applyFilters(): void {
    if (this.activeReport) {
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
}
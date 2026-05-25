import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';



// Interface pour les membres des projets par manager
export interface ManagerProjectMemberDto {
  id: number;
  fullName: string;
  email: string;
  profile: string;
  tjm: number;
  role: string;
}

// Interface pour les projets par manager
export interface ManagerProjectDto {
  id: number;
  name: string;
  client: string;
  status: string;
  description: string;
  startDate: string;
  endDate: string;
  teamName?: string;
  chefProjetName?: string;
  managerName?: string;
  factured: boolean;
  members: ManagerProjectMemberDto[];
}

export interface ProjectReportDto {
  id: number;
  nom: string;
  description: string;
  status: string;
  dateDebut: string;
  dateFinEstimee: string;
  dateFinReelle: string;
  chefProjetNom: string;
  chefProjetPrenom: string;
  teamNom: string;
  teamId: number;
  overdue: boolean;
  joursRetard: number;
}

export interface TaskReportDto {
  id: number;
  titre: string;
  description: string;
  status: string;
  priority: string;
  dateEcheance: string;
  joursRetard: number;
  assigneNom: string;
  assignePrenom: string;
  projectId: number;
  projectNom: string;
}

export interface UserReportDto {
  id: number;
  email: string;
  nom: string;
  prenom: string;
  role: string;
  active: boolean;
  locked: boolean;
  failedAttempts: number;
  passwordResetCount: number;
  profileLibelle: string;
}

export interface UserStatusReportDto {
  users: UserReportDto[];
  totalActive: number;
  totalInactive: number;
  totalLocked: number;
  topPasswordResetter: UserReportDto;
  maxPasswordResetCount: number;
}

export interface SelectOption {
  id: number;
  nom: string;
}

@Injectable({ providedIn: 'root' })
export class ReportingService {
  private base = 'http://localhost:8080/api/reporting';

  constructor(private http: HttpClient) {}

  getProjectsReport(year?: number, status?: string, teamId?: number): Observable<ProjectReportDto[]> {
    let params = new HttpParams();
    if (year) params = params.set('year', year.toString());
    if (status) params = params.set('status', status);
    if (teamId) params = params.set('teamId', teamId.toString());
    return this.http.get<ProjectReportDto[]>(`${this.base}/projects`, { params });
  }

  getOverdueProjects(): Observable<ProjectReportDto[]> {
    return this.http.get<ProjectReportDto[]>(`${this.base}/projects/overdue`);
  }

  getOverdueTasks(projectId?: number): Observable<TaskReportDto[]> {
    let params = new HttpParams();
    if (projectId) params = params.set('projectId', projectId.toString());
    return this.http.get<TaskReportDto[]>(`${this.base}/tasks/overdue`, { params });
  }

  getUsersWithoutProfile(): Observable<UserReportDto[]> {
    return this.http.get<UserReportDto[]>(`${this.base}/users/no-profile`);
  }

  getUsersByStatus(active?: boolean): Observable<UserStatusReportDto> {
    let params = new HttpParams();
    if (active !== undefined && active !== null) {
      params = params.set('active', active.toString());
    }
    return this.http.get<UserStatusReportDto>(`${this.base}/users/by-status`, { params });
  }

  getProjectsForSelect(): Observable<SelectOption[]> {
    return this.http.get<SelectOption[]>(`${this.base}/projects/select`);
  }

  getTeamsForSelect(): Observable<SelectOption[]> {
    return this.http.get<SelectOption[]>(`${this.base}/teams/select`);
  }

  // ──────────────────────────────────────────────────────────────
  // EXPORTS
  // ──────────────────────────────────────────────────────────────

  exportProjectsExcel(year?: number, status?: string, teamId?: number): Observable<Blob> {
    let params = new HttpParams();
    if (year) params = params.set('year', year.toString());
    if (status) params = params.set('status', status);
    if (teamId) params = params.set('teamId', teamId.toString());
    return this.http.get(`${this.base}/projects/export`, {
      params,
      responseType: 'blob'
    });
  }

  exportProjectsPdf(year?: number, status?: string, teamId?: number): Observable<Blob> {
    let params = new HttpParams();
    if (year) params = params.set('year', year.toString());
    if (status) params = params.set('status', status);
    if (teamId) params = params.set('teamId', teamId.toString());
    return this.http.get(`${this.base}/projects/export/pdf`, {
      params,
      responseType: 'blob'
    });
  }

  exportOverdueProjectsExcel(): Observable<Blob> {
    return this.http.get(`${this.base}/projects/overdue/export`, {
      responseType: 'blob'
    });
  }

  exportOverdueProjectsPdf(): Observable<Blob> {
    return this.http.get(`${this.base}/projects/overdue/export/pdf`, {
      responseType: 'blob'
    });
  }

  exportOverdueTasksExcel(projectId?: number): Observable<Blob> {
    let params = new HttpParams();
    if (projectId) params = params.set('projectId', projectId.toString());
    return this.http.get(`${this.base}/tasks/overdue/export`, {
      params,
      responseType: 'blob'
    });
  }

  exportOverdueTasksPdf(projectId?: number): Observable<Blob> {
    let params = new HttpParams();
    if (projectId) params = params.set('projectId', projectId.toString());
    return this.http.get(`${this.base}/tasks/overdue/export/pdf`, {
      params,
      responseType: 'blob'
    });
  }

  exportUsersNoProfileExcel(): Observable<Blob> {
    return this.http.get(`${this.base}/users/no-profile/export`, {
      responseType: 'blob'
    });
  }

  exportUsersNoProfilePdf(): Observable<Blob> {
    return this.http.get(`${this.base}/users/no-profile/export/pdf`, {
      responseType: 'blob'
    });
  }

  exportUsersByStatusExcel(active?: boolean): Observable<Blob> {
    let params = new HttpParams();
    if (active !== undefined && active !== null) {
      params = params.set('active', active.toString());
    }
    return this.http.get(`${this.base}/users/by-status/export`, {
      params,
      responseType: 'blob'
    });
  }

  exportUsersByStatusPdf(active?: boolean): Observable<Blob> {
    let params = new HttpParams();
    if (active !== undefined && active !== null) {
      params = params.set('active', active.toString());
    }
    return this.http.get(`${this.base}/users/by-status/export/pdf`, {
      params,
      responseType: 'blob'
    });
  }
  getProjectsByManager(): Observable<ManagerProjectDto[]> {
  return this.http.get<ManagerProjectDto[]>(`${this.base}/projects/by-manager`);
}
}
import { Injectable } from '@angular/core';
import { HttpClient, HttpParams, HttpResponse } from '@angular/common/http';
import { Observable } from 'rxjs';

export type ProjectStatus =
  | 'PRE_VALIDE'
  | 'EN_COURS'
  | 'EN_VALIDATION'
  | 'VALIDE'
  | 'REJETE'
  | 'CLOTURE';

export type RiskLevel = 'FAIBLE' | 'MOYEN' | 'ELEVE';

export interface ProjectDto {
  id?: number;
  name: string;
  description?: string;
  client: string;
  progressPercentage: number;
  status?: ProjectStatus;
  riskLevel: RiskLevel;
  startDate: string;
  endDate: string;
  teamId?: number;
  teamName?: string;
  deleted?: boolean;
  createdAt?: string;
  updatedAt?: string;
  managerId?: number;
  managerName?: string;

  managerComment?: string;
  reviewedAt?: string;
   chefProjetId?: number;
  chefProjetName?: string;
}

@Injectable({
  providedIn: 'root'
})
export class ProjectService {
  private readonly API_URL = 'http://localhost:8080/api/projects';

  constructor(private http: HttpClient) {}

  createProject(project: ProjectDto): Observable<HttpResponse<any>> {
    return this.http.post<any>(this.API_URL, project, {
      observe: 'response'
    });
  }

  updateProject(projectId: number, project: ProjectDto): Observable<HttpResponse<any>> {
    return this.http.put<any>(`${this.API_URL}/${projectId}`, project, {
      observe: 'response'
    });
  }

  getProjectById(projectId: number): Observable<ProjectDto> {
    return this.http.get<ProjectDto>(`${this.API_URL}/${projectId}`);
  }

  getAllProjects(query?: string, status?: string): Observable<ProjectDto[]> {
    let params = new HttpParams();

    if (query && query.trim()) {
      params = params.set('query', query.trim());
    }

    if (status && status.trim()) {
      params = params.set('status', status.trim());
    }

    return this.http.get<ProjectDto[]>(this.API_URL, { params });
  }

  getMyProjects(query?: string, status?: string): Observable<ProjectDto[]> {
    let params = new HttpParams();

    if (query && query.trim()) {
      params = params.set('query', query.trim());
    }

    if (status && status.trim()) {
      params = params.set('status', status.trim());
    }

    return this.http.get<ProjectDto[]>(`${this.API_URL}/my-projects`, { params });
  }

  setDeletedStatus(projectId: number, deleted: boolean): Observable<ProjectDto> {
    const params = new HttpParams().set('deleted', deleted.toString());
    return this.http.patch<ProjectDto>(`${this.API_URL}/${projectId}/deleted`, null, { params });
  }
  assignTeamToProject(projectId: number, teamId: number): Observable<ProjectDto> {
  return this.http.patch<ProjectDto>(`${this.API_URL}/${projectId}/assign-team?teamId=${teamId}`, {});
}
  
}
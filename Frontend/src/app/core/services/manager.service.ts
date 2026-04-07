import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface ManagerSelectDto {
  id: number;
  fullName: string;
  email?: string;
}

/* 🔹 Projets visibles par le manager */
export interface ManagerProjectItemDto {
  id: number;
  projectName: string;
  status: string;
  client: string;
  managerName: string | null;
  createdAt: string;
}

/* 🔹 Chef de projet pour select */
export interface ChefProjetSummary {
  id: number;
  fullName: string;
  email?: string;
}

/* 🔹 Review (validation / rejet) */
export type ManagerDecision = 'VALIDER' | 'REJETER';

export interface ManagerProjectReviewDto {
  projectId: number;
  chefProjetId: number;
  commentaire: string;
  decision: ManagerDecision;
}

@Injectable({
  providedIn: 'root'
})
export class ManagerService {

  private readonly API_URL = 'http://localhost:8080/api/projects';

  constructor(private http: HttpClient) {}

  /* ==============================
     🔹 Managers (déjà existant)
  ============================== */
  getManagersForSelect(): Observable<ManagerSelectDto[]> {
    return this.http.get<ManagerSelectDto[]>(`${this.API_URL}/managers/select`);
  }

  /* ==============================
     🔹 Liste projets manager
     GET /api/projects/manager
  ============================== */
  getManagerProjects(): Observable<ManagerProjectItemDto[]> {
    return this.http.get<ManagerProjectItemDto[]>(`${this.API_URL}/manager`);
  }

  /* ==============================
     🔹 Liste chefs de projet
     GET /api/projects/manager/chefs-projet/select
  ============================== */
  getChefsProjetForSelect(): Observable<ChefProjetSummary[]> {
    return this.http.get<ChefProjetSummary[]>(`${this.API_URL}/manager/chefs-projet/select`);
  }

  /* ==============================
     🔹 Validation / rejet projet
     POST /api/projects/manager/review
  ============================== */
  reviewProject(request: ManagerProjectReviewDto): Observable<any> {
    return this.http.post(`${this.API_URL}/manager/review`, request);
  }
}
import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface ManagerSelectDto {
  id: number;
  fullName: string;
  email?: string;
}

/* Projets visibles par le manager */
export interface ManagerProjectItemDto {
  id: number;
  projectName: string;
  status: string;
  client: string;
  managerName: string | null;
  createdAt: string;

  chefProjetId?: number | null;
  chefProjetName?: string | null;

  managerComment?: string | null;
  reviewedAt?: string | null;
}

/* Chef de projet pour select */
export interface ChefProjetSummary {
  id: number;
  fullName: string;
  email?: string;
  prenom?: string;
  nom?: string;
}

/* Review (validation / rejet) */
export type ManagerDecision = 'VALIDER' | 'REJETER';

export interface ManagerProjectReviewDto {
  projectId: number;
  chefProjetId?: number | null;
  commentaire: string;
  decision: ManagerDecision;
}

@Injectable({
  providedIn: 'root'
})
export class ManagerService {
  private readonly API_URL = 'http://localhost:8080/api/projects';

  constructor(private http: HttpClient) {}

  getManagersForSelect(): Observable<ManagerSelectDto[]> {
    return this.http.get<ManagerSelectDto[]>(`${this.API_URL}/managers/select`);
  }

  getManagerProjects(): Observable<ManagerProjectItemDto[]> {
    return this.http.get<ManagerProjectItemDto[]>(`${this.API_URL}/manager`);
  }

  getChefsProjetForSelect(): Observable<ChefProjetSummary[]> {
    return this.http.get<ChefProjetSummary[]>(`${this.API_URL}/manager/chefs-projet/select`);
  }

  reviewProject(request: ManagerProjectReviewDto): Observable<any> {
    return this.http.post(`${this.API_URL}/manager/review`, request);
  }
}
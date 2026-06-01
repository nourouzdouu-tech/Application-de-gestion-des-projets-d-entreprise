import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface RiskPredictionResult {
  projectId: number;
  projectName: string;
  level: 'FAIBLE' | 'MOYEN' | 'ELEVE';
  score: number;
  reason: string;
}

export interface RecommendedMember {
  userId: number;
  fullName: string;
  email: string;
  activeTasks: number;
  reason: string;
}

export interface TeamRecommendationResult {
  projectId: number;
  projectName: string;
  recommendedMembers: RecommendedMember[];
  aiJustification: string;
}

@Injectable({ providedIn: 'root' })
export class PredictionService {
  private http = inject(HttpClient);
  private readonly base = 'http://localhost:8080/api/projects';

    // Lecture du cache (rapide)
getProjectRisk(projectId: number): Observable<RiskPredictionResult> {
  return this.http.get<RiskPredictionResult>(`${this.base}/${projectId}/risk`);
}

  getAllRisks(): Observable<RiskPredictionResult[]> {
    return this.http.get<RiskPredictionResult[]>(`${this.base}/risks`);
  }

  getTeamRecommendation(projectId: number): Observable<TeamRecommendationResult> {
    return this.http.get<TeamRecommendationResult>(
      `${this.base}/${projectId}/team-recommendation`
    );
  }


// Force un nouvel appel IA (bouton "Actualiser")
forceRefreshRisk(projectId: number): Observable<RiskPredictionResult> {
  return this.http.post<RiskPredictionResult>(
    `${this.base}/${projectId}/risk/refresh`, {}
  );
}
}
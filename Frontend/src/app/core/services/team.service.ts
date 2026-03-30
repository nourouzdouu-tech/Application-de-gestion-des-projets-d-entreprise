import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface MemberInfo {
  id: number;
  fullName: string;
  email: string;
  enabled: boolean;
  roleName: string;
}
export interface UserSearchResult {
  id: number;
  fullName: string;
  email: string;
  roleName: string;
  alreadyInTeam?: boolean;  // Ajoutez cette propriété (optionnelle)
  teamName?: string;  
}

export interface TeamDto {
  id?: number;
  name: string;
  description?: string;
  projectManagerId?: number;
  projectManagerName?: string;
  members?: MemberInfo[];
  deleted?: boolean;
  createdAt?: string;
  updatedAt?: string;
}

export interface AssignUserToTeamRequest {
  userId: number;
}

@Injectable({
  providedIn: 'root'
})
export class TeamService {
  private readonly API_URL = 'http://localhost:8080/api/teams';

  constructor(private http: HttpClient) {}

  /** POST /api/teams — Créer une équipe */
  createTeam(team: TeamDto): Observable<TeamDto> {
    return this.http.post<TeamDto>(this.API_URL, team);
  }

  /** PUT /api/teams/:id — Mettre à jour une équipe */
  updateTeam(teamId: number, team: TeamDto): Observable<TeamDto> {
    return this.http.put<TeamDto>(`${this.API_URL}/${teamId}`, team);
  }

  /** POST /api/teams/:id/members — Affecter un utilisateur à une équipe */
  assignUserToTeam(teamId: number, userId: number): Observable<TeamDto> {
    const body: AssignUserToTeamRequest = { userId };
    return this.http.post<TeamDto>(`${this.API_URL}/${teamId}/members`, body);
  }

  /** GET /api/teams/:id — Récupérer une équipe par ID */
  getTeamById(teamId: number): Observable<TeamDto> {
    return this.http.get<TeamDto>(`${this.API_URL}/${teamId}`);
  }

  /** GET /api/teams — Récupérer toutes les équipes */
  getAllTeams(): Observable<TeamDto[]> {
    return this.http.get<TeamDto[]>(this.API_URL);
  }

  /** PATCH /api/teams/:id/deleted — Supprimer ou restaurer une équipe */
  setDeletedStatus(teamId: number, deleted: boolean): Observable<TeamDto> {
    const params = new HttpParams().set('deleted', deleted.toString());
    return this.http.patch<TeamDto>(`${this.API_URL}/${teamId}/deleted`, null, { params });
  }

  /** GET /api/teams/my-team — récupérer l'équipe du chef de projet connecté */
  getMyTeam(): Observable<TeamDto> {
    return this.http.get<TeamDto>(`${this.API_URL}/my-team`);
  }
  searchAvailableUsers(query: string): Observable<UserSearchResult[]> {
  const params = new HttpParams().set('query', query);
  return this.http.get<UserSearchResult[]>(
    `${this.API_URL}/users/search`, { params }
  );
}
// Dans team.service.ts
removeUserFromTeam(teamId: number, userId: number): Observable<TeamDto> {
  return this.http.delete<TeamDto>(`${this.API_URL}/${teamId}/members/${userId}`);
}
}
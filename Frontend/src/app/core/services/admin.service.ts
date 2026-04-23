import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface RoleSummary {
  id: number;
  nom: string;
}

export interface PermissionSummary {
  id: number;
  nom: string;
  description: string;
}

export interface RoleResponse {
  id: number;
  nom: string;
  description: string;
  active: boolean;
  permissions: PermissionSummary[];
  usersCount?: number;
}

export interface UserUpdateRequest {
  prenom: string;
  nom: string;
  email: string;
  genre: 'HOMME' | 'FEMME';
  roleCodes: string[]; 
  profileId: number;
}

export interface UserResponse {
  id: number;
  email: string;
  prenom: string;
  nom: string;
  genre: 'HOMME' | 'FEMME';
  failedAttempts: number;
  locked: boolean;
  mustChangePassword: boolean;
  roles: RoleSummary[];
  profileId?: number;
  profileLibelle?: string;
}

export interface UserCreateRequest {
  prenom: string;
  nom: string;
  email: string;
   genre: 'HOMME' | 'FEMME';
  roleCodes: string[]; 
  password: string;
  profileId: number;
}

export interface RoleCreateRequest {
  nom: string;
  description: string;
  permissionIds?: number[];
}

export interface RoleUpdateRequest {
  nom: string;
  description: string;
  active: boolean;
  permissionIds?: number[];
}

export interface DashboardStats {
  totalUsers: number;
  activeUsers: number;
  inactiveUsers: number;
  totalRoles: number;
  usersPerRole: { [roleId: string]: number };
}

export interface RoleStats {
  roleId: number;
  roleName: string;
  userCount: number;
  percentage: number;
}

export interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
}

export interface Representant {
  id?: number;
  nom: string;
  email: string;
  telephone: string;
}

export interface ClientResponse {
  id: number;
  nom: string;
  representantsCount: number;
  representants: Representant[];
}

export interface ClientCreateRequest {
  nom: string;
  representants: Representant[];
}

export interface ClientUpdateRequest {
  nom: string;
  representants: Representant[];
}

export interface ClientResponseOld {
  id: number;
  nom: string;
  email: string;
  telephone: string;
}

export interface ClientCreateRequestOld {
  nom: string;
  email: string;
  telephone: string;
}

export interface ClientUpdateRequestOld {
  nom: string;
  email: string;
  telephone: string;
}

@Injectable({ providedIn: 'root' })
export class AdminService {
  private http = inject(HttpClient);
  private apiUrl = 'http://localhost:8080/api/admin/users';
  private clientApiUrl = 'http://localhost:8080/api/admin/clients';

  getUsers(page: number, size = 5, q = '', role = '', locked?: boolean): Observable<PageResponse<UserResponse>> {
    let params = new HttpParams()
      .set('page', (page - 1).toString())
      .set('size', size.toString());

    if (q) params = params.set('q', q);
    if (role) params = params.set('role', role);
    if (locked !== undefined) params = params.set('locked', locked.toString());

    return this.http.get<PageResponse<UserResponse>>(this.apiUrl, { params });
  }

  createUser(req: UserCreateRequest): Observable<UserResponse> {
    return this.http.post<UserResponse>(this.apiUrl, req);
  }

  updateUser(id: number, req: UserUpdateRequest): Observable<UserResponse> {
    return this.http.put<UserResponse>(`${this.apiUrl}/${id}`, req);
  }

  deleteUser(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }

  disableUser(id: number): Observable<void> {
    return this.http.patch<void>(`${this.apiUrl}/${id}/disable`, {});
  }

  enableUser(id: number): Observable<void> {
    return this.http.patch<void>(`${this.apiUrl}/${id}/enable`, {});
  }

  getRoles(): Observable<RoleResponse[]> {
    return this.http.get<RoleResponse[]>('http://localhost:8080/api/admin/roles');
  }

  getDashboardStats(): Observable<DashboardStats> {
    return this.http.get<DashboardStats>('http://localhost:8080/api/admin/dashboard/stats');
  }

  getRoleStats(): Observable<RoleStats[]> {
    return this.http.get<RoleStats[]>('http://localhost:8080/api/admin/dashboard/role-stats');
  }

  getUsersByRole(roleId: number): Observable<UserResponse[]> {
    return this.http.get<UserResponse[]>(`http://localhost:8080/api/admin/roles/${roleId}/users`);
  }

  getActiveUserCount(): Observable<number> {
    return this.http.get<number>('http://localhost:8080/api/admin/dashboard/active-users');
  }

  getInactiveUserCount(): Observable<number> {
    return this.http.get<number>('http://localhost:8080/api/admin/dashboard/inactive-users');
  }

  createRole(req: RoleCreateRequest): Observable<RoleResponse> {
    return this.http.post<RoleResponse>('http://localhost:8080/api/admin/roles', req);
  }

  updateRole(id: number, req: RoleUpdateRequest): Observable<RoleResponse> {
    return this.http.put<RoleResponse>(`http://localhost:8080/api/admin/roles/${id}`, req);
  }

  deleteRole(id: number): Observable<void> {
    return this.http.delete<void>(`http://localhost:8080/api/admin/roles/${id}`);
  }

  getRole(id: number): Observable<RoleResponse> {
    return this.http.get<RoleResponse>(`http://localhost:8080/api/admin/roles/${id}`);
  }

  getPermissions(): Observable<PermissionSummary[]> {
    return this.http.get<PermissionSummary[]>('http://localhost:8080/api/admin/permissions');
  }

  createPermission(req: { nom: string; description: string }): Observable<PermissionSummary> {
    return this.http.post<PermissionSummary>('http://localhost:8080/api/admin/permissions', req);
  }

  getClients(page: number = 0, size: number = 10, q: string = ''): Observable<PageResponse<ClientResponse>> {
    let params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());

    if (q) params = params.set('q', q);

    return this.http.get<PageResponse<ClientResponse>>(this.clientApiUrl, { params });
  }

  createClient(req: ClientCreateRequest): Observable<ClientResponse> {
    return this.http.post<ClientResponse>(this.clientApiUrl, req);
  }

  getClientById(id: number): Observable<ClientResponse> {
    return this.http.get<ClientResponse>(`${this.clientApiUrl}/${id}`);
  }

  updateClient(id: number, req: ClientUpdateRequest): Observable<ClientResponse> {
    return this.http.put<ClientResponse>(`${this.clientApiUrl}/${id}`, req);
  }

  deleteClient(id: number): Observable<void> {
    return this.http.delete<void>(`${this.clientApiUrl}/${id}`);
  }
}
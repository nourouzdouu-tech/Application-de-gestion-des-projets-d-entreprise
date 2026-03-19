import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface LoginRequest {
  email: string;
  password: string;
}

export interface AuthResponse {
  accessToken: string;
  tokenType: string;
  email: string;
  prenom: string;
  nom: string;
  roles: string[];
  redirectTo: string;
  mustChangePassword: boolean;
}
export interface ChangePasswordRequest {
  oldPassword: string;
  newPassword: string;
  confirmPassword: string;
}
@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private apiUrl = 'http://localhost:8080/api/auth';

  constructor(private http: HttpClient) {}

  login(email: string, password: string): Observable<AuthResponse> {
    const loginRequest: LoginRequest = { email, password };
    return this.http.post<AuthResponse>(`${this.apiUrl}/login`, loginRequest);
  }

  logout(): Observable<any> {
    return this.http.post(`${this.apiUrl}/logout`, {});
  }

  saveToken(token: string): void {
    localStorage.setItem('auth_token', token);
  }

  getToken(): string | null {
    return localStorage.getItem('auth_token');
  }

  removeToken(): void {
    localStorage.removeItem('auth_token');
  }

  isAuthenticated(): boolean {
    return !!this.getToken();
  }
  changePassword(request: ChangePasswordRequest): Observable<any> {
  return this.http.post(
    `${this.apiUrl}/change-password`, 
    request
  );
}

unlockUser(email: string): Observable<any> {
  return this.http.post(
    `http://localhost:8080/api/admin/users/${email}/unlock`, 
    {}
  );
}
saveUser(user: AuthResponse): void {
  localStorage.setItem('auth_user', JSON.stringify({
    email: user.email,
    prenom: user.prenom,
    nom: user.nom,
    roles: user.roles
  }));
}

getUser(): { email: string; prenom: string; nom: string; roles: string[] } | null {
  const user = localStorage.getItem('auth_user');
  return user ? JSON.parse(user) : null;
}

getInitials(): string {
  const user = this.getUser();
  if (!user) return '??';
  return (user.prenom?.charAt(0) + user.nom?.charAt(0)).toUpperCase();
}

removeUser(): void {
  localStorage.removeItem('auth_user');
  localStorage.removeItem('auth_token');
}

}
import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface User {
  id: number;
  email: string;
  prenom: string;
  nom: string;
  roles: string[];
  profileId?: number;
  profileLibelle?: string;
  tjm?: number;
}

@Injectable({ providedIn: 'root' })
export class UserService {
  private apiUrl = 'http://localhost:8080/api/users';

  constructor(private http: HttpClient) {}

 getAllUsers(): Observable<User[]> {
  return this.http.get<User[]>('http://localhost:8080/api/users/list');
}
  getUserById(id: number): Observable<User> {
    return this.http.get<User>(`${this.apiUrl}/${id}`);
  }

searchAvailableUsers(query: string = ''): Observable<User[]> {
  // L'endpoint réel est dans TeamController : /api/teams/search-available-users
  return this.http.get<User[]>(`http://localhost:8080/api/teams/search-available-users?query=${query}`);
}

}
import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface ManagerSelectDto {
  id: number;
  fullName: string;
  email?: string;
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
}
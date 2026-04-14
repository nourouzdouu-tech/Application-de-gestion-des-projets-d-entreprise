import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export type TaskStatus = 'A_faire' | 'En_cours' | 'Terminé' | 'Validation';

export interface TaskDto {
  id?: number;
  title: string;
  description?: string;
  status: TaskStatus;
  createdAt?: string;
  dueDate?: string;
  assignedToId: number;
  assignedToName?: string;
  projectId: number;
  projectName?: string;
  deleted?: boolean;
  priority?: 'BASSE' | 'MOYENNE' | 'HAUTE';
}

@Injectable({ providedIn: 'root' })
export class TaskService {
  private apiUrl = 'http://localhost:8080/api/tasks';

  constructor(private http: HttpClient) {}

  createTask(task: TaskDto): Observable<TaskDto> {
    return this.http.post<TaskDto>(this.apiUrl, task);
  }

  getTasksByProject(projectId: number): Observable<TaskDto[]> {
  return this.http.get<TaskDto[]>(`${this.apiUrl}/project/${projectId}`);
}

  getMyTasks(): Observable<TaskDto[]> {
    return this.http.get<TaskDto[]>(`${this.apiUrl}/my-tasks`);
  }

  deleteTask(id: number): Observable<void> {
  return this.http.delete<void>(`${this.apiUrl}/${id}`);
}
  updateTask(id: number, task: TaskDto): Observable<TaskDto> {
  return this.http.put<TaskDto>(`${this.apiUrl}/${id}`, task);
}
  
}
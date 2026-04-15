import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
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

 getMyTasks(query?: string, priority?: string, assignedToId?: number): Observable<TaskDto[]> {
  let params = new HttpParams();

  if (query) {
    params = params.set('query', query);
  }

  if (priority && priority !== 'Toutes') {
    const backendPriorityMap: Record<string, string> = {
      Haute: 'HAUTE',
      Moyenne: 'MOYENNE',
      Basse: 'BASSE'
    };
    params = params.set('priority', backendPriorityMap[priority] ?? priority);
  }

  if (assignedToId) {
    params = params.set('assignedToId', assignedToId);
  }

  return this.http.get<TaskDto[]>(`${this.apiUrl}/my-tasks`, { params });
}

  updateMyTaskStatus(id: number, status: TaskStatus): Observable<TaskDto> {
    const params = new HttpParams().set('status', status);
    return this.http.patch<TaskDto>(`${this.apiUrl}/${id}/status`, null, { params });
  }

  deleteTask(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }

  updateTask(id: number, task: TaskDto): Observable<TaskDto> {
    return this.http.put<TaskDto>(`${this.apiUrl}/${id}`, task);
  }
}
import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface ClientSelectResponse {
  id: number;
  nom: string;
  representantsCount: number;
  representants: {
    id?: number;
    nom: string;
    email: string;
    telephone: string;
  }[];
}

@Injectable({
  providedIn: 'root'
})
export class ClientService {
  private http = inject(HttpClient);
  private apiUrl = 'http://localhost:8080/api/clients';

  getClientsForSelect(): Observable<ClientSelectResponse[]> {
    return this.http.get<ClientSelectResponse[]>(`${this.apiUrl}/select`);
  }
}
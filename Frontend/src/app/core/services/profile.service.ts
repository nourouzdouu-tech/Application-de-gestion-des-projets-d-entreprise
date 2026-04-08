import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface ProfileDto {
  id?: number;
  libelle: string;
  tjm: number;
  deleted?: boolean;
  createdAt?: string;
  updatedAt?: string;
}

@Injectable({
  providedIn: 'root'
})
export class ProfileService {
  private readonly API_URL = 'http://localhost:8080/api/admin/profiles';

  constructor(private http: HttpClient) {}

  getAllProfiles(): Observable<ProfileDto[]> {
    return this.http.get<ProfileDto[]>(this.API_URL);
  }

  createProfile(profile: ProfileDto): Observable<any> {
    return this.http.post(this.API_URL, profile, { responseType: 'text' as 'json' });
  }

  updateProfile(profileId: number, profile: ProfileDto): Observable<any> {
    return this.http.put(`${this.API_URL}/${profileId}`, profile, { responseType: 'text' as 'json' });
  }

  getProfileById(profileId: number): Observable<ProfileDto> {
    return this.http.get<ProfileDto>(`${this.API_URL}/${profileId}`);
  }

  setDeletedStatus(profileId: number, deleted: boolean): Observable<any> {
    const params = new HttpParams().set('deleted', deleted.toString());
    return this.http.patch(`${this.API_URL}/${profileId}/deleted`, null, { responseType: 'text' as 'json', params });
  }
}
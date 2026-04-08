import { Injectable } from '@angular/core';
import { Subject } from 'rxjs';

export interface ToastMessage {
  message: string;
  type: 'success' | 'error' | 'info';
}

@Injectable({ providedIn: 'root' })
export class NotificationService {
  private toastSubject = new Subject<ToastMessage | null>();
  toast$ = this.toastSubject.asObservable();

  showSuccess(message: string) {
    this.toastSubject.next({ message, type: 'success' });
    setTimeout(() => this.clear(), 3000);
  }

  showError(message: string) {
    this.toastSubject.next({ message, type: 'error' });
    setTimeout(() => this.clear(), 4000);
  }

  clear() {
    this.toastSubject.next(null);
  }
}
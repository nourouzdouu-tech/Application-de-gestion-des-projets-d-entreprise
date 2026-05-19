// notification-websocket.service.ts
import { Injectable } from '@angular/core';
import { Subject } from 'rxjs';
import { WebSocketService, WebSocketMessage } from './websocket.service';
import { NotificationService as ToastService } from './notification.service';

export interface AdminNotification {
  id: number;
  title: string;
  content: string;
  type: string;
  userId: number;
  userName: string;
  read: boolean;
  createdAt: string;
  actionUrl: string;
  metadata?: any;
}

@Injectable({ providedIn: 'root' })
export class NotificationWebSocketService {
  private notificationSubject = new Subject<AdminNotification>();
  private unreadCountSubject = new Subject<number>();
  
  notifications$ = this.notificationSubject.asObservable();
  unreadCount$ = this.unreadCountSubject.asObservable();

  constructor(
    private webSocketService: WebSocketService,
    private toastService: ToastService
  ) {
    this.listenToWebSocketMessages();
  }

  private listenToWebSocketMessages(): void {
    this.webSocketService.getMessages().subscribe((message: WebSocketMessage) => {
      // Vérifier si c'est une nouvelle notification
      if ((message as any).type === 'new_notification' && (message as any).notification) {
        const notification = (message as any).notification as AdminNotification;
        this.notificationSubject.next(notification);
        this.showNotificationToast(notification);
      }
      
      // Vérifier si c'est une mise à jour du compteur
      if ((message as any).type === 'unread_count' && (message as any).count !== undefined) {
        this.unreadCountSubject.next((message as any).count);
      }
    });
  }

  private showNotificationToast(notification: AdminNotification): void {
    const icon = this.getNotificationIcon(notification.type);
    const toastMessage = `${icon} ${notification.title}\n${notification.content}`;
    
    switch (notification.type) {
      case 'ACCOUNT_LOCKED':
        this.toastService.showError(toastMessage);
        break;
      case 'ACCOUNT_ENABLED':
        this.toastService.showSuccess(toastMessage);
        break;
      case 'TASK_VALIDATED':
        this.toastService.showSuccess(toastMessage);
        break;
      case 'TASK_REJECTED':
        this.toastService.showError(toastMessage);
        break;
      default:
        this.toastService.showSuccess(toastMessage);
    }
  }

  private getNotificationIcon(type: string): string {
    const icons: { [key: string]: string } = {
      'ACCOUNT_LOCKED': '🔒',
      'ACCOUNT_ENABLED': '✅',
      'TASK_ASSIGNED': '📋',
      'TASK_VALIDATED': '✅',
      'TASK_REJECTED': '❌',
      'BROADCAST': '📢',
      'PROJECT_VALIDATED': '📁'
    };
    return icons[type] || '🔔';
  }
}
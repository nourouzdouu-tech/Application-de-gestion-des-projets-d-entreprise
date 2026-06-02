// notification-bell-chef.component.ts
import { Component, OnInit, OnDestroy, HostListener } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Subscription } from 'rxjs';
import { NotificationApiService, Notification } from './notification-api.service';
import { NotificationWebSocketService } from './notification-websocket.service';
import { NotificationService as ToastService } from './notification.service';

@Component({
  selector: 'app-notification-bell-chef',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="notification-container">
      <button class="notification-btn" (click)="toggleDropdown()" [class.has-notifications]="unreadCount > 0">
        <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8">
          <path d="M15 17h5l-1.405-1.405A2.032 2.032 0 0 1 18 14.158V11a6 6 0 1 0-12 0v3.159c0 .538-.214 1.055-.595 1.436L4 17h5" stroke-linecap="round" stroke-linejoin="round"/>
          <path d="M13.73 21a2 2 0 0 1-3.46 0" stroke-linecap="round" stroke-linejoin="round"/>
        </svg>
        @if (unreadCount > 0) {
          <span class="badge">{{ unreadCount > 99 ? '99+' : unreadCount }}</span>
        }
      </button>
      
      @if (isOpen) {
        <div class="dropdown">
          <div class="dropdown-header">
            <h3>Notifications Chef de Projet</h3>
            @if (unreadCount > 0) {
              <button (click)="markAllAsRead($event)" class="mark-all-btn">
                <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.6" style="vertical-align: middle; margin-right:6px;">
                  <path d="M20 6L9 17l-5-5" stroke-linecap="round" stroke-linejoin="round"/>
                </svg>
                Tout lire
              </button>
            }
          </div>
          
          <div class="dropdown-content">
            @if (loading && notifications.length === 0) {
              <div class="loading">Chargement...</div>
            }
            
            @if (!loading && notifications.length === 0) {
              <div class="no-notifications">
                <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.6" style="vertical-align: middle; margin-right:6px;">
                  <path d="M15 17h5l-1.405-1.405A2.032 2.032 0 0 1 18 14.158V11a6 6 0 1 0-12 0v3.159c0 .538-.214 1.055-.595 1.436L4 17h5" stroke-linecap="round" stroke-linejoin="round"/>
                  <path d="M13.73 21a2 2 0 0 1-3.46 0" stroke-linecap="round" stroke-linejoin="round"/>
                </svg>
                Aucune notification
              </div>
            }
            
            @for (notif of notifications; track notif.id) {
              <div class="notification-item" 
                   [class.unread]="!notif.read"
                   (click)="onNotificationClick(notif)">
                <div class="notification-content">
                  <div class="notification-title">{{ notif.title }}</div>
                  <div class="notification-message">{{ notif.content }}</div>
                  <div class="notification-time">{{ getFormattedDate(notif.createdAt) }}</div>
                </div>
                @if (!notif.read) {
                  <div class="notification-badge">Nouveau</div>
                }
              </div>
            }
          </div>
          
          @if (hasMore) {
            <div class="dropdown-footer">
              <button (click)="loadMore()" class="load-more-btn">Voir plus...</button>
            </div>
          }
        </div>
      }
    </div>
  `,
  styles: [`
    .notification-container {
      position: relative;
      display: inline-block;
    }
    .notification-btn {
      position: relative;
      background: none;
      border: none;
      font-size: 24px;
      cursor: pointer;
      padding: 8px;
      border-radius: 50%;
      transition: background 0.3s;
    }
    .notification-btn:hover {
      background: rgba(0,0,0,0.1);
    }
    .badge {
      position: absolute;
      top: -5px;
      right: -5px;
      background: #f44336;
      color: white;
      border-radius: 50%;
      padding: 2px 6px;
      font-size: 11px;
      min-width: 18px;
      text-align: center;
      font-weight: bold;
    }
    .dropdown {
      position: absolute;
      top: 100%;
      right: 0;
      width: 420px;
      max-height: 520px;
      background: white;
      border-radius: 12px;
      box-shadow: 0 4px 20px rgba(0,0,0,0.15);
      z-index: 1000;
      display: flex;
      flex-direction: column;
      animation: slideDown 0.2s ease;
    }
    @keyframes slideDown {
      from {
        opacity: 0;
        transform: translateY(-10px);
      }
      to {
        opacity: 1;
        transform: translateY(0);
      }
    }
    .dropdown-header {
      padding: 12px 16px;
      border-bottom: 1px solid #e0e0e0;
      display: flex;
      justify-content: space-between;
      align-items: center;
      background: #fafafa;
      border-radius: 12px 12px 0 0;
    }
    .dropdown-header h3 {
      margin: 0;
      font-size: 16px;
      font-weight: 600;
    }
    .mark-all-btn {
      background: none;
      border: none;
      color: #1976d2;
      cursor: pointer;
      font-size: 12px;
    }
    .dropdown-content {
      flex: 1;
      overflow-y: auto;
      max-height: 420px;
    }
    .notification-item {
      padding: 12px 16px;
      border-bottom: 1px solid #f0f0f0;
      display: flex;
      gap: 12px;
      cursor: pointer;
      transition: background 0.2s;
      position: relative;
    }
    .notification-item:hover {
      background: #f5f5f5;
    }
    .notification-item.unread {
      background: #e3f2fd;
    }
    .notification-icon {
      font-size: 24px;
    }
    .notification-content {
      flex: 1;
    }
    .notification-title {
      font-weight: bold;
      margin-bottom: 4px;
      font-size: 14px;
    }
    .notification-message {
      font-size: 13px;
      color: #666;
      margin-bottom: 4px;
      line-height: 1.4;
    }
    .notification-time {
      font-size: 11px;
      color: #999;
    }
    .notification-badge {
      position: absolute;
      top: 12px;
      right: 12px;
      background: #1976d2;
      color: white;
      font-size: 10px;
      padding: 2px 6px;
      border-radius: 10px;
    }
    .no-notifications, .loading {
      padding: 40px;
      text-align: center;
      color: #999;
    }
    .dropdown-footer {
      padding: 10px;
      border-top: 1px solid #e0e0e0;
      text-align: center;
    }
    .load-more-btn {
      background: none;
      border: none;
      color: #1976d2;
      cursor: pointer;
    }
  `]
})
export class NotificationBellChefComponent implements OnInit, OnDestroy {
  isOpen = false;
  notifications: Notification[] = [];
  unreadCount = 0;
  currentPage = 0;
  hasMore = true;
  loading = false;
  
  private subscriptions: Subscription[] = [];

  constructor(
    private notificationApi: NotificationApiService,
    private notificationWS: NotificationWebSocketService,
    private toastService: ToastService
  ) {}

  ngOnInit(): void {
    console.log('=== NotificationBellChefComponent INIT ===');
    this.loadNotifications();
    this.loadUnreadCount();
    
    // Écouter les nouvelles notifications
    this.subscriptions.push(
      this.notificationWS.notifications$.subscribe((notification: Notification) => {
        console.log('Nouvelle notification chef reçue:', notification);
        this.notifications = [notification, ...this.notifications];
        this.unreadCount++;
      })
    );
    
    this.subscriptions.push(
      this.notificationWS.unreadCount$.subscribe((count: number) => {
        console.log('Nouveau compteur chef:', count);
        this.unreadCount = count;
      })
    );
  }
  
  ngOnDestroy(): void {
    this.subscriptions.forEach(sub => sub.unsubscribe());
  }
  
  loadNotifications(): void {
    console.log('Chargement des notifications chef...');
    this.loading = true;
    this.notificationApi.getNotifications(this.currentPage, 20).subscribe({
      next: (response: any) => {
        console.log('Notifications chef chargées:', response);
        this.notifications = response.content || [];
        this.hasMore = !response.last;
        this.loading = false;
      },
      error: (error: any) => {
        console.error('Erreur chargement:', error);
        this.loading = false;
      }
    });
  }
  
  loadUnreadCount(): void {
    console.log('Chargement du compteur chef...');
    this.notificationApi.getUnreadCount().subscribe({
      next: (data: any) => {
        console.log('Compteur chef:', data);
        this.unreadCount = data.count;
      },
      error: (error: any) => console.error('Erreur:', error)
    });
  }
  
  loadMore(): void {
    if (!this.hasMore || this.loading) return;
    this.currentPage++;
    this.loading = true;
    this.notificationApi.getNotifications(this.currentPage, 20).subscribe({
      next: (response: any) => {
        this.notifications = [...this.notifications, ...(response.content || [])];
        this.hasMore = !response.last;
        this.loading = false;
      },
      error: (error: any) => {
        console.error('Erreur:', error);
        this.loading = false;
      }
    });
  }
  
  toggleDropdown(): void {
    this.isOpen = !this.isOpen;
    if (this.isOpen) {
      if (this.notifications.length === 0) {
        this.loadNotifications();
      }
      this.loadUnreadCount();
    }
  }
  
  @HostListener('document:click', ['$event'])
  onClickOutside(event: Event): void {
    const target = event.target as HTMLElement;
    if (!target.closest('.notification-container')) {
      this.isOpen = false;
    }
  }
  
  onNotificationClick(notification: Notification): void {
    console.log('Click sur notification chef:', notification.id);
    if (!notification.read) {
      this.markAsRead(notification.id);
    }
    if (notification.actionUrl) {
      window.location.href = notification.actionUrl;
    }
    this.isOpen = false;
  }
  
  markAsRead(id: number): void {
    this.notificationApi.markAsRead(id).subscribe({
      next: () => {
        const notif = this.notifications.find(n => n.id === id);
        if (notif && !notif.read) {
          notif.read = true;
          this.unreadCount--;
        }
      },
      error: (error: any) => console.error('Erreur:', error)
    });
  }
  
  markAllAsRead(event: Event): void {
    event.stopPropagation();
    this.notificationApi.markAllAsRead().subscribe({
      next: () => {
        this.notifications.forEach(n => n.read = true);
        this.unreadCount = 0;
        this.toastService.showSuccess('Toutes les notifications ont été marquées comme lues');
      },
      error: (error: any) => console.error('Erreur:', error)
    });
  }
  
  getFormattedDate(dateString: string): string {
    if (!dateString) return '';
    const date = new Date(dateString);
    const now = new Date();
    const diffMs = now.getTime() - date.getTime();
    const diffMins = Math.floor(diffMs / 60000);
    const diffHours = Math.floor(diffMs / 3600000);
    const diffDays = Math.floor(diffMs / 86400000);
    
    if (diffMins < 1) return 'À l\'instant';
    if (diffMins < 60) return `Il y a ${diffMins} min`;
    if (diffHours < 24) return `Il y a ${diffHours} h`;
    if (diffDays === 1) return 'Hier';
    if (diffDays < 7) return `Il y a ${diffDays} jours`;
    return date.toLocaleDateString('fr-FR');
  }
  
  getIcon(type: string): string {
    const icons: {[key: string]: string} = {
      'TASK_PENDING_VALIDATION': '📋',
      'TASK_VALIDATED': '✅',
      'TASK_REJECTED': '❌',
      'PROJECT_ASSIGNED': '📁',
      'TASK_ASSIGNED_TO_MEMBER': '👥',
      'ACCOUNT_LOCKED': '🔒',
      'NEW_MESSAGE': '💬',      // ✅ AJOUT
        'NEW_FILE_RECEIVED': '📎'
    };
    return icons[type] || '🔔';
  }
}
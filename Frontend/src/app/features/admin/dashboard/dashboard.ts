import { Component, OnInit, inject, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { AdminService, PageResponse, UserResponse } from '../../../core/services/admin.service';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './dashboard.html',
  styleUrl: './dashboard.css'
})
export class Dashboard implements OnInit {
  private adminService = inject(AdminService);
  
  // Signals pour les données du backend
  users = signal<UserResponse[]>([]);
  totalUsers = signal(0);
  activeUsers = signal(0);
  inactiveUsers = signal(0);
  loading = signal(false);

  // KPI Data - sera mis à jour avec les vraies données
  kpiData = computed(() => [
    { title: 'Total Utilisateurs', value: this.totalUsers().toString(), icon: '👥' },
    { title: 'Utilisateurs Actifs', value: this.activeUsers().toString(), icon: '⚡' },
    { title: 'Utilisateurs Inactifs', value: this.inactiveUsers().toString(), icon: '🚫' },
    { title: 'Taux de Validation', value: '94.2%', icon: '✓' },
    { title: 'Taux de Rejet', value: '5.8%', icon: '✕' },
    { title: 'Charge Moyenne', value: '72%', icon: '⚙️' }
  ]);

  userLoadData = [
    { name: 'Michael S. (DevOps)', percentage: 84 },
    { name: 'Sarah J. (Frontend)', percentage: 72 },
    { name: 'Kevin L. (Security)', percentage: 48 },
    { name: 'Anna W. (Backend)', percentage: 81 }
  ];

  alertData = computed(() =>
    this.users().slice(0, 4).map((user, index) => ({
      name: `${user.prenom} ${user.nom}`,
      role: user.roles[0]?.nom || 'N/A',
      project: ['DXC Cloud Platform', 'Cyber Shield v2', 'Data Pipeline X', 'Infrastructure Auto'][index],
      status: ['Critical Load', 'Optimal', 'Warning Load', 'Validation Alert'][index],
      lastValidation: ['2 mins ago', '1 hour ago', '14 mins ago', 'Just now'][index]
    }))
  );

  monthlyData = [
    { month: 'JAN', users: 100 },
    { month: 'FEB', users: 150 },
    { month: 'MAR', users: 200 },
    { month: 'APR', users: 180 },
    { month: 'MAY', users: 220 },
    { month: 'JUN', users: 210 },
    { month: 'JUL', users: 280 }
  ];

  chartData = [
    { week: 'W1', valid: 85, reject: 15 },
    { week: 'W2', valid: 88, reject: 12 },
    { week: 'W3', valid: 82, reject: 18 },
    { week: 'W4', valid: 90, reject: 10 }
  ];

  ngOnInit() {
    this.loadDashboardData();
  }

  loadDashboardData() {
    this.loading.set(true);
    
    // Charger tous les utilisateurs (page 1, 100 par page pour avoir les stats)
    this.adminService.getUsers(1, 100).subscribe({
      next: (response: PageResponse<UserResponse>) => {
        this.users.set(response.content);
        this.totalUsers.set(response.totalElements);
        
        // Calculer les utilisateurs actifs et inactifs
        const active = response.content.filter(u => !u.locked).length;
        const inactive = response.content.filter(u => u.locked).length;
        
        this.activeUsers.set(active);
        this.inactiveUsers.set(inactive);
        this.loading.set(false);
      },
      error: (err) => {
        console.error('Erreur lors du chargement des utilisateurs:', err);
        this.loading.set(false);
      }
    });
  }

  exportData() {
    console.log('Export data initiated - Total users:', this.totalUsers());
    // Vous pouvez ajouter la logique d'export ici
  }

  getStatusClass(status: string): string {
    switch (status.toLowerCase()) {
      case 'critical load': return 'status-critical';
      case 'warning load': return 'status-warning';
      case 'validation alert': return 'status-alert';
      case 'optimal': return 'status-optimal';
      default: return 'status-default';
    }
  }

  getChartHeight(valid: number): string {
    return `${valid * 2}px`;
  }

  getChartRejectHeight(reject: number): string {
    return `${reject * 2}px`;
  }
  getUserLoad(user: UserResponse): number {
  if (user.locked) return 95;
  if (user.failedAttempts >= 3) return 80;
  if (user.mustChangePassword) return 65;
  return 45;
}
getUserInitials(user: UserResponse): string {
  return (user.prenom?.charAt(0) + user.nom?.charAt(0)).toUpperCase();
}

getRoleNames(user: UserResponse): string {
  return user.roles?.map(r => r.nom).join(', ') || 'Sans rôle';
}



getUserStatus(user: UserResponse): string {
  if (user.locked) return 'Critical';
  if (user.failedAttempts >= 3) return 'Warning';
  return 'Optimal';
}

getUserStatusClass(user: UserResponse): string {
  if (user.locked) return 'status-critical';
  if (user.failedAttempts >= 3) return 'status-warning';
  return 'status-optimal';
}
}
import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterModule } from '@angular/router';
import { AuthService } from '../../core/services/auth.service';

interface SidebarItem {
  label: string;
  route: string;
  badge?: number;
  exact?: boolean;
}

@Component({
  selector: 'app-manager-layout',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './manager.html',
  styleUrl: './manager.css',
})
export class ManagerLayoutComponent {
  private authService = inject(AuthService);
  private router = inject(Router);

  readonly menu: SidebarItem[] = [
    { label: 'Tableau de bord', route: '/manager/projets' },
    { label: 'Calendrier', route: '/manager/projets' },
    { label: 'Messages', route: '/manager/projets', badge: 3 },
    { label: 'Revue des projets', route: '/manager/projets', exact: true },
  ];

  currentUser() {
    return this.authService.getUser();
  }

  getInitials(): string {
    return this.authService.getInitials();
  }

  logout(): void {
    this.authService.logout().subscribe({
      next: () => {
        this.authService.removeUser();
        this.router.navigateByUrl('/login');
      },
      error: () => {
        this.authService.removeUser();
        this.router.navigateByUrl('/login');
      }
    });
  }
}
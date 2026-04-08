import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterModule } from '@angular/router';
import { AuthService } from '../../core/services/auth.service';

@Component({
  selector: 'app-admin',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './admin.html',
  styleUrl: './admin.css'
})
export class Admin {

  private router = inject(Router);
  authService = inject(AuthService);

  currentUser = signal(this.authService.getUser());

  logout() {
    this.authService.removeUser();
    this.router.navigate(['/login']);
  }

  getInitials(): string {
    const user = this.currentUser();
    if (!user) return '';

    return (user.prenom?.[0] || '') + (user.nom?.[0] || '');
  }
}
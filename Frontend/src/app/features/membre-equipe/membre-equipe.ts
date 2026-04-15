import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterModule } from '@angular/router';
import { AuthService } from '../../core/services/auth.service';

@Component({
  selector: 'app-membre-equipe',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './membre-equipe.html',
  styleUrls: ['./membre-equipe.css']
})
export class MembreEquipe {
  private router = inject(Router);
  authService = inject(AuthService);

  currentUser = signal<any>(this.authService.getUser());

  logout(): void {
    this.authService.removeUser();
    localStorage.removeItem('token');
    this.router.navigate(['/login']);
  }

  getInitials(): string {
    const user = this.currentUser();
    if (!user) return 'ME';
    return `${user.prenom?.[0] || ''}${user.nom?.[0] || ''}`.toUpperCase();
  }
}
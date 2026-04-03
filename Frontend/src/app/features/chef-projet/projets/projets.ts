import { Component, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, Router } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';

@Component({
  selector: 'app-projets',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './projets.html',
  styleUrl: './projets.css',
})
export class Projets {
  currentUser = signal<any>(null);
  
  // ✅ Rendre public (enlever private)
  constructor(
    public authService: AuthService,  // 'public' au lieu de 'private'
    private router: Router
  ) {
    this.currentUser.set(this.authService.getUser());
  }

  getInitials(): string {
    const user = this.currentUser();
    if (user?.prenom && user?.nom) {
      return (user.prenom.charAt(0) + user.nom.charAt(0)).toUpperCase();
    }
    return 'U';
  }

  logout(): void {
    this.authService.removeUser();
    this.router.navigate(['/login']);
  }
}
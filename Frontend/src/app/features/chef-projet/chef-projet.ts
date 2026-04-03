import { Component, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { AuthService } from '../../core/services/auth.service';

@Component({
  selector: 'app-chef-projet',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './chef-projet.html',
  styleUrls: ['./chef-projet.css']
})
export class ChefProjet {
  currentUser = signal<any>(null);

  constructor(private authService: AuthService) {
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
    window.location.href = '/login';
  }
}
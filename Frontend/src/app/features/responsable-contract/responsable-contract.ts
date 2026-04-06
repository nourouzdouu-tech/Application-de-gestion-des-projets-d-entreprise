import { Component, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterModule } from '@angular/router';
import { AuthService } from '../../core/services/auth.service';

@Component({
  selector: 'app-responsable-contract',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './responsable-contract.html',
  styleUrls: ['./responsable-contract.css']
})
export class ResponsableContract {
  currentUser = signal<any>(null);

  constructor(
    private authService: AuthService,
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
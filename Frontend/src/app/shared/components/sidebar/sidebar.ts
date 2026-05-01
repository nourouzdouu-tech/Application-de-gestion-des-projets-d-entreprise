import { Component, inject, signal, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterModule } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';
import { HasRoleDirective } from '../../directives/has-role.directive';

@Component({
  selector: 'app-sidebar',
  standalone: true,
  imports: [CommonModule, RouterModule, HasRoleDirective, FormsModule],
  templateUrl: './sidebar.html',
  styleUrl: './sidebar.css'
})
export class SidebarComponent implements OnInit {
  private router = inject(Router);
  authService = inject(AuthService);

  currentUser = signal<any>(this.authService.getUser());
  showProfileModal = signal(false);
  mustChangePassword = signal(false);

  // Tab: 'info' | 'password'
  activeTab = signal<'info' | 'password'>('info');

  // Password fields
  oldPassword = signal('');
  newPassword = signal('');
  confirmPassword = signal('');

  // Eye toggles (plain booleans)
  showOld = false;
  showNew = false;
  showConfirm = false;

  // Toast
  toastMessage = signal('');
  toastType = signal<'success' | 'error'>('success');
  showToast = signal(false);

  ngOnInit(): void {
    const user = this.authService.getUser();
    // If admin reset the password → force open modal on password tab
    if (user?.mustChangePassword) {
      this.mustChangePassword.set(true);
      this.activeTab.set('password');
      this.showProfileModal.set(true);
    }
  }

  openProfileModal(): void {
    this.oldPassword.set('');
    this.newPassword.set('');
    this.confirmPassword.set('');
    this.showOld = false;
    this.showNew = false;
    this.showConfirm = false;
    this.activeTab.set(this.mustChangePassword() ? 'password' : 'info');
    this.showProfileModal.set(true);
  }

  closeProfileModal(): void {
    if (this.mustChangePassword()) {
      this.triggerToast('error', 'Vous devez changer votre mot de passe avant de continuer.');
      return;
    }
    this.showProfileModal.set(false);
  }

  setTab(tab: 'info' | 'password'): void {
    if (this.mustChangePassword()) return;
    this.activeTab.set(tab);
  }

  // ── Password strength ──────────────────────────────────────
  get strength(): 'weak' | 'medium' | 'strong' {
    const pw = this.newPassword();
    if (!pw || pw.length < 6) return 'weak';
    const checks = [
      pw.length >= 8,
      /[A-Z]/.test(pw),
      /[0-9]/.test(pw),
      /[^A-Za-z0-9]/.test(pw)
    ].filter(Boolean).length;
    if (checks >= 3) return 'strong';
    if (checks >= 2) return 'medium';
    return 'weak';
  }

  get strengthLabel(): string {
    return { weak: 'Faible', medium: 'Moyen', strong: 'Fort' }[this.strength];
  }

  get strengthColor(): string {
    return { weak: '#ef4444', medium: '#f59e0b', strong: '#16a34a' }[this.strength];
  }

  get strengthWidth(): string {
    return { weak: '33%', medium: '66%', strong: '100%' }[this.strength];
  }

  // ── Match helpers ──────────────────────────────────────────
  get passwordsMatch(): boolean {
    return this.newPassword() !== '' && this.newPassword() === this.confirmPassword();
  }

  get passwordsMismatch(): boolean {
    return this.confirmPassword() !== '' && this.newPassword() !== this.confirmPassword();
  }

  // ── Change password ────────────────────────────────────────
  changePassword(): void {
    if (!this.oldPassword() || !this.newPassword() || !this.confirmPassword()) {
      this.triggerToast('error', 'Veuillez remplir tous les champs.');
      return;
    }
    if (this.newPassword().length < 6) {
      this.triggerToast('error', 'Le mot de passe doit contenir au moins 6 caractères.');
      return;
    }
    if (!this.passwordsMatch) {
      this.triggerToast('error', 'Les mots de passe ne correspondent pas.');
      return;
    }

    this.authService.changePassword({
      oldPassword: this.oldPassword(),
      newPassword: this.newPassword(),
      confirmPassword: this.confirmPassword()
    }).subscribe({
      next: () => {
        // Persist mustChangePassword = false in localStorage
        const user = this.authService.getUser();
        if (user) {
          user.mustChangePassword = false;
          this.authService.saveUser(user as any);
          this.currentUser.set(this.authService.getUser());
        }
        this.mustChangePassword.set(false);
        this.showProfileModal.set(false);
        this.triggerToast('success', 'Mot de passe changé ! Votre compte est maintenant actif.');
      },
      error: (err) => {
        this.triggerToast('error', err.error?.message || 'Ancien mot de passe incorrect.');
      }
    });
  }

  // ── Auth ──────────────────────────────────────────────────
  logout(): void {
    this.authService.removeUser();
    localStorage.removeItem('token');
    this.router.navigate(['/login']);
  }

  getInitials(): string {
    const user = this.currentUser();
    if (!user) return 'U';
    return `${user.prenom?.[0] || ''}${user.nom?.[0] || ''}`.toUpperCase();
  }

  getRoleLabel(): string {
    const roles = this.authService.getRoles();
    if (!roles || roles.length === 0) return 'Utilisateur';
    return roles.join(', ');
  }

  private triggerToast(type: 'success' | 'error', msg: string): void {
    this.toastMessage.set(msg);
    this.toastType.set(type);
    this.showToast.set(true);
    setTimeout(() => this.showToast.set(false), 4000);
  }
}
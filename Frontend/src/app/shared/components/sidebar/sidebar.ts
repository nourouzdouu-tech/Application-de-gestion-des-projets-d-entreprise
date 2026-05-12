// sidebar.ts
import { Component, inject, signal, OnInit, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterModule } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';
import { DomSanitizer, SafeHtml } from '@angular/platform-browser';

interface NavItem {
  path: string;
  label: string;
  icon: string;
  roles: string[];
  badge?: number;
  isGeneric?: boolean;
  alternativePaths?: { path: string; roles: string[] }[];
}

@Component({
  selector: 'app-sidebar',
  standalone: true,
  imports: [CommonModule, RouterModule, FormsModule],
  templateUrl: './sidebar.html',
  styleUrl: './sidebar.css'
})
export class SidebarComponent implements OnInit {
  private router = inject(Router);
  private sanitizer = inject(DomSanitizer);
  authService = inject(AuthService);

  currentUser = signal<any>(this.authService.getUser());
  showProfileModal = signal(false);
  mustChangePassword = signal(false);

  activeTab = signal<'info' | 'password'>('info');

  oldPassword = signal('');
  newPassword = signal('');
  confirmPassword = signal('');

  showOld = false;
  showNew = false;
  showConfirm = false;

  toastMessage = signal('');
  toastType = signal<'success' | 'error'>('success');
  showToast = signal(false);

  private allItems: NavItem[] = [
    { 
      path: '/dashboard', 
      label: 'Tableau de bord', 
      icon: 'dashboard', 
      roles: ['ADMIN', 'MANAGER', 'CHEF_PROJET', 'MEMBRE_EQUIPE', 'RESPONSABLE_CONTRAT'],
      isGeneric: true,
      alternativePaths: [
        { path: '/admin/dashboard', roles: ['ADMIN'] },
        { path: '/manager/dashboard', roles: ['MANAGER'] },
        { path: '/chef-projet/dashboard', roles: ['CHEF_PROJET'] },
        { path: '/membre-equipe/dashboard', roles: ['MEMBRE_EQUIPE'] },
        { path: '/responsable-contrat/dashboard', roles: ['RESPONSABLE_CONTRAT'] }
      ]
    },
    { path: '/admin/utilisateurs', label: 'Gestion des Utilisateurs', icon: 'users', roles: ['ADMIN'] },
    { path: '/admin/roles', label: 'Gestion des Rôles', icon: 'shield', roles: ['ADMIN'] },
    { path: '/admin/clients', label: 'Gestion des Clients', icon: 'clients', roles: ['ADMIN'] },
    { path: '/admin/profile', label: 'Gestion des Profils', icon: 'profile', roles: ['ADMIN'] },
    { path: '/manager/projets', label: 'Revue des projets', icon: 'projects', roles: ['MANAGER'] },
    { path: '/chef-projet/projets', label: 'Mes Projets', icon: 'projects', roles: ['CHEF_PROJET'] },
    { path: '/chef-projet/equipes', label: "Gestion d'Équipe", icon: 'team', roles: ['CHEF_PROJET'] },
    { path: '/membre-equipe/taches', label: 'Mes Tâches', icon: 'tasks', roles: ['MEMBRE_EQUIPE'] },
    { path: '/membre-equipe/projets', label: 'Mes Projets', icon: 'projects', roles: ['MEMBRE_EQUIPE'] },
    { path: '/responsable-contrat/projets', label: 'Projets', icon: 'projects', roles: ['RESPONSABLE_CONTRAT'] },
    // UN SEUL AUDIT - route unique qui gérera les deux types via des onglets dans le composant
    { 
      path: '/audit', 
      label: 'Audit', 
      icon: 'audit', 
      roles: ['ADMIN', 'MANAGER'],
      isGeneric: true,
      alternativePaths: [
        { path: '/admin/audit', roles: ['ADMIN'] },
        { path: '/manager/audit', roles: ['MANAGER'] }
      ]
    },
    { 
      path: '/reporting', 
      label: 'Reporting', 
      icon: 'reporting', 
      roles: ['ADMIN', 'MANAGER', 'CHEF_PROJET', 'MEMBRE_EQUIPE', 'RESPONSABLE_CONTRAT'],
      isGeneric: true,
      alternativePaths: [
        { path: '/admin/reporting', roles: ['ADMIN'] },
        { path: '/manager/reporting', roles: ['MANAGER'] },
        { path: '/chef-projet/reporting', roles: ['CHEF_PROJET'] },
        { path: '/membre-equipe/reporting', roles: ['MEMBRE_EQUIPE'] },
        { path: '/responsable-contrat/reporting', roles: ['RESPONSABLE_CONTRAT'] }
      ]
    },
    { 
      path: '/calendrier', 
      label: 'Calendrier', 
      icon: 'calendar', 
      roles: ['MANAGER', 'CHEF_PROJET', 'MEMBRE_EQUIPE'],
      isGeneric: true,
      alternativePaths: [
        { path: '/manager/calendrier', roles: ['MANAGER'] },
        { path: '/chef-projet/calendrier', roles: ['CHEF_PROJET'] },
        { path: '/membre-equipe/calendrier', roles: ['MEMBRE_EQUIPE'] }
      ]
    },
    { 
      path: '/messagerie', 
      label: 'Messages', 
      icon: 'messages', 
      roles: ['MANAGER', 'CHEF_PROJET', 'MEMBRE_EQUIPE'],
      //badge: 3,
      isGeneric: true,
      alternativePaths: [
        { path: '/manager/messages', roles: ['MANAGER'] },
        { path: '/chef-projet/messages', roles: ['CHEF_PROJET'] },
        { path: '/membre-equipe/messages', roles: ['MEMBRE_EQUIPE'] }
      ]
    }
  ];

  filteredItems = computed(() => {
    const userRoles = this.authService.getRoles();
    if (!userRoles || userRoles.length === 0) return [];

    const visibleItems: NavItem[] = [];

    for (const item of this.allItems) {
      const hasAccess = item.roles.some(role => userRoles.includes(role));
      
      if (hasAccess) {
        let finalPath = item.path;
        
        if (item.isGeneric && item.alternativePaths && item.alternativePaths.length > 0) {
          for (const alt of item.alternativePaths) {
            if (alt.roles.some(role => userRoles.includes(role))) {
              finalPath = alt.path;
              break;
            }
          }
        }
        
        visibleItems.push({
          ...item,
          path: finalPath
        });
      }
    }

    return visibleItems;
  });

  ngOnInit(): void {
    const user = this.authService.getUser();
    if (user?.mustChangePassword) {
      this.mustChangePassword.set(true);
      this.activeTab.set('password');
      this.showProfileModal.set(true);
    }
  }

  // Retourne l'icône complète comme SafeHtml
  getIconSvg(iconName: string): SafeHtml {
    const icons: Record<string, string> = {
      dashboard: `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <rect x="3" y="3" width="7" height="7" rx="1"/>
                    <rect x="14" y="3" width="7" height="7" rx="1"/>
                    <rect x="3" y="14" width="7" height="7" rx="1"/>
                    <rect x="14" y="14" width="7" height="7" rx="1"/>
                  </svg>`,
      users: `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"/>
                <circle cx="9" cy="7" r="4"/>
                <path d="M23 21v-2a4 4 0 0 0-3-3.87"/>
                <path d="M16 3.13a4 4 0 0 1 0 7.75"/>
              </svg>`,
      shield: `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                 <path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"/>
               </svg>`,
      clients: `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"/>
                  <circle cx="12" cy="7" r="4"/>
                </svg>`,
      profile: `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"/>
                  <circle cx="12" cy="7" r="4"/>
                </svg>`,
      audit: `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M12 8v4l3 3M12 2a10 10 0 1 0 10 10"/>
              </svg>`,
      reporting: `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <line x1="12" y1="20" x2="12" y2="10"/>
                    <line x1="18" y1="20" x2="18" y2="4"/>
                    <line x1="6" y1="20" x2="6" y2="16"/>
                  </svg>`,
      projects: `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                   <path d="M3 7a2 2 0 0 1 2-2h3l2 2h9a2 2 0 0 1 2 2v8a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V7z"/>
                 </svg>`,
      team: `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
               <path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"/>
               <circle cx="9" cy="7" r="4"/>
               <path d="M23 21v-2a4 4 0 0 0-3-3.87"/>
               <path d="M16 3.13a4 4 0 0 1 0 7.75"/>
             </svg>`,
      tasks: `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <polyline points="9 11 12 14 22 4"/>
                <path d="M21 12v7a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h11"/>
              </svg>`,
      calendar: `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                   <rect x="3" y="4" width="18" height="18" rx="2"/>
                   <line x1="16" y1="2" x2="16" y2="6"/>
                   <line x1="8" y1="2" x2="8" y2="6"/>
                   <line x1="3" y1="10" x2="21" y2="10"/>
                 </svg>`,
      messages: `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                   <path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"/>
                 </svg>`
    };
    
    const svgString = icons[iconName] || `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                                            <circle cx="12" cy="12" r="10"/>
                                          </svg>`;
    
    return this.sanitizer.bypassSecurityTrustHtml(svgString);
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

  get passwordsMatch(): boolean {
    return this.newPassword() !== '' && this.newPassword() === this.confirmPassword();
  }

  get passwordsMismatch(): boolean {
    return this.confirmPassword() !== '' && this.newPassword() !== this.confirmPassword();
  }

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

  const roleLabels: Record<string, string> = {
    ADMIN: 'Administrateur système',
    CHEF_PROJET: 'Chef de projet',
    MANAGER: 'Manager',
    MEMBRE_EQUIPE: "Membre d'équipe",
    RESPONSABLE_CONTRAT: 'Responsable de contrat'
  };

  return roles.map(r => roleLabels[r] ?? r).join(', ');
}
  private triggerToast(type: 'success' | 'error', msg: string): void {
    this.toastMessage.set(msg);
    this.toastType.set(type);
    this.showToast.set(true);
    setTimeout(() => this.showToast.set(false), 4000);
  }
}
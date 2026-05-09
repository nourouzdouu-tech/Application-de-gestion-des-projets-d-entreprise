import { Component, inject, signal, computed, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AdminService, UserResponse, RoleResponse } from '../../../core/services/admin.service';
import { AuthService } from '../../../core/services/auth.service';
import { ProfileDto, ProfileService } from '../../../core/services/profile.service';
import { Router } from '@angular/router';
import { forkJoin } from 'rxjs';

@Component({
  selector: 'app-utilisateurs',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './utilisateurs.html',
  styleUrls: ['./utilisateurs.css']
})
export class Utilisateurs implements OnInit {
  private adminService = inject(AdminService);
  private profileService = inject(ProfileService);
  authService = inject(AuthService);
  private router = inject(Router);

  currentUser = signal(this.authService.getUser());

  // Données
  allUsers = signal<UserResponse[]>([]);  // Tous les utilisateurs (source)
  filteredUsersList = signal<UserResponse[]>([]);  // Utilisateurs après filtrage
  users = signal<UserResponse[]>([]);  // Gardé pour compatibilité
  
  // Pagination
  totalElements = signal(0);
  totalPages = signal(0);
  currentPage = signal(1);
  itemsPerPage = 5;
  loading = signal(false);
  
  // Filtres
  searchQuery = signal('');
  filterRole = signal('');
  filterStatus = signal<boolean | undefined>(undefined);
  filterProfile = signal<number | undefined>(undefined);
  
  // Stats
  totalUsersCount = signal(0);
  activeUsersCount = signal(0);
  inactiveUsersCount = signal(0);
  showProfileModal = signal(false);
  realTotalUsers = signal(0);

  profileNom = signal('');
  profilePrenom = signal('');
  profileEmail = signal('');
  profilePassword = signal('');

  activeCount = computed(() => this.activeUsersCount());
  lockedCount = computed(() => this.inactiveUsersCount());
  activeRolesCount = computed(() => this.roles().length);

  // Pagination pages
  pages = computed(() => {
    const total = this.totalPages();
    return Array.from({ length: total }, (_, i) => i + 1);
  });

  // Utilisateurs paginés (pour l'affichage)
  paginatedUsers = computed(() => {
    const start = (this.currentPage() - 1) * this.itemsPerPage;
    const end = start + this.itemsPerPage;
    return this.filteredUsersList().slice(start, end);
  });

  roles = signal<RoleResponse[]>([]);
  profiles = signal<ProfileDto[]>([]);

  // Modale création
  showModal = signal(false);
  newNom = signal('');
  newPrenom = signal('');
  newEmail = signal('');
  newPassword = signal('');
  newRoles = signal<string[]>([]);
  newGenre = signal<'HOMME' | 'FEMME'>('FEMME');
  editGenre = signal<'HOMME' | 'FEMME'>('FEMME');
  newProfileId = signal<number | null>(null);

  // Modale édition
  showEditModal = signal(false);
  editId = signal<number | null>(null);
  editNom = signal('');
  editPrenom = signal('');
  editEmail = signal('');
  editRoles = signal<string[]>([]);
  editProfileId = signal<number | null>(null);

  // Modale confirmation
  showConfirmModal = signal(false);
  confirmType = signal<'delete' | 'reset'>('delete');
  confirmMessage = signal('');
  confirmAction = signal<() => void>(() => {});

  // Toast
  toastMessage = signal('');
  toastType = signal<'success' | 'error'>('success');
  showToast = signal(false);

  oldPassword = signal('');
  newProfilePassword = signal('');
  confirmProfilePassword = signal('');

  ngOnInit() {
    this.loadUsers();
    this.loadGlobalStats();
    this.loadRoles();
    this.loadProfiles();
  }

  // Charge TOUS les utilisateurs depuis le backend
  loadUsers() {
    this.loading.set(true);
    
    this.adminService.getUsers(1, 1000, '', '', undefined, undefined).subscribe({
      next: (page) => {
        this.allUsers.set(page.content);
        this.realTotalUsers.set(page.totalElements);
        this.totalUsersCount.set(page.totalElements);
        this.applyFilters(); // Applique les filtres
        this.loading.set(false);
      },
      error: () => {
        this.loading.set(false);
        this.showError('Erreur lors du chargement des utilisateurs');
      }
    });
  }

  // Applique tous les filtres sur allUsers
  applyFilters() {
    let result = [...this.allUsers()];
    
    console.log('🔍 Application des filtres - Total:', result.length);
    
    // Filtre par profil
    if (this.filterProfile()) {
      result = result.filter(user => user.profileId === this.filterProfile());
      console.log('  - Filtre profil:', this.filterProfile(), '→', result.length);
    }
    
    // Filtre par rôle
    if (this.filterRole()) {
      result = result.filter(user => 
        user.roles.some(role => role.nom === this.filterRole())
      );
      console.log('  - Filtre rôle:', this.filterRole(), '→', result.length);
    }
    
    // Filtre par statut
    if (this.filterStatus() !== undefined) {
      result = result.filter(user => user.locked === this.filterStatus());
      console.log('  - Filtre statut:', this.filterStatus() ? 'Inactif' : 'Actif', '→', result.length);
    }
    
    // Filtre par recherche
    if (this.searchQuery()) {
      const query = this.searchQuery().toLowerCase();
      result = result.filter(user => 
        user.nom.toLowerCase().includes(query) ||
        user.prenom.toLowerCase().includes(query) ||
        user.email.toLowerCase().includes(query)
      );
      console.log('  - Filtre recherche:', this.searchQuery(), '→', result.length);
    }
    
    this.filteredUsersList.set(result);
    this.totalElements.set(result.length);
    this.totalPages.set(Math.ceil(result.length / this.itemsPerPage));
    
    // Réinitialiser la page si nécessaire
    if (this.currentPage() > this.totalPages() && this.totalPages() > 0) {
      this.currentPage.set(1);
    } else if (this.totalPages() === 0) {
      this.currentPage.set(1);
    }
  }

  loadGlobalStats() {
    forkJoin({
      total: this.adminService.getUsers(1, 1, '', '', undefined, undefined),
      active: this.adminService.getUsers(1, 1, '', '', false, undefined),
      inactive: this.adminService.getUsers(1, 1, '', '', true, undefined)
    }).subscribe({
      next: ({ total, active, inactive }) => {
        this.totalUsersCount.set(total.totalElements);
        this.activeUsersCount.set(active.totalElements);
        this.inactiveUsersCount.set(inactive.totalElements);
        this.realTotalUsers.set(total.totalElements);
      },
      error: (err) => console.error('Erreur chargement stats utilisateurs', err)
    });
  }

  // Méthodes de filtrage
  onFilterProfile(profileId: string) {
    console.log('📌 Filtre profil:', profileId);
    this.filterProfile.set(profileId ? Number(profileId) : undefined);
    this.currentPage.set(1);
    this.applyFilters();
  }

  onFilterRole(value: string) {
    this.filterRole.set(value);
    this.currentPage.set(1);
    this.applyFilters();
  }

  onFilterStatus(value: string) {
    if (value === 'true') {
      this.filterStatus.set(true);
    } else if (value === 'false') {
      this.filterStatus.set(false);
    } else {
      this.filterStatus.set(undefined);
    }
    this.currentPage.set(1);
    this.applyFilters();
  }

  onSearch(value: string) {
    this.searchQuery.set(value);
    this.currentPage.set(1);
    this.applyFilters();
  }

  setPage(page: number) {
    if (page >= 1 && page <= this.totalPages()) {
      this.currentPage.set(page);
    }
  }

  getRangeStart() {
    if (this.filteredUsersList().length === 0) return 0;
    return (this.currentPage() - 1) * this.itemsPerPage + 1;
  }

  getRangeEnd() {
    return Math.min(this.currentPage() * this.itemsPerPage, this.filteredUsersList().length);
  }

  // Clear filters
  clearSearch() {
    this.searchQuery.set('');
    this.currentPage.set(1);
    this.applyFilters();
  }

  clearRoleFilter() {
    this.filterRole.set('');
    this.currentPage.set(1);
    this.applyFilters();
  }

  clearStatusFilter() {
    this.filterStatus.set(undefined);
    this.currentPage.set(1);
    this.applyFilters();
  }

  clearProfileFilter() {
    this.filterProfile.set(undefined);
    this.currentPage.set(1);
    this.applyFilters();
  }

  clearAllFilters() {
    this.searchQuery.set('');
    this.filterRole.set('');
    this.filterStatus.set(undefined);
    this.filterProfile.set(undefined);
    this.currentPage.set(1);
    this.applyFilters();
  }

  loadRoles() {
    this.adminService.getRoles().subscribe({
      next: (roles) => this.roles.set(roles.filter(r => r.active)),
      error: (err) => console.error('Erreur chargement rôles', err)
    });
  }

  loadProfiles() {
    this.profileService.getAllProfiles().subscribe({
      next: (profiles) => this.profiles.set(profiles),
      error: (err) => console.error('Erreur chargement profils', err)
    });
  }

  toggleUserStatus(user: UserResponse) {
    if (user.locked) {
      this.adminService.enableUser(user.id).subscribe({
        next: () => {
          this.showSuccess('Utilisateur activé avec succès !');
          this.loadUsers();
          this.loadGlobalStats();
        },
        error: () => this.showError('Erreur lors de l\'activation')
      });
    } else {
      this.adminService.disableUser(user.id).subscribe({
        next: () => {
          this.showSuccess('Utilisateur désactivé avec succès !');
          this.loadUsers();
          this.loadGlobalStats();
        },
        error: () => this.showError('Erreur lors de la désactivation')
      });
    }
  }

  deleteUser(id: number) {
    this.confirmMessage.set('Voulez-vous vraiment supprimer cet utilisateur ? Cette action est irréversible.');
    this.confirmAction.set(() => {
      this.adminService.deleteUser(id).subscribe({
        next: () => {
          this.showSuccess('Utilisateur supprimé avec succès !');
          this.loadUsers();
          this.loadGlobalStats();
        },
        error: () => this.showError('Erreur lors de la suppression')
      });
    });
    this.confirmType.set('delete');
    this.showConfirmModal.set(true);
  }

  confirmYes() {
    this.confirmAction()();
    this.showConfirmModal.set(false);
  }

  confirmNo() {
    this.showConfirmModal.set(false);
  }

  openModal() {
    this.showModal.set(true);
  }

  closeModal() {
    this.showModal.set(false);
    this.newNom.set('');
    this.newPrenom.set('');
    this.newEmail.set('');
    this.newPassword.set('');
    this.newRoles.set([]);
    this.newGenre.set('FEMME');
    this.newProfileId.set(null);
  }

  submitModal() {
    if (!this.newNom() || !this.newPrenom() || !this.newEmail() ||
        !this.newPassword() || this.newRoles().length === 0 || !this.newGenre() || !this.newProfileId()) {
      this.showError('Veuillez remplir tous les champs (au moins un rôle)');
      return;
    }

    this.adminService.createUser({
      nom: this.newNom(),
      prenom: this.newPrenom(),
      email: this.newEmail(),
      password: this.newPassword(),
      roleCodes: this.newRoles(),
      genre: this.newGenre(),
      profileId: this.newProfileId()!
    }).subscribe({
      next: () => {
        this.closeModal();
        this.loadUsers();
        this.loadGlobalStats();
        this.showSuccess('Utilisateur créé avec succès !');
      },
      error: (err) => {
        if (err.status === 409) {
          this.showError('Cet email est déjà utilisé');
        } else if (err.status === 404) {
          this.showError('Rôle ou profil introuvable');
        } else {
          this.showError('Erreur lors de la création');
        }
      }
    });
  }

  resetPassword(user: UserResponse) {
    this.confirmMessage.set(
      `Voulez-vous réinitialiser le mot de passe de ${user.prenom} ${user.nom} ? Un mot de passe temporaire sera envoyé par email.`
    );
    this.confirmAction.set(() => {
      this.adminService.resetPassword(user.id).subscribe({
        next: () => {
          this.showSuccess(`Mot de passe temporaire envoyé à ${user.email}`);
          this.loadUsers();
        },
        error: () => {
          this.showError('Erreur lors de la réinitialisation du mot de passe');
        }
      });
    });
    this.confirmType.set('reset');
    this.showConfirmModal.set(true);
  }

  openEditModal(user: UserResponse) {
    this.editId.set(user.id);
    this.editNom.set(user.nom);
    this.editPrenom.set(user.prenom);
    this.editEmail.set(user.email);
    this.editGenre.set(user.genre);
    this.editRoles.set(user.roles.map(r => r.nom));
    this.editProfileId.set(user.profileId ?? null);
    this.showEditModal.set(true);
  }

  closeEditModal() {
    this.showEditModal.set(false);
    this.editId.set(null);
    this.editNom.set('');
    this.editPrenom.set('');
    this.editEmail.set('');
    this.editRoles.set([]);
    this.editGenre.set('FEMME');
    this.editProfileId.set(null);
  }

  submitEditModal() {
    if (!this.editNom() || !this.editPrenom() || !this.editEmail() ||
        this.editRoles().length === 0 || !this.editGenre() || !this.editProfileId()) {
      this.showError('Veuillez remplir tous les champs (au moins un rôle)');
      return;
    }

    this.adminService.updateUser(this.editId()!, {
      nom: this.editNom(),
      prenom: this.editPrenom(),
      email: this.editEmail(),
      genre: this.editGenre(),
      roleCodes: this.editRoles(),
      profileId: this.editProfileId()!
    }).subscribe({
      next: () => {
        this.closeEditModal();
        this.loadUsers();
        this.loadGlobalStats();
        this.showSuccess('Utilisateur modifié avec succès !');
      },
      error: (err) => {
        if (err.status === 409) {
          this.showError('Cet email est déjà utilisé');
        } else {
          this.showError('Erreur lors de la modification');
        }
      }
    });
  }

  showSuccess(msg: string) {
    this.toastMessage.set(msg);
    this.toastType.set('success');
    this.showToast.set(true);
    setTimeout(() => this.showToast.set(false), 3000);
  }

  showError(msg: string) {
    this.toastMessage.set(msg);
    this.toastType.set('error');
    this.showToast.set(true);
    setTimeout(() => this.showToast.set(false), 3000);
  }

  getRoleBadgeClass(role: string): string {
    switch (role.toUpperCase()) {
      case 'ADMINISTRATEUR':
      case 'ADMIN':
        return 'badge-admin';
      case 'CHEF_PROJET':
        return 'badge-chef';
      case 'MANAGER':
        return 'badge-manager';
      default:
        return 'badge-membre';
    }
  }

  getInitials(prenom: string, nom: string): string {
    return (prenom.charAt(0) + nom.charAt(0)).toUpperCase();
  }

  getAvatarColor(index: number): string {
    const colors = ['#E8F4FD', '#FFF0E8', '#F0F0FF', '#E8FFF0', '#FFF0F0', '#F5F0FF'];
    return colors[index % colors.length];
  }

  openProfile() {
    const user = this.currentUser();
    this.profileNom.set(user?.nom || '');
    this.profilePrenom.set(user?.prenom || '');
    this.profileEmail.set(user?.email || '');
    this.profilePassword.set('');
    this.showProfileModal.set(true);
    this.oldPassword.set('');
    this.newProfilePassword.set('');
    this.confirmProfilePassword.set('');
  }

  changeProfilePassword() {
    if (!this.oldPassword() || !this.newProfilePassword() || !this.confirmProfilePassword()) {
      this.showError('Veuillez remplir les champs mot de passe');
      return;
    }

    this.authService.changePassword({
      oldPassword: this.oldPassword(),
      newPassword: this.newProfilePassword(),
      confirmPassword: this.confirmProfilePassword()
    }).subscribe({
      next: () => {
        this.showSuccess('Mot de passe modifié avec succès');
        this.oldPassword.set('');
        this.newProfilePassword.set('');
        this.confirmProfilePassword.set('');
        this.closeProfile();
      },
      error: (err) => {
        this.showError(err.error?.message || 'Erreur lors du changement de mot de passe');
      }
    });
  }

  closeProfile() {
    this.showProfileModal.set(false);
  }

  updateProfile() {
    const data = {
      nom: this.profileNom(),
      prenom: this.profilePrenom(),
      email: this.profileEmail()
    };

    this.authService.updateProfile(data).subscribe({
      next: (res) => {
        this.authService.saveUser(res);
        this.currentUser.set({
          email: res.email,
          prenom: res.prenom,
          nom: res.nom,
          roles: res.roles
        });
        this.showSuccess('Profil modifié avec succès !');
        this.closeProfile();
      },
      error: (err) => {
        console.log('ERREUR BACKEND 👉', err);
        this.showError(err.error?.message || 'Erreur lors de la modification');
      }
    });
  }

  toggleRoleSelection(roleNom: string, event: Event) {
    const isChecked = (event.target as HTMLInputElement).checked;
    if (isChecked) {
      this.newRoles.set([...this.newRoles(), roleNom]);
    } else {
      this.newRoles.set(this.newRoles().filter(r => r !== roleNom));
    }
  }

  toggleEditRoleSelection(roleNom: string, event: Event) {
    const isChecked = (event.target as HTMLInputElement).checked;
    if (isChecked) {
      this.editRoles.set([...this.editRoles(), roleNom]);
    } else {
      this.editRoles.set(this.editRoles().filter(r => r !== roleNom));
    }
  }

  goToDashboard(): void {
    const roles = this.authService.getRoles();
    
    if (roles.includes('ADMIN')) {
      this.router.navigate(['/admin/dashboard']);
    } else if (roles.includes('CHEF_PROJET')) {
      this.router.navigate(['/chef-projet/dashboard']);
    } else if (roles.includes('MANAGER')) {
      this.router.navigate(['/manager/dashboard']);
    } else if (roles.includes('RESPONSABLE_CONTRAT')) {
      this.router.navigate(['/responsable-contrat/dashboard']);
    } else if (roles.includes('MEMBRE_EQUIPE')) {
      this.router.navigate(['/membre-equipe/dashboard']);
    } else {
      this.router.navigate(['/admin/dashboard']);
    }
  }

  getRoleLabel(roleNom: string): string {
    const role = this.roles().find(r => r.nom === roleNom);
    return role?.description || roleNom;
  }

  getProfileLabel(profileId: number): string {
    const profile = this.profiles().find(p => p.id === profileId);
    return profile?.libelle || '';
  }
}
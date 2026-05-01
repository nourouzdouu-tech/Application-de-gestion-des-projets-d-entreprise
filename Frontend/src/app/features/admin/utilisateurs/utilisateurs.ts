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

  users = signal<UserResponse[]>([]);
  totalElements = signal(0);
  totalPages = signal(0);
  currentPage = signal(1);
  searchQuery = signal('');
  filterRole = signal('');
  filterStatus = signal<boolean | undefined>(undefined);
  showProfileModal = signal(false);

  profileNom = signal('');
  profilePrenom = signal('');
  profileEmail = signal('');
  profilePassword = signal('');
  

  itemsPerPage = 5;
  loading = signal(false);

  totalUsersCount = signal(0);
  activeUsersCount = signal(0);
  inactiveUsersCount = signal(0);

  activeCount = computed(() => this.activeUsersCount());
  lockedCount = computed(() => this.inactiveUsersCount());
  activeRolesCount = computed(() => this.roles().length);
  pages = computed(() =>
    Array.from({ length: this.totalPages() }, (_, i) => i + 1)
  );

  roles = signal<RoleResponse[]>([]);
  profiles = signal<ProfileDto[]>([]);

  showModal = signal(false);
  newNom = signal('');
  newPrenom = signal('');
  newEmail = signal('');
  newPassword = signal('');
newRoles = signal<string[]>([]);
 newGenre = signal<'HOMME' | 'FEMME'>('FEMME');
editGenre = signal<'HOMME' | 'FEMME'>('FEMME');
  newProfileId = signal<number | null>(null);

  showEditModal = signal(false);
  editId = signal<number | null>(null);
  editNom = signal('');
  editPrenom = signal('');
  editEmail = signal('');
  editRoles = signal<string[]>([]);
  
  editProfileId = signal<number | null>(null);

  showConfirmModal = signal(false);
  confirmType = signal<'delete' | 'reset'>('delete');
  confirmMessage = signal('');
  confirmAction = signal<() => void>(() => {});

  toastMessage = signal('');
  toastType = signal<'success' | 'error'>('success');
  showToast = signal(false);

  ngOnInit() {
    this.loadUsers();
    this.loadGlobalStats();
    this.loadRoles();
    this.loadProfiles();
  }

  loadUsers() {
    this.loading.set(true);
    this.adminService.getUsers(
      this.currentPage(),
      this.itemsPerPage,
      this.searchQuery(),
      this.filterRole(),
      this.filterStatus()
    ).subscribe({
      next: (page) => {
        this.users.set(page.content);
        this.totalElements.set(page.totalElements);
        this.totalPages.set(page.totalPages);
        this.loading.set(false);
      },
      error: () => this.loading.set(false)
    });
  }

  loadGlobalStats() {
    forkJoin({
      total: this.adminService.getUsers(1, 1),
      active: this.adminService.getUsers(1, 1, '', '', false),
      inactive: this.adminService.getUsers(1, 1, '', '', true)
    }).subscribe({
      next: ({ total, active, inactive }) => {
        this.totalUsersCount.set(total.totalElements);
        this.activeUsersCount.set(active.totalElements);
        this.inactiveUsersCount.set(inactive.totalElements);
      },
      error: (err) => console.error('Erreur chargement stats utilisateurs', err)
    });
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

  onFilterRole(value: string) {
    this.filterRole.set(value);
    this.currentPage.set(1);
    this.loadUsers();
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
    this.loadUsers();
  }

  onSearch(value: string) {
    const search = value.toLowerCase().trim();

    this.searchQuery.set(value);
    this.currentPage.set(1);

    if (search === 'actif') {
      this.filterStatus.set(false);
    } else if (search === 'inactif') {
      this.filterStatus.set(true);
    } else {
      this.filterStatus.set(undefined);
    }

    this.loadUsers();
  }

  setPage(page: number) {
    if (page >= 1 && page <= this.totalPages()) {
      this.currentPage.set(page);
      this.loadUsers();
    }
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
  this.newRoles.set([]);        // ← réinitialisation
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

oldPassword = signal('');
newProfilePassword = signal('');
confirmProfilePassword = signal('');

 openEditModal(user: UserResponse) {
  this.editId.set(user.id);
  this.editNom.set(user.nom);
  this.editPrenom.set(user.prenom);
  this.editEmail.set(user.email);
  
  // Maintenant user.genre est déjà 'HOMME' ou 'FEMME'
  this.editGenre.set(user.genre);  // ← Direct, sans conversion
  
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
  this.editRoles.set([]);       // ← réinitialisation
  this.editGenre.set('FEMME');
  this.editProfileId.set(null);
}submitEditModal() {
  if (!this.editNom() || !this.editPrenom() || !this.editEmail() ||
      this.editRoles().length === 0 || !this.editGenre() || !this.editProfileId()) {
    this.showError('Veuillez remplir tous les champs (au moins un rôle)');
    return;
  }

  // editGenre() est déjà 'HOMME' ou 'FEMME' - parfait pour le backend
  this.adminService.updateUser(this.editId()!, {
    nom: this.editNom(),
    prenom: this.editPrenom(),
    email: this.editEmail(),
    genre: this.editGenre(),  // ← Envoie 'HOMME' ou 'FEMME'
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

  getRangeStart() {
    return (this.currentPage() - 1) * this.itemsPerPage + 1;
  }

  getRangeEnd() {
    return Math.min(this.currentPage() * this.itemsPerPage, this.totalElements());
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
  updateSelectedRoles(event: Event) {
  const select = event.target as HTMLSelectElement;
  const selectedOptions = Array.from(select.selectedOptions).map(opt => opt.value);
  this.newRoles.set(selectedOptions);
}
updateEditRoles(event: Event) {
  const select = event.target as HTMLSelectElement;
  const selectedOptions = Array.from(select.selectedOptions).map(opt => opt.value);
  this.editRoles.set(selectedOptions);
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

}

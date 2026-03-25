import { Component, inject, signal, computed, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { AdminService, UserResponse, RoleResponse } from '../../../core/services/admin.service';
import { AuthService } from '../../../core/services/auth.service';
import { Dashboard } from '../dashboard/dashboard';
import { Roles } from '../roles/roles';

@Component({
  selector: 'app-utilisateurs',
  standalone: true,
  imports: [CommonModule, FormsModule, Dashboard, Roles],
  templateUrl: './utilisateurs.html',
  styleUrls: ['./utilisateurs.css']
})
export class Utilisateurs implements OnInit {
  private adminService = inject(AdminService);
  private router = inject(Router);
  authService = inject(AuthService);
  currentUser = signal(this.authService.getUser());

  users = signal<UserResponse[]>([]);
  totalElements = signal(0);
  totalPages = signal(0);
  currentPage = signal(1);
  searchQuery = signal('');
  itemsPerPage = 5;
  loading = signal(false);
  activeNavItem = signal('utilisateurs');

  activeCount = computed(() => this.users().filter(u => !u.locked).length);
  lockedCount = computed(() => this.users().filter(u => u.locked).length);
  pages = computed(() =>
    Array.from({ length: this.totalPages() }, (_, i) => i + 1)
  );

  roles = signal<RoleResponse[]>([]);
  showModal = signal(false);
  newNom = signal('');
  newPrenom = signal('');
  newEmail = signal('');
  newPassword = signal('');
  newRole = signal('');
  newGenre = signal('FEMME');

  showEditModal = signal(false);
  editId = signal<number | null>(null);
  editNom = signal('');
  editPrenom = signal('');
  editEmail = signal('');
  editRole = signal('');
  editGenre = signal('FEMME');

  showConfirmModal = signal(false);
  confirmMessage = signal('');
  confirmAction = signal<() => void>(() => {});

  toastMessage = signal('');
  toastType = signal<'success' | 'error'>('success');
  showToast = signal(false);

  ngOnInit() {
    this.loadUsers();
    this.loadRoles();
  }

  logout() {
    this.authService.removeUser();
    this.router.navigate(['/login']);
  }

  loadUsers() {
    this.loading.set(true);
    this.adminService.getUsers(this.currentPage(), this.itemsPerPage, this.searchQuery())
      .subscribe({
        next: (page) => {
          this.users.set(page.content);
          this.totalElements.set(page.totalElements);
          this.totalPages.set(page.totalPages);
          this.loading.set(false);
        },
        error: () => this.loading.set(false)
      });
  }

  onSearch(value: string) {
    this.searchQuery.set(value);
    this.currentPage.set(1);
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
      this.adminService.enableUser(user.id).subscribe(() => this.loadUsers());
    } else {
      this.adminService.disableUser(user.id).subscribe(() => this.loadUsers());
    }
  }

  deleteUser(id: number) {
    this.confirmMessage.set('Voulez-vous vraiment supprimer cet utilisateur ? Cette action est irréversible.');
    this.confirmAction.set(() => {
      this.adminService.deleteUser(id).subscribe({
        next: () => {
          this.showSuccess('Utilisateur supprimé avec succès !');
          this.loadUsers();
        },
        error: () => this.showError('Erreur lors de la suppression')
      });
    });
    this.showConfirmModal.set(true);
  }

  confirmYes() {
    this.confirmAction()();
    this.showConfirmModal.set(false);
  }

  confirmNo() {
    this.showConfirmModal.set(false);
  }

  loadRoles() {
    this.adminService.getRoles().subscribe({
      next: (roles) => this.roles.set(roles.filter(r => r.active)),
      error: (err) => console.error('Erreur chargement rôles', err)
    });
  }

  openModal() { this.showModal.set(true); }

  closeModal() {
    this.showModal.set(false);
    this.newNom.set('');
    this.newPrenom.set('');
    this.newEmail.set('');
    this.newPassword.set('');
    this.newRole.set('');
    this.newGenre.set('FEMME');
  }

  submitModal() {
    if (!this.newNom() || !this.newPrenom() || !this.newEmail() ||
        !this.newPassword() || !this.newRole() || !this.newGenre()) {
      this.showError('Veuillez remplir tous les champs');
      return;
    }
    this.adminService.createUser({
      nom: this.newNom(),
      prenom: this.newPrenom(),
      email: this.newEmail(),
      password: this.newPassword(),
      roleCode: this.newRole(),
      genre: this.newGenre()
    }).subscribe({
      next: () => {
        this.closeModal();
        this.loadUsers();
        this.showSuccess('Utilisateur créé avec succès !');
      },
      error: (err) => {
        if (err.status === 409) this.showError('Cet email est déjà utilisé');
        else if (err.status === 404) this.showError('Rôle introuvable');
        else this.showError('Erreur lors de la création');
      }
    });
  }

  openEditModal(user: UserResponse) {
    this.editId.set(user.id);
    this.editNom.set(user.nom);
    this.editPrenom.set(user.prenom);
    this.editEmail.set(user.email);
    this.editGenre.set(user.genre === 'M' ? 'HOMME' : 'FEMME');
    this.editRole.set(user.roles[0]?.nom ?? '');
    this.showEditModal.set(true);
  }

  closeEditModal() {
    this.showEditModal.set(false);
    this.editId.set(null);
    this.editNom.set('');
    this.editPrenom.set('');
    this.editEmail.set('');
    this.editRole.set('');
    this.editGenre.set('FEMME');
  }

  submitEditModal() {
    if (!this.editNom() || !this.editPrenom() || !this.editEmail() ||
        !this.editRole() || !this.editGenre()) {
      this.showError('Veuillez remplir tous les champs');
      return;
    }
    this.adminService.updateUser(this.editId()!, {
      nom: this.editNom(),
      prenom: this.editPrenom(),
      email: this.editEmail(),
      genre: this.editGenre(),
      roleCode: this.editRole()
    }).subscribe({
      next: () => {
        this.closeEditModal();
        this.loadUsers();
        this.showSuccess('Utilisateur modifié avec succès !');
      },
      error: (err) => {
        if (err.status === 409) this.showError('Cet email est déjà utilisé');
        else this.showError('Erreur lors de la modification');
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

  setNav(id: string) { this.activeNavItem.set(id); }
  getRangeStart() { return (this.currentPage() - 1) * this.itemsPerPage + 1; }
  getRangeEnd() { return Math.min(this.currentPage() * this.itemsPerPage, this.totalElements()); }

  getRoleBadgeClass(role: string): string {
    switch (role.toUpperCase()) {
      case 'ADMINISTRATEUR': return 'badge-admin';
      case 'CHEF_PROJET':    return 'badge-chef';
      case 'MANAGER':        return 'badge-manager';
      default:               return 'badge-membre';
    }
  }

  getInitials(prenom: string, nom: string): string {
    return (prenom.charAt(0) + nom.charAt(0)).toUpperCase();
  }

  getAvatarColor(index: number): string {
    const colors = ['#E8F4FD', '#FFF0E8', '#F0F0FF', '#E8FFF0', '#FFF0F0', '#F5F0FF'];
    return colors[index % colors.length];
  }
}
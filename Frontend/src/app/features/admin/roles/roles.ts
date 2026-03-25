import { Component, inject, signal, computed, OnInit, Output, EventEmitter } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { AdminService, RoleResponse, PermissionSummary } from '../../../core/services/admin.service';
import { AuthService } from '../../../core/services/auth.service';

interface PermissionGroup {
  title: string;
  icon: string;
  permissions: PermissionSummary[];
}

@Component({
  selector: 'app-roles',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './roles.html',
  styleUrl: './roles.css',
})
export class Roles implements OnInit {
  @Output() navigateToDashboard = new EventEmitter<void>();

  private adminService = inject(AdminService);
  private router = inject(Router);
  authService = inject(AuthService);
  currentUser = signal(this.authService.getUser());

  roles = signal<RoleResponse[]>([]);
  allPermissions = signal<PermissionSummary[]>([]);
  permissionGroups = signal<PermissionGroup[]>([]);
  loading = signal(false);
  selectedRole = signal<RoleResponse | null>(null);
  selectedPermissions = signal<Set<number>>(new Set());

  // Modal states
  showModal = signal(false);
  newNom = signal('');
  newDescription = signal('');

  showEditModal = signal(false);
  editId = signal<number | null>(null);
  editNom = signal('');
  editDescription = signal('');

  showConfirmModal = signal(false);
  confirmMessage = signal('');
  confirmAction = signal<() => void>(() => {});

  showPermissionModal = signal(false);
  permissionNom = signal('');
  permissionDescription = signal('');

  toastMessage = signal('');
  toastType = signal<'success' | 'error'>('success');
  showToast = signal(false);

  ngOnInit() {
    this.loadRoles();
    this.loadPermissions();
  }

  logout() {
    this.authService.removeUser();
    this.router.navigate(['/login']);
  }

  loadPermissions() {
    this.adminService.getPermissions().subscribe({
      next: (permissions) => {
        this.allPermissions.set(permissions);
        this.groupPermissions(permissions);
      },
      error: (err) => console.error('Erreur chargement permissions', err)
    });
  }

  groupPermissions(permissions: PermissionSummary[]) {
    const groups: { [key: string]: PermissionSummary[] } = {};
    
    permissions.forEach(perm => {
      let group = 'Autres';
      const nomLower = perm.nom.toLowerCase();
      
      if (nomLower.includes('projet')) {
        group = 'Gestion des Projets';
      } else if (nomLower.includes('rapport') || nomLower.includes('donnees') || nomLower.includes('export') || nomLower.includes('tableau')) {
        group = 'Reporting & Données';
      } else if (nomLower.includes('invit') || nomLower.includes('utilisateur') || nomLower.includes('audit')) {
        group = 'Invitations';
      }
      
      if (!groups[group]) {
        groups[group] = [];
      }
      groups[group].push(perm);
    });

    const groupOrder = ['Gestion des Projets', 'Reporting & Données', 'Invitations', 'Autres'];
    const icons: { [key: string]: string } = {
      'Gestion des Projets': '📋',
      'Reporting & Données': '📊',
      'Invitations': '👥',
      'Autres': '🔒'
    };

    const permGroups = groupOrder
      .filter(key => groups[key])
      .map(key => ({
        title: key,
        icon: icons[key],
        permissions: groups[key]
      }));

    this.permissionGroups.set(permGroups);
  }

  loadRoles() {
    this.loading.set(true);
    this.adminService.getRoles().subscribe({
      next: (roles) => {
        this.roles.set(roles);
        this.loading.set(false);
        if (roles.length > 0) {
          this.selectRole(roles[0]);
        }
      },
      error: (err) => {
        console.error('Erreur chargement rôles', err);
        this.loading.set(false);
        this.showError('Erreur lors du chargement des rôles');
      }
    });
  }

  selectRole(role: RoleResponse) {
    this.selectedRole.set(role);
    this.selectedPermissions.set(new Set(role.permissions?.map(p => p.id) || []));
  }

  countUsersWithRole(roleId: number): number {
    const role = this.roles().find(r => r.id === roleId);
    return role ? (role.usersCount ?? 0) : 0;
  }

  hasPermission(permissionId: number): boolean {
    return this.selectedPermissions().has(permissionId);
  }

  togglePermission(permissionId: number) {
    const perms = new Set(this.selectedPermissions());
    if (perms.has(permissionId)) {
      perms.delete(permissionId);
    } else {
      perms.add(permissionId);
    }
    this.selectedPermissions.set(perms);
  }

  savePermissions() {
    if (!this.selectedRole()) {
      this.showError('Aucun rôle sélectionné');
      return;
    }
    
    const role = this.selectedRole()!;
    const permissionIds = Array.from(this.selectedPermissions());
    
    console.log('Envoi au backend - RoleId:', role.id, 'Permissions:', permissionIds);
    
    this.adminService.updateRole(role.id, {
      nom: role.nom,
      description: role.description,
      active: role.active,
      permissionIds: permissionIds
    }).subscribe({
      next: () => {
        this.loadRoles();
        this.showSuccess('Permissions sauvegardées avec succès !');
      },
      error: (err) => {
        console.error('Erreur sauvegarde permissions', err);
        this.showError('Erreur lors de la sauvegarde des permissions');
      }
    });
  }

  resetPermissions() {
    if (this.selectedRole()) {
      this.selectRole(this.selectedRole()!);
      this.showSuccess('Permissions réinitialisées');
    }
  }

  openModal() {
    this.showModal.set(true);
  }

  closeModal() {
    this.showModal.set(false);
    this.newNom.set('');
    this.newDescription.set('');
  }

  submitModal() {
    if (!this.newNom() || !this.newDescription()) {
      this.showError('Veuillez remplir tous les champs');
      return;
    }
    this.adminService.createRole({
      nom: this.newNom(),
      description: this.newDescription()
    }).subscribe({
      next: () => {
        this.closeModal();
        this.loadRoles();
        this.showSuccess('Rôle créé avec succès !');
      },
      error: (err) => {
        console.error('Erreur création rôle', err);
        if (err.status === 409) this.showError('Ce rôle existe déjà');
        else if (err.error?.message) this.showError(err.error.message);
        else this.showError('Erreur lors de la création');
      }
    });
  }

  openEditModal(role: RoleResponse) {
    this.editId.set(role.id);
    this.editNom.set(role.nom);
    this.editDescription.set(role.description);
    this.showEditModal.set(true);
  }

  closeEditModal() {
    this.showEditModal.set(false);
    this.editId.set(null);
    this.editNom.set('');
    this.editDescription.set('');
  }

    submitEditModal() {
    if (!this.editNom() || !this.editDescription()) {
      this.showError('Veuillez remplir tous les champs');
      return;
    }
    this.adminService.updateRole(this.editId()!, {
      nom: this.editNom(),
      description: this.editDescription(),
      active: true
    }).subscribe({
      next: () => {
        this.closeEditModal();
        this.loadRoles();
        this.showSuccess('Rôle modifié avec succès !');
      },
      error: (err) => {
        console.error('Erreur modification rôle', err);
        if (err.status === 409) this.showError('Ce rôle existe déjà');
        else this.showError('Erreur lors de la modification');
      }
    });
  }

  deleteRole(id: number) {
    this.confirmMessage.set('Voulez-vous vraiment supprimer ce rôle ? Cette action est irréversible.');
    this.confirmAction.set(() => {
      this.adminService.deleteRole(id).subscribe({
        next: () => {
          this.showSuccess('Rôle supprimé avec succès !');
          this.loadRoles();
          this.selectedRole.set(null);
        },
        error: (err) => {
          console.error('Erreur suppression rôle', err);
          this.showError('Erreur lors de la suppression');
        }
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

  openPermissionModal() {
    this.showPermissionModal.set(true);
  }

  closePermissionModal() {
    this.showPermissionModal.set(false);
    this.permissionNom.set('');
    this.permissionDescription.set('');
  }

  submitPermissionModal() {
    if (!this.permissionNom()) {
      this.showError('Veuillez saisir le nom de la permission');
      return;
    }

    this.adminService.createPermission({
      nom: this.permissionNom(),
      description: this.permissionDescription()
    }).subscribe({
      next: () => {
        this.closePermissionModal();
        this.loadPermissions();
        this.showSuccess('Permission créée avec succès !');
      },
      error: (err) => {
        console.error('Erreur création permission', err);
        if (err.status === 409) this.showError('Cette permission existe déjà');
        else this.showError('Erreur lors de la création');
      }
    });
  }

  toggleRoleStatus(role: RoleResponse) {
    const newStatus = !role.active;
    this.adminService.updateRole(role.id, {
      nom: role.nom,
      description: role.description,
      active: newStatus,
      permissionIds: role.permissions?.map(p => p.id)
    }).subscribe({
      next: () => {
        this.loadRoles();
        this.showSuccess(`Rôle ${newStatus ? 'activé' : 'désactivé'} avec succès !`);
      },
      error: (err) => {
        console.error('Erreur modification statut', err);
        this.showError('Erreur lors de la modification du statut');
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
}
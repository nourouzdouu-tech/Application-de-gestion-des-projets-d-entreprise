import { Component, OnDestroy, OnInit, ChangeDetectorRef, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { NavigationEnd, Router, RouterModule } from '@angular/router';
import { filter, Subscription } from 'rxjs';
import { TeamService, TeamDto, MemberInfo, UserSearchResult } from '../../../core/services/team.service';
import { AuthService } from '../../../core/services/auth.service';

@Component({
  selector: 'app-equipes',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule],
  templateUrl: './equipes.html',
  styleUrl: './equipes.css',
})
export class Equipes implements OnInit, OnDestroy {

  // ─── Données des équipes ─────────────────────────────────────────────────
  teams: TeamDto[] = [];
  selectedTeam: TeamDto | null = null;
  filteredMembers: MemberInfo[] = [];
  searchTerm = '';

  // ─── Modals ───────────────────────────────────────────────────────────────
  showCreateModal = false;
  showAssignModal = false;
  isRemovingMember = false;

  // ─── Assign Modal ─────────────────────────────────────────────────────────
  assignForm = {
    searchQuery: '',
    selectedUser: null as UserSearchResult | null
  };
  searchResults: UserSearchResult[] = [];
  isSearching = false;
  userIdError = '';

  // ─── Create / Edit Form ───────────────────────────────────────────────────
  teamForm = {
    name: '',
    description: ''
  };
  editMode = false;
  selectedTeamId: number | null = null;
  isLoading = false;
  successMessage = '';
  errorMessage = '';
  nameError = '';
  descriptionError = '';

  private routerSub?: Subscription;
  authService: AuthService;
  currentUser = signal<{ email: string; prenom: string; nom: string; roles: string[] } | null>(null);

  constructor(
    private teamService: TeamService,
    private router: Router,
    private cdr: ChangeDetectorRef,
    authService: AuthService
  ) {
    this.authService = authService;
    this.currentUser.set(this.authService.getUser());
  }

  ngOnInit(): void {
    this.loadMyTeams();

    this.routerSub = this.router.events
      .pipe(filter(event => event instanceof NavigationEnd))
      .subscribe(() => {
        if (this.router.url === '/chef-projet/equipes') {
          this.loadMyTeams();
        }
      });
  }

  ngOnDestroy(): void {
    this.routerSub?.unsubscribe();
  }

  logout(): void {
    this.authService.removeUser();
    this.router.navigate(['/login']);
  }

  // ─── Team Loading ─────────────────────────────────────────────────────────
  loadMyTeams(): void {
    this.isLoading = true;
    this.teamService.getMyTeams().subscribe({
      next: (teams) => {
        this.teams = teams;
        if (teams.length > 0) {
          this.selectedTeam = teams[0];
          this.filteredMembers = this.selectedTeam.members || [];
        } else {
          this.selectedTeam = null;
          this.filteredMembers = [];
        }
        this.errorMessage = '';
        this.isLoading = false;
        this.cdr.detectChanges();
      },
      error: (err) => {
        if (err.status !== 404) {
          this.errorMessage = err.error?.message || "Impossible de charger vos équipes.";
        }
        this.teams = [];
        this.selectedTeam = null;
        this.filteredMembers = [];
        this.isLoading = false;
        this.cdr.detectChanges();
      }
    });
  }

  selectTeam(team: TeamDto): void {
    this.selectedTeam = team;
    this.filteredMembers = team.members || [];
    this.searchTerm = '';
    this.errorMessage = '';
    this.successMessage = '';
    this.cdr.detectChanges();
  }

  // ─── Members Filter ───────────────────────────────────────────────────────
  filterMembers(): void {
    const term = this.searchTerm.toLowerCase().trim();
    if (!this.selectedTeam?.members) {
      this.filteredMembers = [];
      return;
    }
    if (!term) {
      this.filteredMembers = [...this.selectedTeam.members];
      return;
    }
    this.filteredMembers = this.selectedTeam.members.filter(member =>
      member.fullName.toLowerCase().includes(term) ||
      member.email.toLowerCase().includes(term) ||
      member.roleName.toLowerCase().includes(term)
    );
  }

  getMemberInitials(fullName: string): string {
    return fullName.substring(0, 2).toUpperCase();
  }

  isProjectManager(member: MemberInfo): boolean {
    if (!this.selectedTeam?.projectManagerId) return false;
    return member.id === this.selectedTeam.projectManagerId;
  }

  // ─── Assign Modal ─────────────────────────────────────────────────────────
  openAssignModal(): void {
    if (!this.selectedTeam) return;
    this.showAssignModal = true;
    this.assignForm = { searchQuery: '', selectedUser: null };
    this.searchResults = [];
    this.userIdError = '';
    this.errorMessage = '';
    this.successMessage = '';
  }

  closeAssignModal(): void {
    this.showAssignModal = false;
    this.assignForm = { searchQuery: '', selectedUser: null };
    this.searchResults = [];
    this.userIdError = '';
    this.errorMessage = '';
    this.successMessage = '';
  }

  onSearchUser(): void {
    const q = this.assignForm.searchQuery.trim();
    this.assignForm.selectedUser = null;

    if (q.length < 2) {
      this.searchResults = [];
      return;
    }

    this.isSearching = true;
    this.teamService.searchAvailableUsers(q).subscribe({
      next: (results: any) => {
        let resultsArray: UserSearchResult[] = [];
        
        if (Array.isArray(results)) {
          resultsArray = results;
        } else if (results && typeof results === 'object' && results.id) {
          resultsArray = [results];
        } else if (results && typeof results === 'object' && results._embedded) {
          resultsArray = results._embedded.userSearchResults || [];
        }
        
        const currentMemberIds = this.selectedTeam?.members?.map(m => m.id) || [];
        
        const availableUsers = resultsArray.filter(user => {
          if (currentMemberIds.includes(user.id)) {
            return false;
          }
          return true;
        });
        
        this.searchResults = availableUsers;
        this.isSearching = false;
      },
      error: (err) => {
        console.error('Search error:', err);
        this.searchResults = [];
        this.isSearching = false;
      }
    });
  }

  selectUser(user: UserSearchResult): void {
    if (user.alreadyInTeam) {
      this.userIdError = `⚠️ ${user.fullName} est déjà membre de l'équipe : ${user.teamName || 'une autre équipe'} !`;
      this.assignForm.selectedUser = null;
      this.assignForm.searchQuery = '';
      this.searchResults = [];
      return;
    }
    
    const isChefProjet = (user.roleName || '').toUpperCase().includes('CHEF_PROJET');
    
    if (isChefProjet) {
      this.userIdError = `⛔ Impossible d'assigner "${user.fullName}" car c'est un CHEF DE PROJET !`;
      this.assignForm.selectedUser = null;
      this.assignForm.searchQuery = '';
      this.searchResults = [];
      return;
    }
    
    const isAlreadyInCurrentTeam = this.selectedTeam?.members?.some(m => m.id === user.id) || false;
    
    if (isAlreadyInCurrentTeam) {
      this.userIdError = `⚠️ ${user.fullName} est déjà membre de cette équipe !`;
      this.assignForm.selectedUser = null;
      this.assignForm.searchQuery = '';
      this.searchResults = [];
      return;
    }
    
    const isManager = (user.roleName || '').toUpperCase().includes('MANAGER');
    const alreadyHasManager = this.selectedTeam?.members?.some(m => 
      (m.roleName || '').toUpperCase().includes('MANAGER')
    ) || false;
    
    if (isManager && alreadyHasManager) {
      this.userIdError = `⚠️ Impossible d'assigner "${user.fullName}" car cette équipe a déjà un MANAGER ! (Un seul manager par équipe)`;
      this.assignForm.selectedUser = null;
      this.assignForm.searchQuery = '';
      this.searchResults = [];
      return;
    }
    
    this.assignForm.selectedUser = user;
    this.assignForm.searchQuery = user.fullName;
    this.searchResults = [];
    this.userIdError = '';
  }

  submitAssignForm(): void {
  if (!this.selectedTeam?.id) return;

  this.userIdError = '';
  this.errorMessage = '';
  this.successMessage = '';

  if (!this.assignForm.selectedUser) {
    this.userIdError = 'Veuillez sélectionner un membre depuis la liste.';
    return;
  }

  this.isLoading = true;

  const userFullName = this.assignForm.selectedUser.fullName;

  this.teamService.assignUserToTeam(this.selectedTeam.id, this.assignForm.selectedUser.id).subscribe({
    next: () => {
      // ✅ Recharger complètement toutes les équipes
      this.teamService.getMyTeams().subscribe({
        next: (teams) => {
          this.teams = teams;
          if (teams.length > 0) {
            // Garder la même équipe sélectionnée si elle existe encore
            const sameTeam = teams.find(t => t.id === this.selectedTeam?.id);
            if (sameTeam) {
              this.selectedTeam = sameTeam;
            } else {
              this.selectedTeam = teams[0];
            }
            this.filteredMembers = this.selectedTeam?.members || [];
          } else {
            this.selectedTeam = null;
            this.filteredMembers = [];
          }
          
          this.successMessage = `${userFullName} a été affecté à l'équipe avec succès ✅`;
          this.assignForm.selectedUser = null;
          this.assignForm.searchQuery = '';
          this.searchResults = [];
          this.isLoading = false;
          this.cdr.detectChanges();
          
          setTimeout(() => {
            this.closeAssignModal();
            this.successMessage = '';
            this.cdr.detectChanges();
          }, 1500);
        },
        error: (err) => {
          this.isLoading = false;
          this.errorMessage = err.error?.message || "Erreur lors du chargement des équipes.";
          this.cdr.detectChanges();
        }
      });
    },
    error: (err) => {
      this.isLoading = false;
      this.errorMessage = err.error?.message || "Erreur lors de l'assignation du membre.";
      this.cdr.detectChanges();
      
      setTimeout(() => {
        this.errorMessage = '';
        this.cdr.detectChanges();
      }, 3000);
    }
  });
}

  // ─── Remove Member ────────────────────────────────────────────────────────
  // ─── Remove Member ────────────────────────────────────────────────────────
removeMember(member: MemberInfo): void {
  if (!this.selectedTeam?.id) return;
  
  const confirmed = confirm(`Voulez-vous vraiment retirer ${member.fullName} de l'équipe ?`);
  if (!confirmed) return;
  
  this.isRemovingMember = true;
  this.errorMessage = '';
  this.successMessage = '';
  
  // Stocker l'ID du membre avant la suppression
  const memberId = member.id;
  const teamId = this.selectedTeam.id;
  
  this.teamService.removeUserFromTeam(teamId, memberId).subscribe({
    next: (updatedTeam) => {
      // Option 1: Mise à jour directe
      this.selectedTeam = updatedTeam;
      this.filteredMembers = [...(updatedTeam.members || [])];
      
      // Option 2: Rechargement complet (plus fiable)
      this.loadMyTeams();
      
      this.successMessage = `${member.fullName} a été retiré de l'équipe avec succès ✅`;
      this.isRemovingMember = false;
      this.cdr.detectChanges();
      
      setTimeout(() => {
        this.successMessage = '';
        this.cdr.detectChanges();
      }, 3000);
    },
    error: (err) => {
      this.isRemovingMember = false;
      this.errorMessage = err.error?.message || "Erreur lors du retrait du membre.";
      this.cdr.detectChanges();
      
      setTimeout(() => {
        this.errorMessage = '';
        this.cdr.detectChanges();
      }, 3000);
    }
  });
}
  // ─── Create / Edit Modal ──────────────────────────────────────────────────
  openCreateModal(): void {
    this.editMode = false;
    this.selectedTeamId = null;
    this.resetForm();
    this.showCreateModal = true;
  }

  openEditModal(): void {
    if (!this.selectedTeam) return;
    this.editMode = true;
    this.selectedTeamId = this.selectedTeam.id || null;
    this.teamForm = {
      name: this.selectedTeam.name,
      description: this.selectedTeam.description || ''
    };
    this.showCreateModal = true;
  }

  closeModal(): void {
    this.showCreateModal = false;
    this.resetForm();
    this.editMode = false;
    this.selectedTeamId = null;
  }

  // ─── Delete Team ──────────────────────────────────────────────────────────
  deleteTeam(): void {
    if (!this.selectedTeam?.id) return;

    const confirmed = confirm(`Voulez-vous vraiment supprimer l'équipe "${this.selectedTeam.name}" ?`);
    if (!confirmed) return;

    this.errorMessage = '';
    this.successMessage = '';
    this.isLoading = true;

    this.teamService.setDeletedStatus(this.selectedTeam.id, true).subscribe({
      next: () => {
        this.isLoading = false;
        this.successMessage = 'Équipe supprimée avec succès ✅';
        this.loadMyTeams();
        this.cdr.detectChanges();

        setTimeout(() => {
          this.successMessage = '';
          this.cdr.detectChanges();
        }, 3000);
      },
      error: (err) => {
        this.isLoading = false;
        this.errorMessage = err.error?.message || 'Erreur lors de la suppression.';
        this.cdr.detectChanges();
      }
    });
  }

  // ─── Form Helpers ─────────────────────────────────────────────────────────
  clearFieldError(field: string): void {
    if (field === 'name') this.nameError = '';
    if (field === 'description') this.descriptionError = '';
    this.errorMessage = '';
    this.successMessage = '';
  }

  submitForm(): void {
    if (!this.validateForm()) return;

    this.errorMessage = '';
    this.successMessage = '';

    const payload: TeamDto = {
      name: this.teamForm.name.trim(),
      description: this.teamForm.description?.trim() || undefined
    };

    if (this.editMode && this.selectedTeamId) {
      this.teamService.updateTeam(this.selectedTeamId, payload).subscribe({
        next: (updated) => {
          this.successMessage = `L'équipe "${updated.name}" a été modifiée avec succès ✅`;
          this.loadMyTeams();
          setTimeout(() => this.closeModal(), 1500);
        },
        error: (err) => {
          this.errorMessage = err.error?.message || 'Erreur lors de la modification.';
        }
      });
      return;
    }

    this.teamService.createTeam(payload).subscribe({
      next: (created) => {
        this.successMessage = `L'équipe "${created?.name || payload.name}" a été créée avec succès ✅`;
        this.loadMyTeams();
        setTimeout(() => this.closeModal(), 1500);
      },
      error: (err) => {
        if (err.status === 409) {
          this.loadMyTeams();
          setTimeout(() => this.closeModal(), 500);
        } else if (err.status === 403) {
          this.errorMessage = err.error?.message || "Vous n'avez pas les droits pour créer une équipe.";
        } else if (err.status === 400) {
          this.errorMessage = err.error?.message || 'Données invalides.';
        } else {
          this.errorMessage = err.error?.message || 'Erreur lors de la création.';
        }
      }
    });
  }

  private validateForm(): boolean {
    let valid = true;
    this.nameError = '';
    this.descriptionError = '';

    const name = this.teamForm.name.trim();
    if (!name) {
      this.nameError = "Le nom de l'équipe est obligatoire.";
      valid = false;
    } else if (name.length > 100) {
      this.nameError = 'Le nom ne doit pas dépasser 100 caractères.';
      valid = false;
    }

    const desc = this.teamForm.description?.trim() || '';
    if (desc.length > 255) {
      this.descriptionError = 'La description ne doit pas dépasser 255 caractères.';
      valid = false;
    }

    return valid;
  }

  private resetForm(): void {
    this.teamForm = { name: '', description: '' };
    this.isLoading = false;
    this.successMessage = '';
    this.errorMessage = '';
    this.nameError = '';
    this.descriptionError = '';
  }
}
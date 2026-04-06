import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';
import { ProjectService, ProjectDto } from '../../../core/services/project.service';
import { AuthService } from '../../../core/services/auth.service';

@Component({
  selector: 'app-projets',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule],
  templateUrl: './projets.html',
  styleUrl: './projets.css',
})
export class Projets implements OnInit {
  projects: ProjectDto[] = [];
  loading = false;
  error: string | null = null;
  formError: string | null = null;

  searchTerm = '';
  selectedStatus = '';

  showForm = false;
  isEditMode = false;
  editingProjectId: number | null = null;

  projectForm: ProjectDto = this.getEmptyForm();

  currentUser = signal<any>(null);

  constructor(
    private projectService: ProjectService,
    public authService: AuthService
  ) {}

  ngOnInit(): void {
    this.currentUser.set(this.authService.getUser());
    this.fetchProjects();
  }

  getEmptyForm(): ProjectDto {
    return {
      name: '',
      description: '',
      client: '',
      progressPercentage: 0,
      riskLevel: 'FAIBLE',
      startDate: '',
      endDate: ''
    };
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

  fetchProjects(): void {
    this.loading = true;
    this.error = null;

    const query = this.searchTerm.trim() || undefined;
    const status = this.selectedStatus || undefined;

    this.projectService.getMyProjects(query, status).subscribe({
      next: (projects: ProjectDto[]) => {
        this.projects = projects ?? [];
        this.loading = false;
      },
      error: (err) => {
        console.error('Erreur chargement projets:', err);
        this.error = err?.error?.message || 'Erreur lors du chargement des projets.';
        this.loading = false;
      }
    });
  }

  openCreateForm(): void {
    this.showForm = true;
    this.isEditMode = false;
    this.editingProjectId = null;
    this.formError = null;
    this.projectForm = this.getEmptyForm();
  }

  openEditForm(project: ProjectDto): void {
    this.showForm = true;
    this.isEditMode = true;
    this.editingProjectId = project.id ?? null;
    this.formError = null;

    this.projectForm = {
      id: project.id,
      name: project.name ?? '',
      description: project.description ?? '',
      client: project.client ?? '',
      progressPercentage: project.progressPercentage ?? 0,
      riskLevel: project.riskLevel ?? 'FAIBLE',
      startDate: project.startDate ?? '',
      endDate: project.endDate ?? '',
      status: project.status,
      teamId: project.teamId,
      teamName: project.teamName
    };
  }

  closeForm(): void {
    this.showForm = false;
    this.isEditMode = false;
    this.editingProjectId = null;
    this.formError = null;
    this.projectForm = this.getEmptyForm();
  }

  submitForm(): void {
    this.formError = null;

    const payload: ProjectDto = {
      ...this.projectForm,
      name: (this.projectForm.name ?? '').toString().trim(),
      description: (this.projectForm.description ?? '').toString().trim(),
      client: (this.projectForm.client ?? '').toString().trim(),
      progressPercentage: Number(this.projectForm.progressPercentage ?? 0),
      riskLevel: this.projectForm.riskLevel,
      startDate: this.projectForm.startDate,
      endDate: this.projectForm.endDate
    };

    if (!payload.name) {
      this.formError = 'Le nom du projet est obligatoire.';
      return;
    }

    if (!payload.client) {
      this.formError = 'Le client est obligatoire.';
      return;
    }

    if (!payload.startDate || !payload.endDate) {
      this.formError = 'Les dates sont obligatoires.';
      return;
    }

    if (payload.progressPercentage < 0 || payload.progressPercentage > 100) {
      this.formError = 'La progression doit être comprise entre 0 et 100.';
      return;
    }

    if (payload.startDate > payload.endDate) {
      this.formError = 'La date de début doit être antérieure à la date de fin.';
      return;
    }

    if (this.isEditMode && this.editingProjectId !== null) {
      this.projectService.updateProject(this.editingProjectId, payload).subscribe({
        next: () => {
          this.closeForm();
          this.fetchProjects();
        },
        error: (err) => {
          console.error('Erreur modification projet:', err);
          this.formError = err?.error?.message || 'Erreur lors de la modification.';
        }
      });
      return;
    }

    this.projectService.createProject(payload).subscribe({
      next: () => {
        this.closeForm();
        this.fetchProjects();
      },
      error: (err) => {
        console.error('Erreur création projet:', err);
        this.formError = err?.error?.message || 'Erreur lors de la création.';
      }
    });
  }

  deleteProject(project: ProjectDto): void {
    if (!project.id) return;

    const confirmed = confirm(`Supprimer le projet "${project.name}" ?`);
    if (!confirmed) return;

    this.projectService.setDeletedStatus(project.id, true).subscribe({
      next: () => {
        this.projects = this.projects.filter(p => p.id !== project.id);
      },
      error: (err) => {
        console.error('Erreur suppression projet:', err);
        this.error = err?.error?.message || 'Erreur lors de la suppression du projet.';
      }
    });
  }

  onSearch(): void {
    this.fetchProjects();
  }

  clearSearch(): void {
    this.searchTerm = '';
    this.selectedStatus = '';
    this.fetchProjects();
  }

  setStatusFilter(status: string): void {
    this.selectedStatus = status;
    this.fetchProjects();
  }

  getStatusLabel(status?: string): string {
    switch (status) {
      case 'PRE_VALIDE':
        return 'Pré validé';
      case 'EN_COURS':
        return 'En cours';
      case 'EN_VALIDATION':
        return 'En validation';
      case 'VALIDE':
        return 'Validé';
      case 'REJETE':
        return 'Rejeté';
      case 'CLOTURE':
        return 'Clôturé';
      default:
        return status ?? '-';
    }
  }

  getRiskLabel(risk?: string): string {
    switch (risk) {
      case 'FAIBLE':
        return 'Faible';
      case 'MOYEN':
        return 'Moyen';
      case 'ELEVE':
        return 'Élevé';
      default:
        return risk ?? '-';
    }
  }

  getRiskClass(risk?: string): string {
    switch (risk) {
      case 'FAIBLE':
        return 'faible';
      case 'MOYEN':
        return 'moyen';
      case 'ELEVE':
        return 'eleve';
      default:
        return 'faible';
    }
  }

  getProgressLabel(progress: number | undefined): string {
    const value = Number(progress ?? 0);

    if (value === 100) return 'SUCCÈS';
    if (value >= 75) return 'TERMINÉ';
    if (value >= 40) return 'ATTENTION';
    if (value > 0) return 'CRITIQUE';
    return 'N/A';
  }
}
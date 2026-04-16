import { Component, OnInit, OnDestroy, signal, ChangeDetectorRef, NgZone } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterModule } from '@angular/router';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import { ProjectService, ProjectDto } from '../../../core/services/project.service';
import { AuthService } from '../../../core/services/auth.service';
import { ClientService, ClientSelectResponse } from '../../../core/services/client.service';
import { ManagerService, ManagerSelectDto } from '../../../core/services/manager.service';

type RepresentantDto = {
  id?: number;
  nom: string;
  email: string;
  telephone: string;
};

@Component({
  selector: 'app-responsable-contrat-projets',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule],
  templateUrl: './projets.html',
  styleUrl: './projets.css',
})
export class Projets implements OnInit, OnDestroy {
  projects: ProjectDto[] = [];
  clients: ClientSelectResponse[] = [];
  managers: ManagerSelectDto[] = [];

  selectedRepresentants: RepresentantDto[] = [];
  selectedRepresentantId: number | null = null;

  loading = false;
  error: string | null = null;
  formError: string | null = null;
  isSubmitting = false;

  searchTerm = '';
  selectedStatus = '';

  showForm = false;
  isEditMode = false;
  editingProjectId: number | null = null;

  showDeleteConfirm = false;
  projectToDelete: ProjectDto | null = null;

  showDetailsModal = false;
  selectedProjectDetails: ProjectDto | null = null;

  projectForm: ProjectDto = this.getEmptyForm();
  currentUser = signal<any>(null);

  currentPage = 1;
  itemsPerPage = 4;

  private destroy$ = new Subject<void>();

  constructor(
    private projectService: ProjectService,
    public authService: AuthService,
    private clientService: ClientService,
    private cdr: ChangeDetectorRef,
    private ngZone: NgZone,
    private managerService: ManagerService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.currentUser.set(this.authService.getUser());
    this.fetchProjects();
    this.loadClients();
    this.loadManagers();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  loadManagers(): void {
    this.managerService.getManagersForSelect()
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (data: ManagerSelectDto[]) => {
          this.managers = data;
        },
        error: (err: any) => {
          console.error('Erreur managers:', err);
        }
      });
  }

  loadClients(): void {
    this.clientService.getClientsForSelect()
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (clients: ClientSelectResponse[]) => {
          this.clients = clients;
        },
        error: (err: any) => {
          console.error('Erreur chargement clients:', err);
        }
      });
  }

  fetchProjects(): void {
    this.loading = true;
    this.error = null;
    this.currentPage = 1;

    const query = this.searchTerm.trim() || undefined;
    const status = this.selectedStatus || undefined;

    this.projectService.getAllProjects(query, status)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (projects: ProjectDto[]) => {
          this.projects = projects;
          this.loading = false;
          this.cdr.detectChanges();
        },
        error: (err: any) => {
          console.error('Erreur chargement projets:', err);
          this.error = 'Erreur lors du chargement des projets.';
          this.loading = false;
          this.cdr.detectChanges();
        }
      });
  }

  get paginatedProjects(): ProjectDto[] {
    const start = (this.currentPage - 1) * this.itemsPerPage;
    return this.projects.slice(start, start + this.itemsPerPage);
  }

  get totalPages(): number {
    return Math.ceil(this.projects.length / this.itemsPerPage);
  }

  get pageNumbers(): number[] {
    return Array.from({ length: this.totalPages }, (_, i) => i + 1);
  }

  get startItem(): number {
    if (this.projects.length === 0) return 0;
    return (this.currentPage - 1) * this.itemsPerPage + 1;
  }

  get endItem(): number {
    return Math.min(this.currentPage * this.itemsPerPage, this.projects.length);
  }

  goToPage(page: number): void {
    if (page < 1 || page > this.totalPages) return;
    this.currentPage = page;
  }

  onClientChange(): void {
    const selectedClient = this.clients.find(c => c.nom === this.projectForm.client);
    this.selectedRepresentants = selectedClient?.representants ?? [];
    this.selectedRepresentantId = null;
  }

  getEmptyForm(): ProjectDto {
    return {
      name: '',
      description: '',
      client: '',
      progressPercentage: 0,
      riskLevel: 'FAIBLE',
      startDate: '',
      endDate: '',
      managerId: undefined
    };
  }

  openCreateForm(): void {
    this.showForm = true;
    this.isEditMode = false;
    this.editingProjectId = null;
    this.formError = null;
    this.selectedRepresentants = [];
    this.selectedRepresentantId = null;
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
      managerId: project.managerId
    };

    const selectedClient = this.clients.find(c => c.nom === this.projectForm.client);
    this.selectedRepresentants = selectedClient?.representants ?? [];
    this.selectedRepresentantId = null;
  }

  closeForm(): void {
    this.showForm = false;
    this.isEditMode = false;
    this.editingProjectId = null;
    this.formError = null;
    this.projectForm = this.getEmptyForm();
    this.selectedRepresentants = [];
    this.selectedRepresentantId = null;
    this.isSubmitting = false;
  }

  submitForm(): void {
    if (this.isSubmitting) return;

    this.formError = null;

    const payload: ProjectDto = {
      ...this.projectForm,
      name: (this.projectForm.name ?? '').toString().trim(),
      description: (this.projectForm.description ?? '').toString().trim(),
      client: (this.projectForm.client ?? '').toString().trim(),
      progressPercentage: Number(this.projectForm.progressPercentage ?? 0),
      riskLevel: this.projectForm.riskLevel ?? 'FAIBLE',
      startDate: this.projectForm.startDate,
      endDate: this.projectForm.endDate,
      managerId: this.projectForm.managerId ? Number(this.projectForm.managerId) : undefined
    };

    if (!payload.name) {
      this.formError = 'Le nom du projet est obligatoire.';
      return;
    }
    if (!payload.client) {
      this.formError = 'Le client est obligatoire.';
      return;
    }
    if (!payload.managerId) {
      this.formError = 'Le manager est obligatoire.';
      return;
    }
    if (!payload.startDate || !payload.endDate) {
      this.formError = 'Les dates sont obligatoires.';
      return;
    }
    if (payload.startDate > payload.endDate) {
      this.formError = 'La date de début doit être antérieure à la date de fin.';
      return;
    }

    this.isSubmitting = true;

    const request$ = this.isEditMode && this.editingProjectId
      ? this.projectService.updateProject(this.editingProjectId, payload)
      : this.projectService.createProject(payload);

    request$.pipe(takeUntil(this.destroy$)).subscribe({
      next: () => {
        this.ngZone.run(() => {
          this.closeForm();
          this.fetchProjects();
        });
      },
      error: (err: any) => {
        this.ngZone.run(() => {
          console.error('SAVE ERROR:', err);
          this.isSubmitting = false;
          this.formError = err?.error?.message || err?.message || 'Erreur lors de l\'enregistrement.';
        });
      }
    });
  }

  confirmDelete(project: ProjectDto): void {
    this.projectToDelete = project;
    this.showDeleteConfirm = true;
  }

  cancelDelete(): void {
    this.projectToDelete = null;
    this.showDeleteConfirm = false;
  }

  deleteProject(): void {
    if (!this.projectToDelete?.id) return;

    this.projectService.setDeletedStatus(this.projectToDelete.id, true)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: () => {
          this.projects = this.projects.filter(p => p.id !== this.projectToDelete!.id);

          if (this.currentPage > this.totalPages && this.totalPages > 0) {
            this.currentPage = this.totalPages;
          }
          if (this.projects.length === 0) {
            this.currentPage = 1;
          }

          this.showDeleteConfirm = false;
          this.projectToDelete = null;
        },
        error: (err: any) => {
          console.error('Erreur suppression projet:', err);
          this.error = err?.error?.message || 'Erreur lors de la suppression du projet.';
          this.showDeleteConfirm = false;
          this.projectToDelete = null;
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

  goToBilling(project: ProjectDto): void {
    if (!project.id) return;
    this.router.navigate(['/responsable-contrat/facturation', project.id]);
  }

  openDetails(project: ProjectDto): void {
    this.selectedProjectDetails = project;
    this.showDetailsModal = true;
  }

  closeDetails(): void {
    this.selectedProjectDetails = null;
    this.showDetailsModal = false;
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

  getStatusLabel(status?: string): string {
    switch (status) {
      case 'EN_VALIDATION':
        return 'En validation';
      case 'PRE_VALIDE':
        return 'Pré-validé';
      case 'EN_COURS':
        return 'En cours';
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

  getStatusClass(status?: string): string {
    switch (status) {
      case 'EN_VALIDATION':
        return 'status-info';
      case 'PRE_VALIDE':
        return 'status-warning';
      case 'VALIDE':
        return 'status-success';
      case 'EN_COURS':
        return 'status-warning';
      case 'REJETE':
        return 'status-danger';
      case 'CLOTURE':
        return 'status-neutral';
      default:
        return 'status-info';
    }
  }
}
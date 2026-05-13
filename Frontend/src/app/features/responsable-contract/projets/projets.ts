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

type WorkflowStepState = 'complete' | 'current' | 'upcoming' | 'rejected';

type WorkflowStep = {
  key: string;
  label: string;
  icon: 'create' | 'validate' | 'rejected' | 'manager' | 'chef' | 'team' | 'progress' | 'close';
  state: WorkflowStepState;
  date: string;
  time: string;
  reviewedAt?: string;
  updatedAt?: string;
  createdAt?: string;
  managerComment?: string;
  managerName?: string;
  chefProjetName?: string;
  managerId?: number;
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
  private allManagers: ManagerSelectDto[] = [];

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

  minEndDate: string = '';

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
          this.allManagers = data;
          this.managers = [...data];
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
    const status = this.selectedStatus.trim() || undefined;

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
          this.error = err?.error?.message || 'Erreur lors du chargement des projets.';
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

  onStartDateChange(): void {
    if (this.projectForm.startDate) {
      this.minEndDate = this.projectForm.startDate;
      if (this.projectForm.endDate && this.projectForm.endDate < this.projectForm.startDate) {
        this.projectForm.endDate = '';
      }
    } else {
      this.minEndDate = '';
    }
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
    } as ProjectDto;
  }

  openCreateForm(): void {
    this.showForm = true;
    this.isEditMode = false;
    this.editingProjectId = null;
    this.formError = null;
    this.selectedRepresentants = [];
    this.selectedRepresentantId = null;
    this.projectForm = this.getEmptyForm();
    this.managers = [...this.allManagers];
    this.minEndDate = '';
  }

  isProjectInProgress(project: ProjectDto): boolean {
    return (project.status ?? '').toUpperCase() === 'EN_COURS';
  }

  isProjectRejected(project: ProjectDto | null): boolean {
    const status = (project?.status ?? '').toString().toUpperCase();
    return status === 'REJETE' || status === 'REJETÉ' || status === 'REJECTED';
  }

  isRejectedEditMode(): boolean {
    return this.isEditMode && this.isProjectRejected(this.projectForm);
  }

  private refreshManagersForProject(project: ProjectDto | null): void {
    const source = this.allManagers.length > 0 ? this.allManagers : this.managers;

    if (!project || !this.isProjectRejected(project)) {
      this.managers = [...source];
      return;
    }

    const rejectedManagerId = Number((project as any).managerId);
    const rejectedManagerName = ((project as any).managerName ?? '').toString().trim().toLowerCase();

    this.managers = source.filter((manager: any) => {
      const managerId = Number(manager.id);
      const managerName = (manager.fullName ?? manager.name ?? '').toString().trim().toLowerCase();

      if (rejectedManagerId && managerId === rejectedManagerId) {
        return false;
      }

      if (rejectedManagerName && managerName === rejectedManagerName) {
        return false;
      }

      return true;
    });
  }

  openEditForm(project: ProjectDto): void {
    if (this.isProjectInProgress(project)) return;

    this.showForm = true;
    this.isEditMode = true;
    this.editingProjectId = project.id ?? null;
    this.formError = null;

    this.refreshManagersForProject(project);

    this.projectForm = {
      id: project.id,
      name: project.name ?? '',
      description: project.description ?? '',
      client: project.client ?? '',
      progressPercentage: project.progressPercentage ?? 0,
      riskLevel: project.riskLevel ?? 'FAIBLE',
      startDate: project.startDate ?? '',
      endDate: project.endDate ?? '',
      managerId: this.isProjectRejected(project) ? undefined : (project as any).managerId,
      status: project.status
    } as ProjectDto;

    this.minEndDate = project.startDate ?? '';

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
    this.managers = [...this.allManagers];
    this.minEndDate = '';
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
      managerId: (this.projectForm as any).managerId
        ? Number((this.projectForm as any).managerId)
        : undefined
    } as ProjectDto;

    if (!payload.name) {
      this.formError = 'Le nom du projet est obligatoire.';
      return;
    }

    if (!payload.client) {
      this.formError = 'Le client est obligatoire.';
      return;
    }

    if (!(payload as any).managerId) {
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
    if (this.isProjectInProgress(project)) return;

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
    this.currentPage = 1;
    this.fetchProjects();
  }

  setStatusFilter(status: string): void {
    this.selectedStatus = (status || '').trim();
    this.currentPage = 1;
    this.fetchProjects();
  }

  isStatusActive(status: string): boolean {
    return this.selectedStatus === status;
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
        return 'En cours de validation';
      case 'PRE_VALIDE':
        return 'Pré-validé';
      case 'EN_COURS':
        return 'En cours de réalisation';
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
        return 'status-success';
      case 'REJETE':
        return 'status-danger';
      case 'CLOTURE':
        return 'status-neutral';
      default:
        return 'status-info';
    }
  }

  formatDateTime(value?: string | null): string {
    if (!value) return '-';

    const date = new Date(value);
    if (Number.isNaN(date.getTime())) return '-';

    const day = String(date.getDate()).padStart(2, '0');
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const year = date.getFullYear();
    const hours = String(date.getHours()).padStart(2, '0');
    const minutes = String(date.getMinutes()).padStart(2, '0');

    return `${day}/${month}/${year} ${hours}:${minutes}`;
  }

  private splitDateTime(value?: string | null): { date: string; time: string } {
    const formatted = this.formatDateTime(value);
    if (formatted === '-') {
      return { date: 'À venir', time: '' };
    }

    const parts = formatted.split(' ');
    return {
      date: parts[0] ?? '-',
      time: parts[1] ?? ''
    };
  }

  getWorkflowSteps(project: ProjectDto | null): WorkflowStep[] {
    const status = (project?.status || '').toString().toUpperCase();

    const reviewedAt = (project as any)?.reviewedAt;
    const updatedAt = (project as any)?.updatedAt;
    const createdAt = (project as any)?.createdAt;
    const startDate = project?.startDate;
    const endDate = project?.endDate;

    const creationDate = this.splitDateTime(createdAt || updatedAt);
    const validationDate = this.splitDateTime(reviewedAt || updatedAt || createdAt);
    const teamDate = this.splitDateTime(updatedAt || reviewedAt || createdAt);
    const progressDate = this.splitDateTime(startDate || updatedAt || reviewedAt || createdAt);
    const closeDate = this.splitDateTime(endDate);

    const steps: WorkflowStep[] = [
      {
        key: 'creation',
        label: 'Création par RC',
        icon: 'create',
        state: 'complete',
        date: creationDate.date,
        time: creationDate.time
      },
      {
        key: 'validation_manager',
        label: 'Validation manager',
        icon: 'validate',
        state: 'upcoming',
        date: validationDate.date,
        time: validationDate.time
      },
      {
        key: 'affectation_equipe',
        label: 'Affectation équipe',
        icon: 'team',
        state: 'upcoming',
        date: teamDate.date,
        time: teamDate.time
      },
      {
        key: 'projet_en_cours',
        label: 'Projet en cours',
        icon: 'progress',
        state: 'upcoming',
        date: progressDate.date,
        time: progressDate.time
      },
      {
        key: 'cloture',
        label: 'Clôturé',
        icon: 'close',
        state: 'upcoming',
        date: closeDate.date,
        time: closeDate.time
      }
    ];

    switch (status) {
      case 'EN_VALIDATION':
        break;

      case 'PRE_VALIDE':
      case 'VALIDE':
        steps[1].state = 'complete';
        steps[2].state = 'current';
        break;

      case 'EN_COURS':
        steps[1].state = 'complete';
        steps[2].state = 'complete';
        steps[3].state = 'current';
        break;

      case 'CLOTURE':
        steps[1].state = 'complete';
        steps[2].state = 'complete';
        steps[3].state = 'complete';
        steps[4].state = 'current';
        break;

      case 'REJETE':
      case 'REJETÉ':
      case 'REJECTED':
        steps[1].state = 'rejected';
        steps[1].icon = 'rejected';
        break;

      default:
        break;
    }

    steps.forEach(step => {
      if (step.state === 'upcoming') {
        step.date = 'À venir';
        step.time = '';
      }
    });

    return steps;
  }
  isProjectClosed(project: ProjectDto): boolean {
  return (project?.status ?? '').toUpperCase() === 'CLOTURE';
}
}

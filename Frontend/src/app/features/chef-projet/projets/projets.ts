import {
  Component, OnInit, signal, ChangeDetectorRef, NgZone
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterModule } from '@angular/router';
import { ProjectService, ProjectDto } from '../../../core/services/project.service';
import { AuthService } from '../../../core/services/auth.service';
import { TeamService, TeamDto } from '../../../core/services/team.service';
import { TaskService, TaskDto } from '../../../core/services/task.service';

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

  selectedTeamPreview: TeamDto | null = null;
  isAssigning = false;
  showDetailModal = false;
  detailProject: ProjectDto | null = null;

  currentPageProjets = 1;
  itemsPerPageProjets = 5;
  totalProjets = 0;
  paginatedProjects: ProjectDto[] = [];

  currentPageTaches = 1;
  itemsPerPageTaches = 5;
  totalTaches = 0;
  paginatedTasks: TaskDto[] = [];

  Math = Math;

  teams: TeamDto[] = [];
  selectedTeamId: number | null = null;
  showTeamModal = false;
  currentProjectForTeam: ProjectDto | null = null;

  showTaskModal = false;
  currentProjectForTask: ProjectDto | null = null;
  taskForm: Partial<TaskDto> = { title: '', description: '', startDate: '', criticite: 3, assignedToId: undefined };

  showTasksModal = false;
  currentProjectTasks: ProjectDto | null = null;
  projectTasks: TaskDto[] = [];

  taskSearchTerm = '';
  taskFilterPriority = '';
  taskFilterMember = '';

  editingTask: TaskDto | null = null;
  taskFormModal = false;

  showValidationModal = false;
  taskToValidate: TaskDto | null = null;
  validationAction: 'valider' | 'rejeter' = 'valider';
  validationComment = '';

  showDeleteTaskModal = false;
  taskIdToDelete: number | null = null;

  showClotureModal = false;
  projectToCloture: ProjectDto | null = null;

  teamMembers: any[] = [];
  searchTerm = '';
  selectedStatus = '';
  selectedTeamFilter = ''; // ← NOUVEAU
  allProjects: ProjectDto[] = [];
  showForm = false;
  isEditMode = false;
  editingProjectId: number | null = null;
  projectForm: ProjectDto = this.getEmptyForm();
  currentUser = signal<any>(null);
  toastMessage: string | null = null;
  toastType: 'success' | 'error' = 'success';

  private avatarColors = [
    '#8b5cf6', '#10b981', '#f59e0b', '#ef4444',
    '#3b82f6', '#ec4899', '#14b8a6', '#f97316'
  ];

  constructor(
    private projectService: ProjectService,
    private teamService: TeamService,
    private taskService: TaskService,
    public authService: AuthService,
    private cdr: ChangeDetectorRef,
    private ngZone: NgZone,
    private router: Router
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

  // ══════════════════════════════════════════════════
  // CLÔTURE PROJET
  // ══════════════════════════════════════════════════

  canCloseProject(project: ProjectDto): boolean {
    return project.status !== 'CLOTURE' && Number(project.progressPercentage ?? 0) === 100;
  }

  openClotureModal(project: ProjectDto): void {
    if (project.status === 'CLOTURE') return;
    if (!project.id) return;

    this.taskService.getTasksByProject(project.id).subscribe({
      next: (tasks) => this.ngZone.run(() => {
        const total = tasks.length;
        const done = tasks.filter(t => t.status === 'Terminé').length;
        const progression = total > 0 ? Math.round((done / total) * 100) : 0;

        if (progression === 100) {
          this.projectToCloture = project;
          this.showClotureModal = true;
        } else {
          this.showToast('🔒 La clôture est possible uniquement si la progression est à 100 %.', 'error');
        }

        this.cdr.detectChanges();
      }),
      error: () => this.ngZone.run(() => {
        this.showToast('Erreur lors de la vérification des tâches du projet.', 'error');
        this.cdr.detectChanges();
      })
    });
  }

  closeClotureModal(): void {
    this.showClotureModal = false;
    this.projectToCloture = null;
    this.cdr.detectChanges();
  }

  confirmCloture(): void {
    if (!this.projectToCloture?.id) return;

    this.projectService.updateProjectStatus(this.projectToCloture.id, 'CLOTURE').subscribe({
      next: () => this.ngZone.run(() => {
        const idx = this.projects.findIndex(p => p.id === this.projectToCloture!.id);
        if (idx !== -1) {
          this.projects[idx] = { ...this.projects[idx], status: 'CLOTURE' };
          this.projects = [...this.projects];
          this.updatePaginatedProjects();
        }
        // Mettre à jour aussi dans allProjects
        const idxAll = this.allProjects.findIndex(p => p.id === this.projectToCloture!.id);
        if (idxAll !== -1) {
          this.allProjects[idxAll] = { ...this.allProjects[idxAll], status: 'CLOTURE' };
          this.allProjects = [...this.allProjects];
        }
        this.showToast('🔒 Projet clôturé avec succès', 'success');
        this.closeClotureModal();
        this.cdr.detectChanges();
      }),
      error: (err) => this.ngZone.run(() => {
        this.showToast(err?.error?.message || 'Erreur lors de la clôture du projet', 'error');
        this.closeClotureModal();
        this.cdr.detectChanges();
      })
    });
  }

  // ══════════════════════════════════════════════════
  // DÉTAILS PROJET
  // ══════════════════════════════════════════════════

  openDetailModal(project: ProjectDto): void {
    this.detailProject = { ...project };
    this.showDetailModal = true;

    this.taskService.getTasksByProject(project.id!).subscribe({
      next: (tasks) => this.ngZone.run(() => {
        const total = tasks.length;
        const done = tasks.filter(t => t.status === 'Terminé').length;
        const progression = total > 0 ? Math.round((done / total) * 100) : 0;
        this.detailProject = { ...this.detailProject!, progressPercentage: progression };
        this.cdr.detectChanges();
      }),
      error: () => { this.cdr.detectChanges(); }
    });

    this.cdr.detectChanges();
  }

  closeDetailModal(): void {
    this.showDetailModal = false;
    this.detailProject = null;
    this.cdr.detectChanges();
  }

  getInitialsFromName(fullName?: string): string {
    if (!fullName) return '?';
    const parts = fullName.trim().split(' ');
    if (parts.length === 1) return parts[0].charAt(0).toUpperCase();
    return (parts[0].charAt(0) + parts[parts.length - 1].charAt(0)).toUpperCase();
  }

  getAvatarColor(name?: string): string {
    if (!name) return this.avatarColors[0];
    let hash = 0;
    for (let i = 0; i < name.length; i++) {
      hash = name.charCodeAt(i) + ((hash << 5) - hash);
    }
    return this.avatarColors[Math.abs(hash) % this.avatarColors.length];
  }

  getTaskCountByStatus(status: string): number {
    return this.projectTasks.filter(t => t.status === status).length;
  }

  getFilteredTasks(): TaskDto[] {
    return this.projectTasks.filter(t => {
      const matchSearch =
        !this.taskSearchTerm ||
        t.title.toLowerCase().includes(this.taskSearchTerm.toLowerCase()) ||
        (t.description || '').toLowerCase().includes(this.taskSearchTerm.toLowerCase());

      const matchPriority =
        !this.taskFilterPriority ||
        (t.priority || '').toUpperCase() === this.taskFilterPriority.toUpperCase();

      const matchMember =
        !this.taskFilterMember ||
        (t.assignedToName || '') === this.taskFilterMember;

      return matchSearch && matchPriority && matchMember;
    });
  }

  getUniqueMembres(): string[] {
    const names = this.projectTasks
      .map(t => t.assignedToName)
      .filter((n): n is string => !!n);
    return [...new Set(names)];
  }

  // ← NOUVEAU : équipes distinctes pour le filtre
  getUniqueEquipes(): string[] {
    const noms = this.allProjects
      .map(p => p.teamName)
      .filter((n): n is string => !!n && n.trim() !== '');
    return [...new Set(noms)].sort();
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

    this.projectService.getMyProjects().subscribe({
      next: (projects) => this.ngZone.run(() => {
        this.allProjects = [...(projects ?? [])];
        this.currentPageProjets = 1;
        this.applyFilters();
        this.loading = false;
        this.cdr.detectChanges();
      }),
      error: (err) => this.ngZone.run(() => {
        this.error = err?.error?.message || 'Erreur lors du chargement.';
        this.loading = false;
        this.cdr.detectChanges();
      })
    });
  }

  onSearch(): void {
    this.currentPageProjets = 1;
    this.applyFilters();
  }

  clearSearch(): void {
    this.searchTerm = '';
    this.selectedStatus = '';
    this.selectedTeamFilter = ''; // ← NOUVEAU
    this.currentPageProjets = 1;
    this.applyFilters();
  }

  setStatusFilter(status: string): void {
    this.selectedStatus = status;
    this.currentPageProjets = 1;
    this.applyFilters();
  }

  applyFilters(): void {
    let filtered = [...this.allProjects];

    // Filtre texte
    if (this.searchTerm.trim()) {
      const q = this.searchTerm.trim().toLowerCase();
      filtered = filtered.filter(p =>
        p.name?.toLowerCase().includes(q) ||
        p.client?.toLowerCase().includes(q)
      );
    }

    // Filtre statut
    if (this.selectedStatus === 'EN_COURS') {
      filtered = filtered.filter(p =>
        p.status === 'EN_COURS' || p.status === 'PRE_VALIDE' || p.status === 'EN_VALIDATION'
      );
    } else if (this.selectedStatus === 'CLOTURE') {
      filtered = filtered.filter(p => p.status === 'CLOTURE');
    }

    // ← NOUVEAU : filtre équipe
    if (this.selectedTeamFilter) {
      filtered = filtered.filter(p => p.teamName === this.selectedTeamFilter);
    }

    this.projects = filtered;
    this.totalProjets = filtered.length;
    this.currentPageProjets = 1;
    this.updatePaginatedProjects();
    this.cdr.detectChanges();
  }

  // ══════════════════════════════════════════════════
  // FORMULAIRE PROJET
  // ══════════════════════════════════════════════════

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

    this.cdr.detectChanges();
  }

  closeForm(): void {
    this.showForm = false;
    this.isEditMode = false;
    this.editingProjectId = null;
    this.formError = null;
    this.projectForm = this.getEmptyForm();
    this.cdr.detectChanges();
  }

  submitForm(): void {
    this.formError = null;

    const payload: ProjectDto = {
      ...this.projectForm,
      name: (this.projectForm.name ?? '').toString().trim(),
      description: (this.projectForm.description ?? '').toString().trim(),
      client: (this.projectForm.client ?? '').toString().trim(),
      progressPercentage: Number(this.projectForm.progressPercentage ?? 0)
    };

    if (!payload.name) { this.formError = 'Le nom du projet est obligatoire.'; this.cdr.detectChanges(); return; }
    if (!payload.client) { this.formError = 'Le client est obligatoire.'; this.cdr.detectChanges(); return; }
    if (!payload.startDate || !payload.endDate) { this.formError = 'Les dates sont obligatoires.'; this.cdr.detectChanges(); return; }
    if (payload.progressPercentage < 0 || payload.progressPercentage > 100) { this.formError = 'La progression doit être entre 0 et 100.'; this.cdr.detectChanges(); return; }
    if (payload.startDate > payload.endDate) { this.formError = 'La date de début doit être antérieure à la date de fin.'; this.cdr.detectChanges(); return; }

    if (this.isEditMode && this.editingProjectId !== null) {
      this.projectService.updateProject(this.editingProjectId, payload).subscribe({
        next: () => this.ngZone.run(() => { this.closeForm(); this.fetchProjects(); this.showToast('✅ Projet modifié avec succès', 'success'); this.cdr.detectChanges(); }),
        error: (err) => this.ngZone.run(() => { this.formError = err?.error?.message || 'Erreur lors de la modification.'; this.cdr.detectChanges(); })
      });
      return;
    }

    this.projectService.createProject(payload).subscribe({
      next: () => this.ngZone.run(() => { this.closeForm(); this.fetchProjects(); this.showToast('✅ Projet créé avec succès', 'success'); this.cdr.detectChanges(); }),
      error: (err) => this.ngZone.run(() => { this.formError = err?.error?.message || 'Erreur lors de la création.'; this.cdr.detectChanges(); })
    });
  }

  deleteProject(project: ProjectDto): void {
    if (!project.id || !confirm(`Supprimer le projet "${project.name}" ?`)) return;

    this.projectService.setDeletedStatus(project.id, true).subscribe({
      next: () => this.ngZone.run(() => {
        this.allProjects = this.allProjects.filter(p => p.id !== project.id);
        this.applyFilters();
        this.showToast('🗑️ Projet supprimé', 'success');
        this.cdr.detectChanges();
      }),
      error: (err) => this.ngZone.run(() => {
        this.error = err?.error?.message || 'Erreur suppression.';
        this.cdr.detectChanges();
      })
    });
  }

  getStatusLabel(status?: string): string {
    const m: Record<string, string> = {
      PRE_VALIDE: 'En cours', EN_COURS: 'En cours', EN_VALIDATION: 'En validation',
      VALIDE: 'Validé', REJETE: 'Rejeté', CLOTURE: 'Clôturé'
    };
    return status ? (m[status] ?? status) : '-';
  }

  getRiskLabel(risk?: string): string {
    const m: Record<string, string> = { FAIBLE: 'Faible', MOYEN: 'Moyen', ELEVE: 'Élevé' };
    return risk ? (m[risk] ?? risk) : '-';
  }

  getRiskClass(risk?: string): string {
    const m: Record<string, string> = { FAIBLE: 'faible', MOYEN: 'moyen', ELEVE: 'eleve' };
    return risk ? (m[risk] ?? 'faible') : 'faible';
  }

  getProgressLabel(progress: number | undefined): string {
    const v = Number(progress ?? 0);
    if (v === 100) return 'SUCCÈS';
    if (v >= 75) return 'TERMINÉ';
    if (v >= 40) return 'ATTENTION';
    if (v > 0) return 'CRITIQUE';
    return 'N/A';
  }

  getTaskStatusLabel(status?: string): string {
    const m: Record<string, string> = { A_faire: 'À faire', En_cours: 'En cours', Terminé: 'Terminé', Validation: 'Validation' };
    return status ? (m[status] ?? status) : 'À faire';
  }

  getTaskDisplayStatus(task: TaskDto): string {
    if (task.rejected && task.status === 'En_cours') return 'Rejeté';
    return this.getTaskStatusLabel(task.status);
  }

  getTaskStatusClass(status?: string): string {
    const m: Record<string, string> = { A_faire: 'statut-a-faire', En_cours: 'statut-en-cours', Terminé: 'statut-termine', Validation: 'statut-validation' };
    return status ? (m[status] ?? '') : 'statut-a-faire';
  }

  getTaskPriorityClass(priority?: string): string {
    if (!priority) return 'priorite-moyenne';
    const p = priority.toLowerCase();
    if (p === 'haute' || p === 'high') return 'priorite-haute';
    if (p === 'basse' || p === 'low') return 'priorite-basse';
    return 'priorite-moyenne';
  }

  getTaskPriorityLabel(priority?: string): string {
    if (!priority) return 'Moyenne';
    const p = priority.toUpperCase();
    if (p === 'HAUTE' || p === 'HIGH') return 'Haute';
    if (p === 'BASSE' || p === 'LOW') return 'Basse';
    return 'Moyenne';
  }

  // ══════════════════════════════════════════════════
  // ÉQUIPES
  // ══════════════════════════════════════════════════

  openAssignTeamModal(project: ProjectDto): void {
    this.currentProjectForTeam = project;
    this.selectedTeamId = null;

    this.teamService.getMyTeams().subscribe({
      next: (teams) => this.ngZone.run(() => {
        this.teams = teams.filter(team => (team.members?.length || 0) > 0);
        if (this.teams.length === 0) {
          this.showToast('⚠️ Aucune équipe avec des membres disponible pour assignation.', 'error');
          return;
        }
        this.showTeamModal = true;
        this.cdr.detectChanges();
      }),
      error: () => alert('Impossible de charger vos équipes.')
    });
  }

  hasTeamMembers(team: TeamDto): boolean {
    return (team.members?.length || 0) > 0;
  }

  confirmAssignTeam(): void {
    if (!this.selectedTeamId || !this.currentProjectForTeam?.id) return;

    this.projectService.assignTeamToProject(this.currentProjectForTeam.id, this.selectedTeamId).subscribe({
      next: () => this.ngZone.run(() => {
        this.showTeamModal = false;
        this.currentProjectForTeam = null;
        this.selectedTeamId = null;
        this.fetchProjects();
        this.showToast('✅ Équipe assignée avec succès', 'success');
        this.cdr.detectChanges();
      }),
      error: (err) => alert('Erreur : ' + (err.error?.message || 'Erreur lors de l\'assignation'))
    });
  }

  closeTeamModal(): void {
    this.showTeamModal = false;
    this.currentProjectForTeam = null;
    this.selectedTeamId = null;
    this.cdr.detectChanges();
  }

  getTeamMemberCount(team: TeamDto): number {
    return (team as any).members?.length || (team as any).memberCount || 0;
  }

  // ══════════════════════════════════════════════════
  // TÂCHES
  // ══════════════════════════════════════════════════

  openCreateTaskModal(project: ProjectDto): void {
    this.currentProjectForTask = project;
    this.taskForm = {
      title: '', description: '', startDate: '', estimatedEndDate: '',
      criticite: 3, priority: 'MOYENNE', assignedToId: undefined, projectId: project.id
    };

    if (project.teamId) {
      this.teamService.getTeamById(project.teamId).subscribe({
        next: (team) => this.ngZone.run(() => {
          this.teamMembers = team.members || [];
          this.showTaskModal = true;
          setTimeout(() => this.calculateEndDate(), 100);
          this.cdr.detectChanges();
        }),
        error: () => alert('Impossible de charger les membres de l\'équipe.')
      });
    } else {
      alert('Ce projet n\'a pas encore d\'équipe assignée.');
    }
  }

  closeTaskModal(): void {
    this.showTaskModal = false;
    this.currentProjectForTask = null;
    this.taskForm = { title: '', description: '', startDate: '', criticite: 3, assignedToId: undefined };
    this.cdr.detectChanges();
  }

  saveTask(): void {
    if (!this.taskForm.title || !this.taskForm.assignedToId || !this.taskForm.criticite) {
      alert('Veuillez remplir tous les champs obligatoires.');
      return;
    }

    this.taskService.createTask(this.taskForm as TaskDto).subscribe({
      next: () => this.ngZone.run(() => {
        this.showToast('✅ Tâche créée avec succès !', 'success');
        if (this.showTasksModal && this.currentProjectForTask?.id) {
          this.loadTasksForProject(this.currentProjectForTask.id);
        }
        this.closeTaskModal();
        this.cdr.detectChanges();
      }),
      error: (err) => alert('Erreur : ' + (err.error?.message || 'Création échouée'))
    });
  }

  openViewTasksModal(project: ProjectDto): void {
    this.currentProjectTasks = project;
    this.taskSearchTerm = '';
    this.taskFilterPriority = '';
    this.taskFilterMember = '';
    this.currentPageTaches = 1;
    this.loadTasksForProject(project.id!);
  }

  loadTasksForProject(projectId: number): void {
    this.taskService.getTasksByProject(projectId).subscribe({
      next: (tasks) => this.ngZone.run(() => {
        this.projectTasks = [...tasks];
        this.currentPageTaches = 1;
        this.refreshTasksPagination();
        this.showTasksModal = true;
        this.cdr.detectChanges();
      }),
      error: () => alert('Erreur lors du chargement des tâches')
    });
  }

  closeTasksModal(): void {
    this.showTasksModal = false;
    this.currentProjectTasks = null;
    this.projectTasks = [];
    this.paginatedTasks = [];
    this.totalTaches = 0;
    this.taskSearchTerm = '';
    this.taskFilterPriority = '';
    this.taskFilterMember = '';
    this.cdr.detectChanges();
  }

  editTask(task: TaskDto): void {
    this.editingTask = { ...task };

    if (this.currentProjectTasks?.teamId) {
      this.teamService.getTeamById(this.currentProjectTasks.teamId).subscribe({
        next: (team) => this.ngZone.run(() => {
          this.teamMembers = team.members || [];
          this.taskFormModal = true;
          this.cdr.detectChanges();
        }),
        error: (err) => console.error(err)
      });
    } else {
      this.taskFormModal = true;
      this.cdr.detectChanges();
    }
  }

  closeTaskFormModal(): void {
    this.taskFormModal = false;
    this.editingTask = null;
    this.cdr.detectChanges();
  }

  saveTaskEdit(): void {
    if (!this.editingTask) return;

    this.taskService.updateTask(this.editingTask.id!, this.editingTask).subscribe({
      next: () => this.ngZone.run(() => {
        this.showToast('✅ Tâche modifiée avec succès !', 'success');
        this.closeTaskFormModal();
        if (this.currentProjectTasks) this.loadTasksForProject(this.currentProjectTasks.id!);
        this.cdr.detectChanges();
      }),
      error: (err) => this.showToast(err.error?.message || 'Modification échouée', 'error')
    });
  }

  deleteTask(taskId: number): void {
    this.taskIdToDelete = taskId;
    this.showDeleteTaskModal = true;
    this.cdr.detectChanges();
  }

  confirmDeleteTask(): void {
    if (!this.taskIdToDelete) return;

    this.taskService.deleteTask(this.taskIdToDelete).subscribe({
      next: () => this.ngZone.run(() => {
        this.showToast('🗑️ Tâche supprimée avec succès', 'success');
        if (this.showTasksModal && this.currentProjectTasks?.id) {
          this.loadTasksForProject(this.currentProjectTasks.id);
        } else {
          this.projectTasks = this.projectTasks.filter(t => t.id !== this.taskIdToDelete);
          this.refreshTasksPagination();
        }
        this.closeDeleteTaskModal();
        this.cdr.detectChanges();
      }),
      error: (err) => {
        this.showToast(err.error?.message || 'Suppression échouée', 'error');
        this.closeDeleteTaskModal();
      }
    });
  }

  closeDeleteTaskModal(): void {
    this.showDeleteTaskModal = false;
    this.taskIdToDelete = null;
    this.cdr.detectChanges();
  }

  validerTask(task: TaskDto): void {
    this.taskToValidate = task;
    this.validationAction = 'valider';
    this.validationComment = '';
    this.showValidationModal = true;
    this.cdr.detectChanges();
  }

  rejeterTask(task: TaskDto): void {
    this.taskToValidate = task;
    this.validationAction = 'rejeter';
    this.validationComment = '';
    this.showValidationModal = true;
    this.cdr.detectChanges();
  }

  closeValidationModal(): void {
    this.showValidationModal = false;
    this.taskToValidate = null;
    this.validationComment = '';
    this.cdr.detectChanges();
  }

  confirmValidation(): void {
    if (!this.taskToValidate?.id) return;

    if (this.validationAction === 'valider') {
      const payload: TaskDto = { ...this.taskToValidate, status: 'Terminé' };
      this.taskService.updateTask(this.taskToValidate.id, payload).subscribe({
        next: () => this.ngZone.run(() => {
          const idx = this.projectTasks.findIndex(t => t.id === this.taskToValidate!.id);
          if (idx !== -1) {
            this.projectTasks[idx] = { ...this.projectTasks[idx], status: 'Terminé' };
            this.projectTasks = [...this.projectTasks];
            this.refreshTasksPagination();
          }
          this.showToast('✅ Tâche validée !', 'success');
          this.closeValidationModal();
          this.cdr.detectChanges();
        }),
        error: (err) => { this.showToast(err.error?.message || 'Erreur lors de la validation', 'error'); this.closeValidationModal(); }
      });
    } else {
      this.taskService.rejectTask(this.taskToValidate.id, this.validationComment).subscribe({
        next: () => this.ngZone.run(() => {
          const idx = this.projectTasks.findIndex(t => t.id === this.taskToValidate!.id);
          if (idx !== -1) {
            this.projectTasks[idx] = {
              ...this.projectTasks[idx], status: 'En_cours',
              rejected: true, rejectionComment: this.validationComment
            };
            this.projectTasks = [...this.projectTasks];
            this.refreshTasksPagination();
          }
          this.showToast('❌ Tâche rejetée.', 'error');
          this.closeValidationModal();
          this.cdr.detectChanges();
        }),
        error: (err) => { this.showToast(err.error?.message || 'Erreur lors du rejet', 'error'); this.closeValidationModal(); }
      });
    }
  }

  // ══════════════════════════════════════════════════
  // TOAST
  // ══════════════════════════════════════════════════

  showToast(message: string, type: 'success' | 'error' = 'success'): void {
    this.toastMessage = message;
    this.toastType = type;
    this.cdr.detectChanges();
    setTimeout(() => { this.toastMessage = null; this.cdr.detectChanges(); }, 3000);
  }

  // ══════════════════════════════════════════════════
  // PAGINATION PROJETS
  // ══════════════════════════════════════════════════

  updatePaginatedProjects(): void {
    const start = (this.currentPageProjets - 1) * this.itemsPerPageProjets;
    const end = start + this.itemsPerPageProjets;
    this.paginatedProjects = this.projects.slice(start, end);
    this.totalProjets = this.projects.length;
  }

  goToPageProjets(page: number): void {
    const maxPage = Math.ceil(this.totalProjets / this.itemsPerPageProjets) || 1;
    if (page < 1 || page > maxPage) return;
    this.currentPageProjets = page;
    this.updatePaginatedProjects();
  }

  getProjetsTotalPages(): number {
    return Math.ceil(this.totalProjets / this.itemsPerPageProjets);
  }

  getProjetsRangeStart(): number {
    if (this.totalProjets === 0) return 0;
    return (this.currentPageProjets - 1) * this.itemsPerPageProjets + 1;
  }

  getProjetsRangeEnd(): number {
    return Math.min(this.currentPageProjets * this.itemsPerPageProjets, this.totalProjets);
  }

  getProjetsPageNumbers(): number[] {
    const total = this.getProjetsTotalPages();
    const current = this.currentPageProjets;
    const pages: number[] = [];
    if (total <= 7) {
      for (let i = 1; i <= total; i++) pages.push(i);
    } else if (current <= 4) {
      pages.push(1, 2, 3, 4, 5, -1, total);
    } else if (current >= total - 3) {
      pages.push(1, -1, total - 4, total - 3, total - 2, total - 1, total);
    } else {
      pages.push(1, -1, current - 1, current, current + 1, -1, total);
    }
    return pages;
  }

  // ══════════════════════════════════════════════════
  // PAGINATION TÂCHES
  // ══════════════════════════════════════════════════

  updatePaginatedTasks(): void {
    const filtered = this.getFilteredTasks();
    const start = (this.currentPageTaches - 1) * this.itemsPerPageTaches;
    const end = start + this.itemsPerPageTaches;
    this.paginatedTasks = filtered.slice(start, end);
    this.totalTaches = filtered.length;
  }

  goToPageTaches(page: number): void {
    const maxPage = Math.ceil(this.totalTaches / this.itemsPerPageTaches) || 1;
    if (page < 1 || page > maxPage) return;
    this.currentPageTaches = page;
    this.updatePaginatedTasks();
  }

  refreshTasksPagination(): void {
    this.currentPageTaches = 1;
    this.updatePaginatedTasks();
  }

  getTachesTotalPages(): number {
    return Math.ceil(this.totalTaches / this.itemsPerPageTaches);
  }

  getTachesRangeStart(): number {
    if (this.totalTaches === 0) return 0;
    return (this.currentPageTaches - 1) * this.itemsPerPageTaches + 1;
  }

  getTachesRangeEnd(): number {
    return Math.min(this.currentPageTaches * this.itemsPerPageTaches, this.totalTaches);
  }

  getTachesPageNumbers(): number[] {
    const total = this.getTachesTotalPages();
    const current = this.currentPageTaches;
    const pages: number[] = [];
    if (total <= 7) {
      for (let i = 1; i <= total; i++) pages.push(i);
    } else if (current <= 4) {
      pages.push(1, 2, 3, 4, 5, -1, total);
    } else if (current >= total - 3) {
      pages.push(1, -1, total - 4, total - 3, total - 2, total - 1, total);
    } else {
      pages.push(1, -1, current - 1, current, current + 1, -1, total);
    }
    return pages;
  }

  // ══════════════════════════════════════════════════
  // NAVIGATION
  // ══════════════════════════════════════════════════

  goToDashboard(): void {
    const roles = this.authService.getRoles();
    if (roles.includes('ADMIN')) { this.router.navigate(['/admin/dashboard']); }
    else if (roles.includes('CHEF_PROJET')) { this.router.navigate(['/chef-projet/dashboard']); }
    else if (roles.includes('MANAGER')) { this.router.navigate(['/manager/dashboard']); }
    else if (roles.includes('RESPONSABLE_CONTRAT')) { this.router.navigate(['/responsable-contrat/dashboard']); }
    else if (roles.includes('MEMBRE_EQUIPE')) { this.router.navigate(['/membre-equipe/dashboard']); }
    else { this.router.navigate(['/admin/dashboard']); }
  }

  // ══════════════════════════════════════════════════
  // CALCUL DATES TÂCHES
  // ══════════════════════════════════════════════════

  calculateEndDate(): void {
    if (!this.taskForm.startDate || !this.taskForm.criticite) return;
    const startDate = new Date(this.taskForm.startDate);
    const criticite = Number(this.taskForm.criticite);
    const priority = this.taskForm.priority || 'MOYENNE';
    let daysToAdd = criticite;
    switch (priority) {
      case 'HAUTE': daysToAdd = Math.max(1, Math.floor(criticite * 0.7)); break;
      case 'BASSE': daysToAdd = Math.floor(criticite * 1.5); break;
    }
    const endDate = new Date(startDate);
    endDate.setDate(startDate.getDate() + daysToAdd);
    this.taskForm.estimatedEndDate = this.formatDate(endDate);
    this.cdr.detectChanges();
  }

  calculateEditEndDate(): void {
    if (!this.editingTask?.startDate || !this.editingTask?.criticite) return;
    const startDate = new Date(this.editingTask.startDate);
    const criticite = Number(this.editingTask.criticite);
    const priority = this.editingTask.priority || 'MOYENNE';
    let daysToAdd = criticite;
    switch (priority) {
      case 'HAUTE': daysToAdd = Math.max(1, Math.floor(criticite * 0.7)); break;
      case 'BASSE': daysToAdd = Math.floor(criticite * 1.5); break;
    }
    const endDate = new Date(startDate);
    endDate.setDate(startDate.getDate() + daysToAdd);
    this.editingTask.estimatedEndDate = this.formatDate(endDate);
    this.cdr.detectChanges();
  }

  formatDate(date: Date): string {
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');
    return `${year}-${month}-${day}`;
  }

  getWorkingDaysFromCriticiteAndPriority(criticite: number, priority: string): number {
    const baseDays: Record<number, number> = { 1: 1, 2: 2, 3: 3, 5: 5, 8: 8, 13: 13 };
    const base = baseDays[criticite] ?? 3;
    const multiplier = priority === 'HAUTE' ? 0.7 : priority === 'BASSE' ? 1.5 : 1;
    return Math.max(1, Math.round(base * multiplier));
  }

  addWorkingDays(startDate: Date, days: number): Date {
    const result = new Date(startDate);
    let addedDays = 0;
    while (addedDays < days) {
      result.setDate(result.getDate() + 1);
      if (result.getDay() !== 0 && result.getDay() !== 6) addedDays++;
    }
    return result;
  }
}
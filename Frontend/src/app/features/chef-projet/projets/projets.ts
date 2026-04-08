import {
  Component,
  OnInit,
  signal,
  ChangeDetectorRef,
  NgZone
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';
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

  teams: TeamDto[] = [];
  selectedTeamId: number | null = null;
  showTeamModal = false;
  currentProjectForTeam: ProjectDto | null = null;

  showTaskModal = false;
  showTasksModal = false;

  toastMessage: string | null = null;
  toastType: 'success' | 'error' = 'success';

  currentProjectTasks: ProjectDto | null = null;
  projectTasks: TaskDto[] = [];
  editingTask: TaskDto | null = null;
  taskFormModal = false;
  currentProjectForTask: ProjectDto | null = null;

  taskForm: Partial<TaskDto> = {
    title: '',
    description: '',
    dueDate: '',
    assignedToId: undefined
  };

  teamMembers: any[] = [];

  searchTerm = '';
  selectedStatus = '';

  showForm = false;
  isEditMode = false;
  editingProjectId: number | null = null;

  projectForm: ProjectDto = this.getEmptyForm();

  currentUser = signal<any>(null);

  constructor(
    private projectService: ProjectService,
    private teamService: TeamService,
    private taskService: TaskService,
    public authService: AuthService,
    private cdr: ChangeDetectorRef,
    private ngZone: NgZone
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
    this.cdr.detectChanges();

    const query = this.searchTerm.trim() || undefined;
    const status = this.selectedStatus || undefined;

    this.projectService.getMyProjects(query, status).subscribe({
      next: (projects: ProjectDto[]) => {
        this.ngZone.run(() => {
          this.projects = [...(projects ?? [])];
          this.loading = false;
          this.error = null;
          this.cdr.detectChanges();
        });
      },
      error: (err) => {
        this.ngZone.run(() => {
          console.error('Erreur chargement projets:', err);
          this.error = err?.error?.message || 'Erreur lors du chargement des projets.';
          this.loading = false;
          this.cdr.detectChanges();
        });
      }
    });
  }

  openCreateForm(): void {
    this.showForm = true;
    this.isEditMode = false;
    this.editingProjectId = null;
    this.formError = null;
    this.projectForm = this.getEmptyForm();
    this.cdr.detectChanges();
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
      progressPercentage: Number(this.projectForm.progressPercentage ?? 0),
      riskLevel: this.projectForm.riskLevel,
      startDate: this.projectForm.startDate,
      endDate: this.projectForm.endDate,
      status: this.projectForm.status
    };

    if (!payload.name) {
      this.formError = 'Le nom du projet est obligatoire.';
      this.cdr.detectChanges();
      return;
    }

    if (!payload.client) {
      this.formError = 'Le client est obligatoire.';
      this.cdr.detectChanges();
      return;
    }

    if (!payload.startDate || !payload.endDate) {
      this.formError = 'Les dates sont obligatoires.';
      this.cdr.detectChanges();
      return;
    }

    if (payload.progressPercentage < 0 || payload.progressPercentage > 100) {
      this.formError = 'La progression doit être comprise entre 0 et 100.';
      this.cdr.detectChanges();
      return;
    }

    if (payload.startDate > payload.endDate) {
      this.formError = 'La date de début doit être antérieure à la date de fin.';
      this.cdr.detectChanges();
      return;
    }

    if (this.isEditMode && this.editingProjectId !== null) {
      this.projectService.updateProject(this.editingProjectId, payload).subscribe({
        next: () => {
          this.ngZone.run(() => {
            this.closeForm();
            this.fetchProjects();
            this.showToast('✅ Projet modifié avec succès', 'success');
            this.cdr.detectChanges();
          });
        },
        error: (err) => {
          this.ngZone.run(() => {
            console.error('Erreur modification projet:', err);
            this.formError = err?.error?.message || 'Erreur lors de la modification.';
            this.cdr.detectChanges();
          });
        }
      });
      return;
    }

    this.projectService.createProject(payload).subscribe({
      next: () => {
        this.ngZone.run(() => {
          this.closeForm();
          this.fetchProjects();
          this.showToast('✅ Projet créé avec succès', 'success');
          this.cdr.detectChanges();
        });
      },
      error: (err) => {
        this.ngZone.run(() => {
          console.error('Erreur création projet:', err);
          this.formError = err?.error?.message || 'Erreur lors de la création.';
          this.cdr.detectChanges();
        });
      }
    });
  }

  deleteProject(project: ProjectDto): void {
    if (!project.id) return;

    const confirmed = confirm(`Supprimer le projet "${project.name}" ?`);
    if (!confirmed) return;

    this.projectService.setDeletedStatus(project.id, true).subscribe({
      next: () => {
        this.ngZone.run(() => {
          this.projects = this.projects.filter(p => p.id !== project.id);
          this.cdr.detectChanges();
          this.showToast('🗑️ Projet supprimé avec succès', 'success');
        });
      },
      error: (err) => {
        this.ngZone.run(() => {
          console.error('Erreur suppression projet:', err);
          this.error = err?.error?.message || 'Erreur lors de la suppression du projet.';
          this.cdr.detectChanges();
        });
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

  openAssignTeamModal(project: ProjectDto): void {
    this.currentProjectForTeam = project;
    this.selectedTeamId = null;
    this.cdr.detectChanges();

    this.teamService.getMyTeams().subscribe({
      next: (teams) => {
        this.ngZone.run(() => {
          this.teams = [...teams];
          this.showTeamModal = true;
          this.cdr.detectChanges();
        });
      },
      error: (err) => {
        console.error('Erreur chargement équipes', err);
        alert('Impossible de charger vos équipes.');
      }
    });
  }

  confirmAssignTeam(): void {
    if (!this.selectedTeamId || !this.currentProjectForTeam?.id) return;

    this.projectService.assignTeamToProject(this.currentProjectForTeam.id, this.selectedTeamId).subscribe({
      next: () => {
        this.ngZone.run(() => {
          this.showTeamModal = false;
          this.currentProjectForTeam = null;
          this.selectedTeamId = null;
          this.fetchProjects();
          this.showToast('✅ Équipe assignée avec succès', 'success');
          this.cdr.detectChanges();
        });
      },
      error: (err) => {
        console.error(err);
        const errorMsg = err.error?.message || 'Erreur lors de l\'assignation';
        if (errorMsg.includes('déjà affectée') || errorMsg.includes('already assigned')) {
          alert('❌ ' + errorMsg);
        } else {
          alert('Erreur : ' + errorMsg);
        }
      }
    });
  }

  closeTeamModal(): void {
    this.showTeamModal = false;
    this.currentProjectForTeam = null;
    this.selectedTeamId = null;
    this.cdr.detectChanges();
  }

  openCreateTaskModal(project: ProjectDto): void {
    this.currentProjectForTask = project;
    this.taskForm = {
      title: '',
      description: '',
      dueDate: '',
      assignedToId: undefined,
      projectId: project.id
    };

    if (project.teamId) {
      this.teamService.getTeamById(project.teamId).subscribe({
        next: (team) => {
          this.ngZone.run(() => {
            this.teamMembers = team.members || [];
            this.showTaskModal = true;
            this.cdr.detectChanges();
          });
        },
        error: (err) => {
          console.error('Erreur chargement équipe', err);
          alert('Impossible de charger les membres de l\'équipe.');
        }
      });
    } else {
      alert('Ce projet n’a pas encore d’équipe assignée.');
    }
  }

  closeTaskModal(): void {
    this.showTaskModal = false;
    this.currentProjectForTask = null;
    this.taskForm = {
      title: '',
      description: '',
      dueDate: '',
      assignedToId: undefined
    };
    this.cdr.detectChanges();
  }

  saveTask(): void {
    if (!this.taskForm.title || !this.taskForm.assignedToId || !this.taskForm.dueDate) {
      alert('Veuillez remplir tous les champs obligatoires (titre, assigné à, date limite).');
      return;
    }

    this.taskService.createTask(this.taskForm as TaskDto).subscribe({
      next: () => {
        this.ngZone.run(() => {
          this.showToast('✅ Tâche créée avec succès !', 'success');
          this.closeTaskModal();
          this.cdr.detectChanges();
        });
      },
      error: (err) => {
        console.error(err);
        alert('Erreur : ' + (err.error?.message || 'Création échouée'));
      }
    });
  }

  openViewTasksModal(project: ProjectDto): void {
    this.currentProjectTasks = project;
    this.loadTasksForProject(project.id!);
  }

  loadTasksForProject(projectId: number): void {
    this.taskService.getTasksByProject(projectId).subscribe({
      next: (tasks) => {
        this.ngZone.run(() => {
          this.projectTasks = [...tasks];
          this.showTasksModal = true;
          this.cdr.detectChanges();
        });
      },
      error: (err) => {
        console.error(err);
        alert('Erreur lors du chargement des tâches');
      }
    });
  }

  closeTasksModal(): void {
    this.showTasksModal = false;
    this.currentProjectTasks = null;
    this.projectTasks = [];
    this.cdr.detectChanges();
  }

  editTask(task: TaskDto): void {
    this.editingTask = { ...task };

    if (this.currentProjectTasks?.teamId) {
      this.teamService.getTeamById(this.currentProjectTasks.teamId).subscribe({
        next: (team) => {
          this.ngZone.run(() => {
            this.teamMembers = team.members || [];
            this.taskFormModal = true;
            this.cdr.detectChanges();
          });
        },
        error: (err) => console.error(err)
      });
    } else {
      this.taskFormModal = true;
      this.cdr.detectChanges();
    }
  }

  deleteTask(taskId: number): void {
    if (confirm('Supprimer cette tâche ?')) {
      this.taskService.deleteTask(taskId).subscribe({
        next: () => {
          this.ngZone.run(() => {
            this.projectTasks = this.projectTasks.filter(t => t.id !== taskId);
            this.showToast('🗑️ Tâche supprimée avec succès', 'success');
            this.cdr.detectChanges();
          });
        },
        error: (err) => {
          console.error(err);
          this.showToast(err.error?.message || 'Suppression échouée', 'error');
        }
      });
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
      next: () => {
        this.ngZone.run(() => {
          this.showToast('✅ Tâche modifiée avec succès !', 'success');
          this.closeTaskFormModal();
          if (this.currentProjectTasks) {
            this.loadTasksForProject(this.currentProjectTasks.id!);
          }
          this.cdr.detectChanges();
        });
      },
      error: (err) => {
        console.error(err);
        this.showToast(err.error?.message || 'Modification échouée', 'error');
      }
    });
  }

  getTaskStatusLabel(status?: string): string {
    switch (status) {
      case 'A_faire':
        return 'À faire';
      case 'En_cours':
        return 'En cours';
      case 'Terminé':
        return 'Terminé';
      case 'Validation':
        return 'Validation';
      default:
        return status ?? 'À faire';
    }
  }

  getTaskStatusClass(status?: string): string {
    switch (status) {
      case 'A_faire':
        return 'status-a-faire';
      case 'En_cours':
        return 'status-en-cours';
      case 'Terminé':
        return 'status-termine';
      case 'Validation':
        return 'status-validation';
      default:
        return '';
    }
  }

  showToast(message: string, type: 'success' | 'error' = 'success'): void {
    this.toastMessage = message;
    this.toastType = type;
    this.cdr.detectChanges();

    setTimeout(() => {
      this.toastMessage = null;
      this.cdr.detectChanges();
    }, 3000);
  }
}
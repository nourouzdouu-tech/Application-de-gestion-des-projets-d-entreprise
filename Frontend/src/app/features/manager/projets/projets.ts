import { Component, computed, signal, inject, OnInit } from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import {
  ManagerService,
  ManagerProjectItemDto,
  ManagerProjectReviewDto,
  ChefProjetSummary
} from '../../../core/services/manager.service';

interface ReviewProject {
  id: number;
  code: string;
  name: string;
  representative: string;
  createdAt: string;
  status: string;
  client: string;
  managerName: string | null;
}

interface ChefProjetOption {
  id: number;
  fullName: string;
  email?: string;
}

@Component({
  selector: 'app-review-projets',
  standalone: true,
  imports: [CommonModule, FormsModule, DatePipe],
  templateUrl: './projets.html',
  styleUrl: './projets.css',
})
export class ReviewProjetsComponent implements OnInit {
  private managerService = inject(ManagerService);

  readonly searchTerm = signal('');
  readonly currentPage = signal(1);
  readonly pageSize = 4;

  readonly loading = signal(false);
  readonly submitting = signal(false);
  readonly errorMessage = signal<string | null>(null);

  readonly projects = signal<ReviewProject[]>([]);
  readonly chefProjetOptions = signal<ChefProjetOption[]>([]);

  readonly isAssignModalOpen = signal(false);
  readonly selectedProject = signal<ReviewProject | null>(null);

  assignForm = {
    projectId: null as number | null,
    chefProjetId: null as number | null,
    comment: '',
  };

  ngOnInit(): void {
    this.loadManagerProjects();
    this.loadChefsProjet();
  }

  private loadManagerProjects(): void {
    this.loading.set(true);
    this.errorMessage.set(null);

    this.managerService.getManagerProjects().subscribe({
      next: (data: ManagerProjectItemDto[]) => {
        const mapped = data.map((project) => ({
          id: project.id,
          code: this.buildCode(project.projectName),
          name: project.projectName,
          representative: project.managerName ?? 'Non défini',
          createdAt: project.createdAt,
          status: this.mapBackendStatus(project.status),
          client: project.client,
          managerName: project.managerName ?? null,
        }));

        this.projects.set(mapped);
        this.loading.set(false);
      },
      error: (err) => {
        console.error('Erreur chargement projets manager', err);
        this.errorMessage.set('Erreur lors du chargement des projets.');
        this.loading.set(false);
      }
    });
  }

  private loadChefsProjet(): void {
    this.managerService.getChefsProjetForSelect().subscribe({
      next: (data: ChefProjetSummary[]) => {
        const mapped = data.map((chef: any) => ({
          id: chef.id,
          fullName:
            chef.fullName ||
            [chef.prenom, chef.nom].filter(Boolean).join(' ') ||
            chef.email ||
            `Chef #${chef.id}`,
          email: chef.email
        }));

        this.chefProjetOptions.set(mapped);
      },
      error: (err) => {
        console.error('Erreur chargement chefs de projet', err);
      }
    });
  }

  readonly filteredProjects = computed(() => {
    const term = this.searchTerm().trim().toLowerCase();

    if (!term) return this.projects();

    return this.projects().filter((project) =>
      [
        project.name,
        project.representative,
        project.status,
        project.code,
        project.client,
      ]
        .join(' ')
        .toLowerCase()
        .includes(term)
    );
  });

  readonly totalPages = computed(() =>
    Math.max(1, Math.ceil(this.filteredProjects().length / this.pageSize))
  );

  readonly paginatedProjects = computed(() => {
    const page = this.currentPage();
    const start = (page - 1) * this.pageSize;
    const end = start + this.pageSize;
    return this.filteredProjects().slice(start, end);
  });

  readonly countAValider = computed(
    () =>
      this.projects().filter(
        (p) =>
          p.status === 'À valider' || p.status === 'En cours de validation'
      ).length
  );

  readonly countAssignes = computed(
    () => this.projects().filter((p) => p.status === 'Assigné').length
  );

  readonly displayRange = computed(() => {
    const total = this.filteredProjects().length;

    if (total === 0) {
      return { start: 0, end: 0, total: 0 };
    }

    const start = (this.currentPage() - 1) * this.pageSize + 1;
    const end = Math.min(this.currentPage() * this.pageSize, total);

    return { start, end, total };
  });

  readonly pageNumbers = computed(() =>
    Array.from({ length: this.totalPages() }, (_, index) => index + 1)
  );

  onSearchChange(): void {
    this.currentPage.set(1);
  }

  goToPage(page: number): void {
    if (page >= 1 && page <= this.totalPages()) {
      this.currentPage.set(page);
    }
  }

  previousPage(): void {
    if (this.currentPage() > 1) {
      this.currentPage.set(this.currentPage() - 1);
    }
  }

  nextPage(): void {
    if (this.currentPage() < this.totalPages()) {
      this.currentPage.set(this.currentPage() + 1);
    }
  }

  openAssignModal(project: ReviewProject): void {
    this.selectedProject.set(project);
    this.assignForm = {
      projectId: project.id,
      chefProjetId: null,
      comment: '',
    };
    this.isAssignModalOpen.set(true);
    document.body.style.overflow = 'hidden';
  }

  closeAssignModal(): void {
    this.isAssignModalOpen.set(false);
    this.selectedProject.set(null);
    this.assignForm = {
      projectId: null,
      chefProjetId: null,
      comment: '',
    };
    document.body.style.overflow = '';
  }

  rejectProject(): void {
    if (!this.assignForm.projectId) return;

    const payload: ManagerProjectReviewDto = {
      projectId: this.assignForm.projectId,
      chefProjetId: this.assignForm.chefProjetId ?? 0,
      commentaire: this.assignForm.comment?.trim() || 'Projet rejeté par le manager',
      decision: 'REJETER'
    };

    this.submitting.set(true);

    this.managerService.reviewProject(payload).subscribe({
      next: () => {
        this.submitting.set(false);
        this.closeAssignModal();
        this.loadManagerProjects();
      },
      error: (err) => {
        console.error('Erreur rejet projet', err);
        this.submitting.set(false);
        alert("Erreur lors du rejet du projet.");
      }
    });
  }

  validateAssignment(): void {
    if (!this.assignForm.projectId || !this.assignForm.chefProjetId) return;

    const payload: ManagerProjectReviewDto = {
      projectId: this.assignForm.projectId,
      chefProjetId: this.assignForm.chefProjetId,
      commentaire: this.assignForm.comment?.trim() || 'Projet validé par le manager',
      decision: 'VALIDER'
    };

    this.submitting.set(true);

    this.managerService.reviewProject(payload).subscribe({
      next: () => {
        this.submitting.set(false);
        this.closeAssignModal();
        this.loadManagerProjects();
      },
      error: (err) => {
        console.error('Erreur validation assignation', err);
        this.submitting.set(false);
        alert("Erreur lors de l'assignation du chef de projet.");
      }
    });
  }

  trackByProjectId(_: number, item: ReviewProject): number {
    return item.id;
  }

  private buildCode(projectName: string): string {
    if (!projectName) return 'PR';
    return projectName
      .split(' ')
      .filter(Boolean)
      .slice(0, 2)
      .map(word => word.charAt(0).toUpperCase())
      .join('') || 'PR';
  }

  private mapBackendStatus(status: string | null | undefined): string {
    switch (status) {
      case 'EN_VALIDATION':
        return 'En cours de validation';
      case 'PRE_VALIDE':
        return 'Assigné';
      case 'VALIDE':
        return 'Assigné';
      default:
        return status || 'À valider';
    }
  }
}
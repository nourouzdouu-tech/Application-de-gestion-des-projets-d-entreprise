import { Component, computed, signal, inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ProjectService, ProjectDto } from '../../../core/services/project.service';
import { Router } from '@angular/router';

interface MemberProject {
  id: number;
  name: string;
  client: string;
  chefProjet: string;
  dueDate: string;
  status: 'En cours' | 'Urgent' | 'Terminé' | 'En pause';
}

interface ActivityItem {
  id: number;
  text: string;
  date: string;
}

@Component({
  selector: 'app-membre-equipe-projets',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './projets.html',
  styleUrls: ['./projets.css']
})
export class MembreEquipeProjets implements OnInit {
  private projectService = inject(ProjectService);

  searchTerm = signal('');
  loading = signal(false);
  error = signal('');
  private router = inject(Router);

  projects = signal<MemberProject[]>([]);

  currentPage = signal(1);
  itemsPerPage = 4;

  activities = signal<ActivityItem[]>([
    {
      id: 1,
      text: 'Chargement des activités récentes du projet...',
      date: 'Maintenant'
    }
  ]);
goToDashboard() {
  this.router.navigate(['/membre-equipe/dashboard']);
}
  ngOnInit(): void {
    this.loadProjects();
  }

  loadProjects(): void {
    this.loading.set(true);
    this.error.set('');

    this.projectService.getMyAssignedProjects().subscribe({
      next: (data: ProjectDto[]) => {
        const mappedProjects = data.map(project => this.mapToMemberProject(project));
        this.projects.set(mappedProjects);
        this.currentPage.set(1);
        this.loading.set(false);

        this.activities.set(
          mappedProjects.length > 0
            ? mappedProjects.slice(0, 3).map((project, index) => ({
                id: index + 1,
                text: `Projet chargé : ${project.name}`,
                date: index === 0 ? 'À l’instant' : `Il y a ${index + 1} heure(s)`
              }))
            : [
                {
                  id: 1,
                  text: 'Aucune activité récente.',
                  date: 'Maintenant'
                }
              ]
        );
      },
      error: (err) => {
        console.error('Erreur chargement projets membre équipe :', err);
        this.error.set('Erreur lors du chargement des projets.');
        this.projects.set([]);
        this.currentPage.set(1);
        this.loading.set(false);
      }
    });
  }

  filteredProjects = computed(() => {
    const search = this.searchTerm().trim().toLowerCase();

    if (!search) {
      return this.projects();
    }

    return this.projects().filter(project =>
      project.name.toLowerCase().includes(search) ||
      project.client.toLowerCase().includes(search) ||
      project.chefProjet.toLowerCase().includes(search)
    );
  });

  totalProjects = computed(() => this.projects().length);

  inProgressProjects = computed(() =>
    this.projects().filter(project => project.status === 'En cours').length
  );

  nearDeadlines = computed(() =>
    this.projects().filter(project => project.status === 'Urgent').length
  );

  totalPages = computed(() => {
    const total = Math.ceil(this.filteredProjects().length / this.itemsPerPage);
    return total > 0 ? total : 1;
  });

  pages = computed(() =>
    Array.from({ length: this.totalPages() }, (_, i) => i + 1)
  );

  paginatedProjects = computed(() => {
    const start = (this.currentPage() - 1) * this.itemsPerPage;
    const end = start + this.itemsPerPage;
    return this.filteredProjects().slice(start, end);
  });

  rangeStart = computed(() =>
    this.filteredProjects().length === 0
      ? 0
      : (this.currentPage() - 1) * this.itemsPerPage + 1
  );

  rangeEnd = computed(() =>
    this.filteredProjects().length === 0
      ? 0
      : Math.min(this.currentPage() * this.itemsPerPage, this.filteredProjects().length)
  );

  onSearch(value: string): void {
    this.searchTerm.set(value);
    this.currentPage.set(1);
  }

  setPage(page: number): void {
    if (page >= 1 && page <= this.totalPages()) {
      this.currentPage.set(page);
    }
  }

  nextPage(): void {
    this.setPage(this.currentPage() + 1);
  }

  prevPage(): void {
    this.setPage(this.currentPage() - 1);
  }

  getStatusClass(status: string): string {
    switch (status) {
      case 'En cours':
        return 'badge-dark';
      case 'Urgent':
        return 'badge-warning';
      case 'Terminé':
        return 'badge-success';
      case 'En pause':
        return 'badge-muted';
      default:
        return 'badge-muted';
    }
  }

  getProjectIcon(_: string): string {
    return '📁';
  }

  private mapToMemberProject(project: ProjectDto): MemberProject {
    return {
      id: project.id ?? 0,
      name: project.name,
      client: project.client,
      chefProjet: project.chefProjetName || 'Non assigné',
      dueDate: this.formatDueDate(project.endDate),
      status: this.mapStatus(project.status)
    };
  }

  private mapStatus(status?: string): 'En cours' | 'Urgent' | 'Terminé' | 'En pause' {
    switch ((status || '').toUpperCase()) {
      case 'EN_COURS':
        return 'En cours';
      case 'PRE_VALIDE':
      case 'EN_VALIDATION':
        return 'Urgent';
      case 'VALIDE':
      case 'CLOTURE':
        return 'Terminé';
      case 'REJETE':
        return 'En pause';
      default:
        return 'En cours';
    }
  }

  private formatDueDate(date?: string): string {
    if (!date) return 'Non définie';

    const parsed = new Date(date);
    if (isNaN(parsed.getTime())) return date;

    return parsed.toLocaleDateString('fr-FR', {
      day: '2-digit',
      month: '2-digit',
      year: 'numeric'
    });
  }
  trackByProjectId(index: number, project: MemberProject): number {
  return project.id;
}
}
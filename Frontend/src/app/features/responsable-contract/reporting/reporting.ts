import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';
import { ProjectDto, ProjectService } from '../../../core/services/project.service';

type ReportingTab = 'all' | 'validation' | 'prevalide' | 'encours' | 'rejete';

@Component({
  selector: 'app-reporting',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule],
  templateUrl: './reporting.html',
  styleUrl: './reporting.css'
})
export class Reporting implements OnInit {
  private projectService = inject(ProjectService);

  loading = signal(false);
  error = signal<string | null>(null);

  projects = signal<ProjectDto[]>([]);
  searchTerm = signal('');
  activeTab = signal<ReportingTab>('all');

  // PAGINATION
  currentPage = signal(1);
  pageSize = 6;

  ngOnInit(): void {
    this.loadProjects();
  }

  loadProjects(): void {
    this.loading.set(true);
    this.error.set(null);

    this.projectService.getAllProjects().subscribe({
      next: (projects) => {
        this.projects.set(projects ?? []);
        this.loading.set(false);
      },
      error: (err) => {
        console.error('Erreur chargement reporting RC:', err);
        this.error.set('Erreur lors du chargement des projets.');
        this.loading.set(false);
      }
    });
  }

  setTab(tab: ReportingTab): void {
    this.activeTab.set(tab);
    this.currentPage.set(1);
  }

  onSearch(value: string): void {
    this.searchTerm.set(value);
    this.currentPage.set(1);
  }

  filteredProjects = computed(() => {
    const search = this.searchTerm().trim().toLowerCase();
    const tab = this.activeTab();

    return this.projects().filter((project) => {
      const matchesSearch =
        !search ||
        project.name?.toLowerCase().includes(search) ||
        project.client?.toLowerCase().includes(search) ||
        project.description?.toLowerCase().includes(search) ||
        project.teamName?.toLowerCase().includes(search);

      const status = project.status ?? '';

      const matchesTab =
        tab === 'all' ||
        (tab === 'validation' && status === 'EN_VALIDATION') ||
        (tab === 'prevalide' && status === 'PRE_VALIDE') ||
        (tab === 'encours' && status === 'EN_COURS') ||
        (tab === 'rejete' && status === 'REJETE');

      return matchesSearch && matchesTab;
    });
  });

  // PAGINATION COMPUTED
  totalPages = computed(() => {
    const total = this.filteredProjects().length;
    return Math.max(1, Math.ceil(total / this.pageSize));
  });

  paginatedProjects = computed(() => {
    const start = (this.currentPage() - 1) * this.pageSize;
    const end = start + this.pageSize;
    return this.filteredProjects().slice(start, end);
  });

  pages = computed(() =>
    Array.from({ length: this.totalPages() }, (_, i) => i + 1)
  );

  goToPage(page: number): void {
    if (page < 1 || page > this.totalPages()) return;
    this.currentPage.set(page);
  }

  getRangeStart(): number {
    if (this.filteredProjects().length === 0) return 0;
    return (this.currentPage() - 1) * this.pageSize + 1;
  }

  getRangeEnd(): number {
    return Math.min(this.currentPage() * this.pageSize, this.filteredProjects().length);
  }

  totalProjects = computed(() => this.projects().length);

  validationProjects = computed(() =>
    this.projects().filter((p) => p.status === 'EN_VALIDATION').length
  );

  preValideProjects = computed(() =>
    this.projects().filter((p) => p.status === 'PRE_VALIDE').length
  );

  enCoursProjects = computed(() =>
    this.projects().filter((p) => p.status === 'EN_COURS').length
  );

  rejectedProjects = computed(() =>
    this.projects().filter((p) => p.status === 'REJETE').length
  );

  activeClients = computed(() =>
    new Set(this.projects().map((p) => p.client).filter(Boolean)).size
  );

  projectsWithoutTeam = computed(() =>
    this.projects().filter((p) => !p.teamId).length
  );

  projectsWithoutManager = computed(() =>
    this.projects().filter((p: any) => !p.managerId).length
  );

  averageProgress = computed(() => {
    const list = this.projects();
    if (!list.length) return 0;

    const total = list.reduce((sum, p) => sum + p.progressPercentage, 0);
    return Math.round(total / list.length);
  });

  getRiskClass(risk?: string): string {
    switch (risk) {
      case 'ELEVE':
      case 'HIGH':
        return 'badge badge-danger';
      case 'MOYEN':
      case 'MEDIUM':
        return 'badge badge-warning';
      case 'FAIBLE':
      case 'LOW':
        return 'badge badge-success';
      default:
        return 'badge badge-neutral';
    }
  }

  getStatusClass(status?: string): string {
    switch (status) {
      case 'EN_VALIDATION':
        return 'status-pill status-validation';
      case 'PRE_VALIDE':
        return 'status-pill status-prevalide';
      case 'EN_COURS':
        return 'status-pill status-encours';
      case 'REJETE':
        return 'status-pill status-rejete';
      case 'VALIDE':
        return 'status-pill status-valide';
      default:
        return 'status-pill status-neutral';
    }
  }

  getProgressBarClass(progress?: number): string {
    if ((progress ?? 0) >= 80) return 'progress-fill progress-good';
    if ((progress ?? 0) >= 40) return 'progress-fill progress-medium';
    return 'progress-fill progress-low';
  }

  formatStatus(status?: string): string {
    switch (status) {
      case 'EN_VALIDATION': return 'En validation';
      case 'PRE_VALIDE': return 'Pré-validé';
      case 'EN_COURS': return 'En cours';
      case 'REJETE': return 'Rejeté';
      case 'VALIDE': return 'Validé';
      default: return status || '—';
    }
  }

  formatRisk(risk?: string): string {
    switch (risk) {
      case 'FAIBLE': return 'Faible';
      case 'MOYEN': return 'Moyen';
      case 'ELEVE': return 'Élevé';
      default: return risk || '—';
    }
  }
}
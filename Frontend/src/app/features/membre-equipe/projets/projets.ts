import { Component, computed, signal, inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ProjectService, ProjectDto } from '../../../core/services/project.service';

interface MemberProject {
  id: number;
  name: string;
  client: string;
  chefProjet: string;
  progress: number;
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

  projects = signal<MemberProject[]>([]);

  activities = signal<ActivityItem[]>([
    {
      id: 1,
      text: 'Chargement des activités récentes du projet...',
      date: 'Maintenant'
    }
  ]);

  ngOnInit(): void {
    this.loadProjects();
  }

  loadProjects(): void {
    this.loading.set(true);
    this.error.set('');

    this.projectService.getMyAssignedProjects().subscribe({
      next: (data: ProjectDto[]) => {
        const mappedProjects = data.map((project) => this.mapToMemberProject(project));
        this.projects.set(mappedProjects);
        this.loading.set(false);

        this.activities.set(
          mappedProjects.slice(0, 3).map((project, index) => ({
            id: index + 1,
            text: `Projet chargé : ${project.name}`,
            date: index === 0 ? 'À l’instant' : `Il y a ${index + 1} heure(s)`
          }))
        );
      },
      error: (err) => {
        console.error('Erreur chargement projets membre équipe :', err);
        this.error.set('Erreur lors du chargement des projets.');
        this.projects.set([]);
        this.loading.set(false);
      }
    });
  }

  filteredProjects = computed(() => {
    const search = this.searchTerm().trim().toLowerCase();
    if (!search) return this.projects();

    return this.projects().filter(p =>
      p.name.toLowerCase().includes(search) ||
      p.client.toLowerCase().includes(search) ||
      p.chefProjet.toLowerCase().includes(search)
    );
  });

  totalProjects = computed(() => this.projects().length);

  inProgressProjects = computed(() =>
    this.projects().filter(p => p.status === 'En cours').length
  );

  nearDeadlines = computed(() =>
    this.projects().filter(p => p.status === 'Urgent').length
  );

  onSearch(value: string): void {
    this.searchTerm.set(value);
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

  getProjectIcon(name: string): string {
    const lower = name.toLowerCase();
    if (lower.includes('site')) return '🗂️';
    if (lower.includes('mobile')) return '📱';
    if (lower.includes('audit')) return '🛡️';
    return '📁';
  }

  private mapToMemberProject(project: ProjectDto): MemberProject {
    return {
      id: project.id ?? 0,
      name: project.name,
      client: project.client,
      chefProjet: project.chefProjetName || 'Non assigné',
      progress: project.progressPercentage ?? 0,
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
      month: 'short',
      year: 'numeric'
    });
  }
}
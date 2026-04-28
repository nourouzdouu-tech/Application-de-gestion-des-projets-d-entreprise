import { Component, signal, computed, inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { TaskService, TaskDto, TaskStatus } from '../../../core/services/task.service';

export type Priorite = 'Haute' | 'Moyenne' | 'Basse';
export type Statut = 'Validation' | 'En cours' | 'A faire' | 'Terminé';

export interface Membre {
  initiales: string;
  couleur: string;
  nom?: string;
}

export interface Tache {
  id: number;
  nom: string;
  projet: string;
  equipe: Membre[];
  membreNom: string;
  membreId?: number;
  priorite: Priorite;
  statut: Statut;

  dateDebut?: string;
  criticite?: number;
  dureeEstimee?: number;
  echeance: string;
}

@Component({
  selector: 'app-taches',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './taches.html',
  styleUrls: ['./taches.css']
})
export class TachesComponent implements OnInit {
  private taskService = inject(TaskService);

  loading = signal(false);
  error = signal<string | null>(null);

  searchValue = '';
  filterPrioriteValue = 'Toutes';
  filterMembreValue = 'Tous';

  priorites: string[] = ['Toutes', 'Haute', 'Moyenne', 'Basse'];

  taches = signal<Tache[]>([]);

  ngOnInit(): void {
    this.loadTasks();
  }

  loadTasks(): void {
    this.loading.set(true);
    this.error.set(null);

    const query = this.searchValue?.trim() || undefined;
    const priority = this.filterPrioriteValue !== 'Toutes' ? this.filterPrioriteValue : undefined;

    this.taskService.getMyTasks(query, priority).subscribe({
      next: (tasks: TaskDto[]) => {
        const mappedTasks = tasks.map(task => this.mapTaskDtoToTache(task));

        const finalTasks = this.filterMembreValue === 'Tous'
          ? mappedTasks
          : mappedTasks.filter(t => t.membreNom === this.filterMembreValue);

        this.taches.set(finalTasks);
        this.loading.set(false);
      },
      error: (err) => {
        console.error('Erreur chargement tâches :', err);
        this.error.set('Erreur lors du chargement des tâches.');
        this.loading.set(false);
      }
    });
  }

  mapTaskDtoToTache(task: TaskDto): Tache {
    const membreNom = task.assignedToName?.trim() || 'Non assigné';

    return {
      id: task.id ?? 0,
      nom: task.title,
      projet: task.projectName || 'Sans projet',
      membreNom,
      membreId: task.assignedToId,
      equipe: [
        {
          initiales: this.getInitiales(membreNom),
          couleur: this.getAvatarColor(membreNom),
          nom: membreNom
        }
      ],
      priorite: this.mapPriority(task.priority),
      statut: this.mapStatus(task.status),
      dateDebut: task.startDate || '',
      criticite: task.criticite,
      dureeEstimee: task.dureeEstimee,
      echeance: task.estimatedEndDate || ''
    };
  }

  mapPriority(priority?: 'BASSE' | 'MOYENNE' | 'HAUTE'): Priorite {
    switch (priority) {
      case 'HAUTE':
        return 'Haute';
      case 'MOYENNE':
        return 'Moyenne';
      case 'BASSE':
        return 'Basse';
      default:
        return 'Moyenne';
    }
  }

  mapStatus(status: TaskStatus): Statut {
    switch ((status || '').toUpperCase()) {
      case 'VALIDATION':
        return 'Validation';
      case 'EN_COURS':
        return 'En cours';
      case 'A_FAIRE':
        return 'A faire';
      case 'TERMINE':
      case 'TERMINÉ':
        return 'Terminé';
      default:
        return 'A faire';
    }
  }

  getInitiales(fullName: string): string {
    if (!fullName || fullName === 'Non assigné') return 'NA';

    const parts = fullName
      .split(' ')
      .map(part => part.trim())
      .filter(Boolean);

    if (parts.length === 1) {
      return parts[0].substring(0, 2).toUpperCase();
    }

    return (parts[0][0] + parts[1][0]).toUpperCase();
  }

  getAvatarColor(value: string): string {
    const colors = [
      '#3b82f6',
      '#8b5cf6',
      '#10b981',
      '#f97316',
      '#ec4899',
      '#6366f1',
      '#14b8a6',
      '#f59e0b'
    ];

    let hash = 0;
    for (let i = 0; i < value.length; i++) {
      hash = value.charCodeAt(i) + ((hash << 5) - hash);
    }

    return colors[Math.abs(hash) % colors.length];
  }

  membres = computed(() => {
    const noms = this.taches()
      .map(t => t.membreNom)
      .filter(nom => !!nom && nom !== 'Non assigné');

    return ['Tous', ...Array.from(new Set(noms))];
  });

  tachesFiltrees = computed(() => this.taches());

  onSearchChange(): void {
    this.loadTasks();
  }

  onPrioriteChange(): void {
    this.loadTasks();
  }

  onMembreChange(): void {
    this.loadTasks();
  }

  get enAttente(): number {
    return this.taches().filter(t => t.statut === 'A faire').length;
  }

  get aValider(): number {
    return this.taches().filter(t => t.statut === 'Validation').length;
  }

  get completees(): number {
    return this.taches().filter(t => t.statut === 'Terminé').length;
  }

  valider(id: number): void {
    const commentaire = prompt('Commentaire de validation :') ?? '';

    this.taskService.validateTask(id, commentaire).subscribe({
      next: () => this.loadTasks(),
      error: (err) => console.error('Erreur validation tâche :', err)
    });
  }

  rejeter(id: number): void {
    const commentaire = prompt('Motif du rejet :');

    if (!commentaire || !commentaire.trim()) {
      return;
    }

    this.taskService.rejectTask(id, commentaire.trim()).subscribe({
      next: () => this.loadTasks(),
      error: (err) => console.error('Erreur rejet tâche :', err)
    });
  }

  nouvelleTache(): void {
    console.log('Nouvelle tâche');
  }

  formatDate(dateStr: string): string {
    if (!dateStr) return '-';

    const d = new Date(dateStr);
    if (isNaN(d.getTime())) return '-';

    return d.toLocaleDateString('fr-FR', {
      day: '2-digit',
      month: 'short',
      year: 'numeric'
    });
  }

  extraMembreCount(equipe: Membre[]): number {
    return Math.max(0, equipe.length - 2);
  }

  visibleMembres(equipe: Membre[]): Membre[] {
    return equipe.slice(0, 2);
  }

  getPrioriteClass(priorite: Priorite): string {
    switch (priorite) {
      case 'Haute':
        return 'priorite-haute';
      case 'Moyenne':
        return 'priorite-moyenne';
      case 'Basse':
        return 'priorite-basse';
      default:
        return '';
    }
  }

  getStatutClass(statut: Statut): string {
    switch (statut) {
      case 'Validation':
        return 'statut-validation';
      case 'En cours':
        return 'statut-en-cours';
      case 'A faire':
        return 'statut-a-faire';
      case 'Terminé':
        return 'statut-termine';
      default:
        return '';
    }
  }
}
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
  priorite: Priorite;
  echeance: string;
  statut: Statut;
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

  searchQuery = signal('');
  filterPriorite = signal<string>('Toutes');
  filterMembre = signal<string>('Tous');
  loading = signal(false);
  error = signal<string | null>(null);

  // valeurs liées aux select/input
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

    this.taskService.getMyTasks().subscribe({
      next: (tasks: TaskDto[]) => {
        const mappedTasks = tasks.map(task => this.mapTaskDtoToTache(task));
        this.taches.set(mappedTasks);
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
      equipe: [
        {
          initiales: this.getInitiales(membreNom),
          couleur: this.getAvatarColor(membreNom),
          nom: membreNom
        }
      ],
      priorite: this.mapPriority(task.priority),
      echeance: task.dueDate || '',
      statut: this.mapStatus(task.status)
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
    switch (status) {
      case 'Validation':
        return 'Validation';
      case 'En_cours':
        return 'En cours';
      case 'A_faire':
        return 'A faire';
      case 'Terminé':
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

  tachesFiltrees = computed(() => {
    const query = this.searchQuery().toLowerCase().trim();
    const priorite = this.filterPriorite();
    const membre = this.filterMembre();

    return this.taches().filter(t => {
      const matchSearch =
        !query ||
        t.nom.toLowerCase().includes(query) ||
        t.projet.toLowerCase().includes(query) ||
        t.membreNom.toLowerCase().includes(query);

      const matchPriorite =
        priorite === 'Toutes' || t.priorite === priorite;

      const matchMembre =
        membre === 'Tous' || t.membreNom === membre;

      return matchSearch && matchPriorite && matchMembre;
    });
  });

  onSearchChange(event: Event): void {
    const value = (event.target as HTMLInputElement).value;
    this.searchValue = value;
    this.searchQuery.set(value);
  }

  onPrioriteChange(event: Event): void {
    const value = (event.target as HTMLSelectElement).value;
    this.filterPrioriteValue = value;
    this.filterPriorite.set(value);
  }

  onMembreChange(event: Event): void {
    const value = (event.target as HTMLSelectElement).value;
    this.filterMembreValue = value;
    this.filterMembre.set(value);
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
    this.taskService.updateMyTaskStatus(id, 'Terminé').subscribe({
      next: () => this.loadTasks(),
      error: (err) => console.error('Erreur validation tâche :', err)
    });
  }

  rejeter(id: number): void {
    this.taskService.updateMyTaskStatus(id, 'A_faire').subscribe({
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
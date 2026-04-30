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
  showTaskModal = false;
  isEditMode = false;
  estimatedEndDate = '';
  teamMembers: any[] = [];

  taskForm = {
    id: null as number | null,
    title: '',
    description: '',
    startDate: '',
    criticite: 3,
    priority: 'MOYENNE' as 'BASSE' | 'MOYENNE' | 'HAUTE',
    assignedToId: null as number | null,
    estimatedEndDate: '',
    projectId: null as number | null
  };

  ngOnInit(): void {
    this.loadTasks();
    this.loadTeamMembers();
  }

  /** Charger les membres de l'équipe pour le dropdown "Assigner à" */
  loadTeamMembers(): void {
    // À adapter selon ton service
    this.taskService.getTeamMembers().subscribe({
      next: (members: any[]) => {
        this.teamMembers = members;
      },
      error: (err) => {
        console.error('Erreur chargement membres:', err);
        this.teamMembers = [];
      }
    });
  }

  /** Calculer la date de fin estimée = Date début + Criticité (jours) */
  calculateEstimatedEndDate(): void {
    if (this.taskForm.startDate && this.taskForm.criticite) {
      const startDate = new Date(this.taskForm.startDate);
      const criticite = this.taskForm.criticite;
      
      // Ajouter les jours
      const endDate = new Date(startDate);
      endDate.setDate(startDate.getDate() + criticite);
      
      // Formater en YYYY-MM-DD
      this.estimatedEndDate = endDate.toISOString().split('T')[0];
      this.taskForm.estimatedEndDate = this.estimatedEndDate;
    } else {
      this.estimatedEndDate = '';
      this.taskForm.estimatedEndDate = '';
    }
  }

  /** Ouvrir la modal pour créer une nouvelle tâche */
  nouvelleTache(): void {
    this.isEditMode = false;
    this.taskForm = {
      id: null,
      title: '',
      description: '',
      startDate: '',
      criticite: 3,
      priority: 'MOYENNE',
      assignedToId: null,
      estimatedEndDate: '',
      projectId: null
    };
    this.estimatedEndDate = '';
    this.showTaskModal = true;
  }

  /** Fermer la modal */
  closeTaskModal(): void {
    this.showTaskModal = false;
    this.isEditMode = false;
    // Réinitialiser les données
    this.taskForm = {
      id: null,
      title: '',
      description: '',
      startDate: '',
      criticite: 3,
      priority: 'MOYENNE',
      assignedToId: null,
      estimatedEndDate: '',
      projectId: null
    };
    this.estimatedEndDate = '';
  }

  /** Sauvegarder la tâche (créer ou modifier) */
  saveTask(): void {
    if (!this.taskForm.title || !this.taskForm.assignedToId || !this.taskForm.criticite) {
      alert('Veuillez remplir tous les champs obligatoires.');
      return;
    }

    // Calculer la date de fin si nécessaire
    if (this.taskForm.startDate && this.taskForm.criticite && !this.taskForm.estimatedEndDate) {
      this.calculateEstimatedEndDate();
      this.taskForm.estimatedEndDate = this.estimatedEndDate;
    }

    const taskToSave: TaskDto = {
      title: this.taskForm.title,
      description: this.taskForm.description,
      startDate: this.taskForm.startDate,
      criticite: this.taskForm.criticite,
      priority: this.taskForm.priority as 'BASSE' | 'MOYENNE' | 'HAUTE',
      assignedToId: this.taskForm.assignedToId!,
      estimatedEndDate: this.taskForm.estimatedEndDate,
      status: 'A_faire',
      projectId: this.taskForm.projectId || 1
    };

    if (this.isEditMode && this.taskForm.id) {
      this.taskService.updateTask(this.taskForm.id, taskToSave).subscribe({
        next: () => {
          this.loadTasks();
          this.closeTaskModal();
          alert('✅ Tâche modifiée avec succès !');
        },
        error: (err) => {
          console.error('Erreur modification:', err);
          alert('Erreur lors de la modification');
        }
      });
    } else {
      this.taskService.createTask(taskToSave).subscribe({
        next: () => {
          this.loadTasks();
          this.closeTaskModal();
          alert('✅ Tâche créée avec succès !');
        },
        error: (err) => {
          console.error('Erreur création:', err);
          alert('Erreur lors de la création');
        }
      });
    }
  }

  /** Charger les tâches du backend */
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

  /** Mapper TaskDto vers Tache pour l'affichage */
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

  /** Mapper priorité du backend vers l'interface */
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

  /** Mapper statut du backend vers l'interface */
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

  /** Extraire les initiales d'un nom */
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

  /** Générer une couleur consistent pour un nom */
  getAvatarColor(value: string): string {
    const colors = [
      '#3b82f6', '#8b5cf6', '#10b981', '#f97316',
      '#ec4899', '#6366f1', '#14b8a6', '#f59e0b'
    ];

    let hash = 0;
    for (let i = 0; i < value.length; i++) {
      hash = value.charCodeAt(i) + ((hash << 5) - hash);
    }

    return colors[Math.abs(hash) % colors.length];
  }

  /** Récupérer les noms de membres uniques */
  membres = computed(() => {
    const noms = this.taches()
      .map(t => t.membreNom)
      .filter(nom => !!nom && nom !== 'Non assigné');

    return ['Tous', ...Array.from(new Set(noms))];
  });

  /** Les tâches filtrées (actuellement toutes) */
  tachesFiltrees = computed(() => this.taches());

  /** Événement de changement de recherche */
  onSearchChange(): void {
    this.loadTasks();
  }

  /** Événement de changement de filtre priorité */
  onPrioriteChange(): void {
    this.loadTasks();
  }

  /** Événement de changement de filtre membre */
  onMembreChange(): void {
    this.loadTasks();
  }

  /** Compter les tâches en attente */
  get enAttente(): number {
    return this.taches().filter(t => t.statut === 'A faire').length;
  }

  /** Compter les tâches à valider */
  get aValider(): number {
    return this.taches().filter(t => t.statut === 'Validation').length;
  }

  /** Compter les tâches complétées */
  get completees(): number {
    return this.taches().filter(t => t.statut === 'Terminé').length;
  }

  /** Valider une tâche */
  valider(id: number): void {
    const commentaire = prompt('Commentaire de validation :') ?? '';

    this.taskService.validateTask(id, commentaire).subscribe({
      next: () => {
        this.loadTasks();
        alert('✅ Tâche validée !');
      },
      error: (err) => {
        console.error('Erreur validation tâche :', err);
        alert('Erreur lors de la validation');
      }
    });
  }

  /** Rejeter une tâche */
  rejeter(id: number): void {
    const commentaire = prompt('Motif du rejet :');

    if (!commentaire || !commentaire.trim()) {
      return;
    }

    this.taskService.rejectTask(id, commentaire.trim()).subscribe({
      next: () => {
        this.loadTasks();
        alert('✅ Tâche rejetée !');
      },
      error: (err) => {
        console.error('Erreur rejet tâche :', err);
        alert('Erreur lors du rejet');
      }
    });
  }

  /** Formater une date en français */
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

  /** Compter les membres supplémentaires */
  extraMembreCount(equipe: Membre[]): number {
    return Math.max(0, equipe.length - 2);
  }

  /** Récupérer les membres visibles (max 2) */
  visibleMembres(equipe: Membre[]): Membre[] {
    return equipe.slice(0, 2);
  }

  /** Classe CSS pour priorité */
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

  /** Classe CSS pour statut */
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
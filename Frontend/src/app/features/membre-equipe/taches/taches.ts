import { Component, signal, computed, inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import {
  CdkDragDrop,
  DragDropModule,
  moveItemInArray
} from '@angular/cdk/drag-drop';
import { TaskService, TaskDto, TaskStatus } from '../../../core/services/task.service';
import { Router } from '@angular/router';
type PrioriteUi = 'HAUTE' | 'MOYENNE' | 'BASSE';
type StatutColonne = 'A_FAIRE' | 'EN_COURS' | 'EN_VALIDATION' | 'TERMINE';

interface TaskItem {
  id: number;
  titre: string;
  projet: string;
  date: string;
  priorite: PrioriteUi;
  statut: StatutColonne;
  commentaire?: string;
  reference?: string;
  membreNom?: string;
  rejected?: boolean;
}

@Component({
  selector: 'app-taches',
  standalone: true,
  imports: [CommonModule, FormsModule, DragDropModule],
  templateUrl: './taches.html',
  styleUrls: ['./taches.css']
})
export class MembreEquipeTaches implements OnInit {
  private taskService = inject(TaskService);
  private router = inject(Router);

  searchQuery = signal('');
  filterPriorite = signal<string>('Toutes');
  filterMembre = signal<string>('Tous');
  loading = signal(false);
  error = signal<string | null>(null);

  searchValue = '';
  filterPrioriteValue = 'Toutes';
  filterMembreValue = 'Tous';

  priorites: string[] = ['Toutes', 'Haute', 'Moyenne', 'Basse'];

  aFaire = signal<TaskItem[]>([]);
  enCours = signal<TaskItem[]>([]);
  enValidation = signal<TaskItem[]>([]);
  termine = signal<TaskItem[]>([]);

  connectedLists: string[] = [
    'todoList',
    'doingList',
    'validationList',
    'doneList'
  ];

  showCommentModal = signal(false);
  selectedComment = signal('');
  selectedTaskTitle = signal('');
goToDashboard() {
  this.router.navigate(['/membre-equipe/dashboard']);
}
  ngOnInit(): void {
    this.loadTasks();
  }

  loadTasks(): void {
    this.loading.set(true);
    this.error.set(null);

    this.taskService.getMyTasks().subscribe({
      next: (tasks: TaskDto[]) => {
        const mappedTasks = tasks.map(task => this.mapTask(task));

        this.aFaire.set(mappedTasks.filter(t => t.statut === 'A_FAIRE'));
        this.enCours.set(mappedTasks.filter(t => t.statut === 'EN_COURS'));
        this.enValidation.set(mappedTasks.filter(t => t.statut === 'EN_VALIDATION'));
        this.termine.set(mappedTasks.filter(t => t.statut === 'TERMINE'));

        this.loading.set(false);
      },
      error: (err) => {
        console.error('Erreur chargement tâches :', err);
        this.error.set('Erreur lors du chargement des tâches.');
        this.loading.set(false);
      }
    });
  }

private mapTask(task: TaskDto): TaskItem {
  const membreNom = task.assignedToName?.trim() || 'Non assigné';
  return {
    id: task.id ?? 0,
    titre: task.title,
    projet: task.projectName || 'Sans projet',
    date: this.formatDate(task.estimatedEndDate || task.createdAt || ''),
    priorite: this.mapPriority(task.priority),
    statut: this.mapStatusToColumn(task.status),
    commentaire: task.rejectionComment || '',   // ✅ typed now
    reference: task.id ? `#${task.id}` : '',
    membreNom,
    rejected: !!task.rejected                   // ✅ typed now
  };
}
  private mapPriority(priority?: 'BASSE' | 'MOYENNE' | 'HAUTE'): PrioriteUi {
    switch (priority) {
      case 'HAUTE':
        return 'HAUTE';
      case 'MOYENNE':
        return 'MOYENNE';
      case 'BASSE':
        return 'BASSE';
      default:
        return 'MOYENNE';
    }
  }

  private mapStatusToColumn(status: TaskStatus | any): StatutColonne {
    const s = String(status).toLowerCase().trim();

    if (s === 'a_faire' || s === 'a faire') {
      return 'A_FAIRE';
    }

    if (
      s === 'en_cours' ||
      s === 'en cours' ||
      s === 'refuse' ||
      s === 'refusé' ||
      s === 'rejeté' ||
      s === 'rejetee'
    ) {
      return 'EN_COURS';
    }

    if (s === 'validation') {
      return 'EN_VALIDATION';
    }

    if (s === 'terminé' || s === 'termine') {
      return 'TERMINE';
    }

    return 'EN_COURS';
  }

  private mapColumnToStatus(status: StatutColonne): TaskStatus {
    switch (status) {
      case 'A_FAIRE':
        return 'A_faire';
      case 'EN_COURS':
        return 'En_cours';
      case 'EN_VALIDATION':
        return 'Validation';
      case 'TERMINE':
        return 'Terminé';
      default:
        return 'A_faire';
    }
  }

  getPriorityLabel(priorite: PrioriteUi): string {
    switch (priorite) {
      case 'HAUTE':
        return 'HAUTE';
      case 'MOYENNE':
        return 'MOYENNE';
      case 'BASSE':
        return 'BASSE';
      default:
        return 'MOYENNE';
    }
  }

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

  membres = computed(() => {
    const all = [
      ...this.aFaire(),
      ...this.enCours(),
      ...this.enValidation(),
      ...this.termine()
    ];

    const noms = all
      .map(t => t.membreNom || 'Non assigné')
      .filter(nom => !!nom && nom !== 'Non assigné');

    return ['Tous', ...Array.from(new Set(noms))];
  });

  filteredAfaire = computed(() => this.applyFilters(this.aFaire()));
  filteredEnCours = computed(() => this.applyFilters(this.enCours()));
  filteredEnValidation = computed(() => this.applyFilters(this.enValidation()));
  filteredTermine = computed(() => this.applyFilters(this.termine()));

  private applyFilters(tasks: TaskItem[]): TaskItem[] {
    const query = this.searchQuery().toLowerCase().trim();
    const priorite = this.filterPriorite();
    const membre = this.filterMembre();

    return tasks.filter(t => {
      const matchSearch =
        !query ||
        t.titre.toLowerCase().includes(query) ||
        t.projet.toLowerCase().includes(query) ||
        (t.membreNom || '').toLowerCase().includes(query);

      const matchPriorite =
        priorite === 'Toutes' ||
        this.priorityUiToDisplay(t.priorite) === priorite;

      const matchMembre =
        membre === 'Tous' || t.membreNom === membre;

      return matchSearch && matchPriorite && matchMembre;
    });
  }

  private priorityUiToDisplay(priority: PrioriteUi): string {
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

  demarrerTache(task: TaskItem): void {
    if (!task.id || task.statut !== 'A_FAIRE') return;
    this.updateTaskStatus(task, 'EN_COURS');
  }

  soumettrePourValidation(task: TaskItem): void {
    if (!task.id || task.statut !== 'EN_COURS') return;
    this.updateTaskStatus(task, 'EN_VALIDATION');
  }

  valider(id: number): void {
    this.taskService.updateMyTaskStatus(id, 'Terminé').subscribe({
      next: () => this.loadTasks(),
      error: (err) => console.error('Erreur validation tâche :', err)
    });
  }

  rejeter(id: number): void {
    this.taskService.updateMyTaskStatus(id, 'En_cours').subscribe({
      next: () => this.loadTasks(),
      error: (err) => console.error('Erreur rejet tâche :', err)
    });
  }

  updateTaskStatus(task: TaskItem, newStatus: StatutColonne): void {
    const previousState = this.snapshotState();

    this.removeTaskFromAllColumns(task.id);
    this.addTaskToColumn({ ...task, statut: newStatus }, newStatus);
    this.refreshSignals();

    this.taskService.updateMyTaskStatus(task.id, this.mapColumnToStatus(newStatus)).subscribe({
      next: () => this.loadTasks(),
      error: (err) => {
        console.error('Erreur mise à jour statut tâche :', err);
        this.restoreState(previousState);
        this.error.set('Impossible de mettre à jour le statut.');
      }
    });
  }

  drop(event: CdkDragDrop<TaskItem[]>, targetStatus: StatutColonne): void {
    if (event.previousContainer === event.container) {
      moveItemInArray(event.container.data, event.previousIndex, event.currentIndex);
      return;
    }

    const task = event.previousContainer.data[event.previousIndex];
    if (!task) return;

    const currentStatus = task.statut;

    const isAllowed =
      (currentStatus === 'A_FAIRE' && targetStatus === 'EN_COURS') ||
      (currentStatus === 'EN_COURS' && targetStatus === 'A_FAIRE') ||
      (currentStatus === 'EN_COURS' && targetStatus === 'EN_VALIDATION');

    if (!isAllowed) {
      return;
    }

    const previousState = this.snapshotState();

    this.removeTaskFromAllColumns(task.id);
    this.addTaskToColumn({ ...task, statut: targetStatus }, targetStatus);
    this.refreshSignals();

    this.taskService.updateMyTaskStatus(task.id, this.mapColumnToStatus(targetStatus)).subscribe({
      next: () => this.loadTasks(),
      error: (err) => {
        console.error('Erreur changement statut tâche :', err);
        this.restoreState(previousState);
        this.error.set('Impossible de mettre à jour le statut.');
      }
    });
  }

  openComment(task: TaskItem): void {
    this.selectedTaskTitle.set(task.titre || 'Tâche');
    this.selectedComment.set(
      task.commentaire?.trim()
        ? task.commentaire
        : 'Aucun commentaire de rejet disponible.'
    );
    this.showCommentModal.set(true);
  }

  closeCommentModal(): void {
    this.showCommentModal.set(false);
    this.selectedComment.set('');
    this.selectedTaskTitle.set('');
  }

  private removeTaskFromAllColumns(taskId: number): void {
    this.aFaire.set(this.aFaire().filter(t => t.id !== taskId));
    this.enCours.set(this.enCours().filter(t => t.id !== taskId));
    this.enValidation.set(this.enValidation().filter(t => t.id !== taskId));
    this.termine.set(this.termine().filter(t => t.id !== taskId));
  }

  private addTaskToColumn(task: TaskItem, status: StatutColonne): void {
    switch (status) {
      case 'A_FAIRE':
        this.aFaire.set([...this.aFaire(), task]);
        break;
      case 'EN_COURS':
        this.enCours.set([...this.enCours(), task]);
        break;
      case 'EN_VALIDATION':
        this.enValidation.set([...this.enValidation(), task]);
        break;
      case 'TERMINE':
        this.termine.set([...this.termine(), task]);
        break;
    }
  }

  private refreshSignals(): void {
    this.aFaire.set([...this.aFaire()]);
    this.enCours.set([...this.enCours()]);
    this.enValidation.set([...this.enValidation()]);
    this.termine.set([...this.termine()]);
  }

  private snapshotState() {
    return {
      aFaire: [...this.aFaire()],
      enCours: [...this.enCours()],
      enValidation: [...this.enValidation()],
      termine: [...this.termine()]
    };
  }

  private restoreState(state: {
    aFaire: TaskItem[];
    enCours: TaskItem[];
    enValidation: TaskItem[];
    termine: TaskItem[];
  }): void {
    this.aFaire.set(state.aFaire);
    this.enCours.set(state.enCours);
    this.enValidation.set(state.enValidation);
    this.termine.set(state.termine);
  }

  formatDate(dateStr: string): string {
    if (!dateStr) return 'Sans échéance';

    const d = new Date(dateStr);
    if (isNaN(d.getTime())) return 'Sans échéance';

    return d.toLocaleDateString('fr-FR', {
      day: '2-digit',
      month: 'short',
      year: 'numeric'
    });
  }

  nouvelleTache(): void {
    console.log('Nouvelle tâche');
  }

  getPrioriteClass(priorite: string): string {
    switch (priorite) {
      case 'HAUTE':
        return 'priorite-haute';
      case 'MOYENNE':
        return 'priorite-moyenne';
      case 'BASSE':
        return 'priorite-basse';
      default:
        return '';
    }
  }

  getStatutClass(statut: string): string {
    switch (statut) {
      case 'Validation':
      case 'EN_VALIDATION':
        return 'statut-validation';
      case 'En cours':
      case 'EN_COURS':
        return 'statut-en-cours';
      case 'A faire':
      case 'A_FAIRE':
        return 'statut-a-faire';
      case 'Terminé':
      case 'TERMINE':
        return 'statut-termine';
      default:
        return '';
    }
  }
}
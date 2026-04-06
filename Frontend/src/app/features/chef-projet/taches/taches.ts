import { Component, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';

export type Priorite = 'Haute' | 'Moyenne' | 'Basse';
export type Statut = 'Validation' | 'En cours' | 'A faire' | 'Terminé';

export interface Membre {
  initiales: string;
  couleur: string;
}

export interface Tache {
  id: number;
  nom: string;
  projet: string;
  equipe: Membre[];
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
export class TachesComponent {
  searchQuery = signal('');
  filterPriorite = signal<string>('Toutes');
  filterMembre = signal<string>('Tous');

  taches = signal<Tache[]>([
    {
      id: 1,
      nom: 'Optimisation SEO Landing Page',
      projet: 'Projet Marketing 2024',
      equipe: [
        { initiales: 'AL', couleur: '#f97316' },
        { initiales: 'BK', couleur: '#8b5cf6' },
        { initiales: 'CM', couleur: '#10b981' }
      ],
      priorite: 'Haute',
      echeance: '2026-10-12',
      statut: 'Validation'
    },
    {
      id: 2,
      nom: 'Développement API Authentification',
      projet: 'Refonte Infrastructure',
      equipe: [
        { initiales: 'DL', couleur: '#3b82f6' }
      ],
      priorite: 'Moyenne',
      echeance: '2026-10-15',
      statut: 'En cours'
    },
    {
      id: 3,
      nom: 'Rédaction Documentation Utilisateur',
      projet: 'Support Client',
      equipe: [
        { initiales: 'EF', couleur: '#ec4899' },
        { initiales: 'GH', couleur: '#f59e0b' }
      ],
      priorite: 'Basse',
      echeance: '2026-10-20',
      statut: 'A faire'
    },
    {
      id: 4,
      nom: 'Audit Sécurité T3',
      projet: 'Compliance',
      equipe: [
        { initiales: 'IJ', couleur: '#6366f1' }
      ],
      priorite: 'Moyenne',
      echeance: '2026-10-05',
      statut: 'Terminé'
    }
  ]);

  priorites: string[] = ['Toutes', 'Haute', 'Moyenne', 'Basse'];

  tachesFiltrees = computed(() => {
    return this.taches().filter(t => {
      const query = this.searchQuery().toLowerCase().trim();

      const matchSearch =
        t.nom.toLowerCase().includes(query) ||
        t.projet.toLowerCase().includes(query);

      const matchPriorite =
        this.filterPriorite() === 'Toutes' ||
        t.priorite === this.filterPriorite();

      return matchSearch && matchPriorite;
    });
  });

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
    this.taches.update(list =>
      list.map(t => t.id === id ? { ...t, statut: 'Terminé' as Statut } : t)
    );
  }

  rejeter(id: number): void {
    this.taches.update(list =>
      list.map(t => t.id === id ? { ...t, statut: 'A faire' as Statut } : t)
    );
  }

  nouvelleTache(): void {
    console.log('Nouvelle tâche');
  }

  formatDate(dateStr: string): string {
    const d = new Date(dateStr);
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
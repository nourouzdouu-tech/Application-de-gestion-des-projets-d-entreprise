import { Component, OnInit, inject, signal, WritableSignal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { AdminService, ClientResponse, PageResponse, Representant } from '../../../core/services/admin.service';
import { AuthService } from '../../../core/services/auth.service';

@Component({
  selector: 'app-clients',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './clients.html',
  styleUrls: ['./clients.css']
})
export class Clients implements OnInit {
  private adminService = inject(AdminService);
  private router = inject(Router);
  authService = inject(AuthService);

  // ── Data ──────────────────────────────────────────
  allClients: ClientResponse[] = [];
  filteredClients: ClientResponse[] = [];
  currentPage = 0;
  pageSize = 10;
  totalPages = 0;
  totalElements = 0;
  searchTerm = '';
  loading: WritableSignal<boolean> = signal(false);
  isSearching = false;

  // Cache pour la recherche (optimisation)
  private searchCache = new Map<string, ClientResponse[]>();

  // ── Modal Créer / Modifier ────────────────────────
  showModal = false;
  isEditing = false;
  editingId: number | null = null;

  formData: { nom: string; representants: Representant[] } = {
    nom: '',
    representants: []
  };
  errorMessage = '';

  // ── Modal Voir Représentants ──────────────────────
  showRepresentantsModal = false;
  selectedClient: ClientResponse | null = null;

  // ── Modal Confirmation Suppression ────────────────
  showConfirmModal = false;
  pendingDeleteId: number | null = null;

  // ── Toast ─────────────────────────────────────────
  showToast = false;
  toastMessage = '';
  toastType: 'success' | 'error' = 'success';

  // ── Signals ───────────────────────────────────────
  activeNavItem = signal<string>('clients');
  currentUser = signal<any>(null);

  private searchDebounce: ReturnType<typeof setTimeout> | null = null;

  // ── Lifecycle ─────────────────────────────────────
  ngOnInit(): void {
    this.loadCurrentUser();
    this.loadAllClients();
  }

  loadCurrentUser(): void {
    this.currentUser.set(this.authService.getUser());
  }

  // ══════════════════════════════════════════════════
  // CHARGEMENT DE TOUS LES CLIENTS (une seule fois)
  // ══════════════════════════════════════════════════

  loadAllClients(): void {
    this.loading.set(true);
    this.adminService.getClients(0, 1000, '').subscribe({
      next: (response: PageResponse<ClientResponse>) => {
        this.allClients = response.content;
        // Pré-calculer les données de recherche pour chaque client
        this.precomputeSearchData();
        this.applySearchFilter();
        this.loading.set(false);
      },
      error: (err) => {
        console.error('Erreur chargement clients:', err);
        this.triggerToast('Erreur lors du chargement des clients', 'error');
        this.loading.set(false);
      }
    });
  }

  // ══════════════════════════════════════════════════
  // PRÉ-CALCUL DES DONNÉES DE RECHERCHE (optimisation)
  // ══════════════════════════════════════════════════

  private precomputeSearchData(): void {
    // Ajouter une propriété cachée pour la recherche rapide
    this.allClients.forEach(client => {
      (client as any)._searchText = this.buildSearchText(client);
    });
  }

  private buildSearchText(client: ClientResponse): string {
    const parts = [client.nom.toLowerCase()];
    
    if (client.representants) {
      client.representants.forEach(rep => {
        if (rep.nom) parts.push(rep.nom.toLowerCase());
        if (rep.email) parts.push(rep.email.toLowerCase());
        if (rep.telephone) parts.push(rep.telephone);
      });
    }
    
    return parts.join(' ');
  }

  // ══════════════════════════════════════════════════
  // RECHERCHE OPTIMISÉE (avec cache)
  // ══════════════════════════════════════════════════

  applySearchFilter(): void {
    const term = this.searchTerm.trim().toLowerCase();
    
    // Vérifier le cache
    if (this.searchCache.has(term)) {
      this.filteredClients = [...(this.searchCache.get(term) || [])];
      this.updatePagination();
      return;
    }
    
    // Recherche optimisée
    let results: ClientResponse[];
    
    if (!term) {
      results = [...this.allClients];
    } else {
      // Utiliser le texte pré-calculé pour une recherche plus rapide
      results = this.allClients.filter(client => 
        (client as any)._searchText.includes(term)
      );
    }
    
    // Mettre en cache le résultat
    this.searchCache.set(term, results);
    
    this.filteredClients = results;
    this.updatePagination();
  }

  private updatePagination(): void {
    this.totalElements = this.filteredClients.length;
    this.totalPages = Math.ceil(this.totalElements / this.pageSize);
    this.currentPage = 0;
  }

  onSearch(value: string): void {
    this.searchTerm = value;
    this.isSearching = true;
    
    if (this.searchDebounce) clearTimeout(this.searchDebounce);
    this.searchDebounce = setTimeout(() => {
      this.applySearchFilter();
      this.isSearching = false;
    }, 200); // Réduit le délai à 200ms
  }

  // Recherche immédiate sans debounce (optionnel)
  onSearchImmediate(value: string): void {
    this.searchTerm = value;
    this.applySearchFilter();
  }

  // ══════════════════════════════════════════════════
  // PAGINATION
  // ══════════════════════════════════════════════════

  get paginatedClients(): ClientResponse[] {
    const start = this.currentPage * this.pageSize;
    const end = start + this.pageSize;
    return this.filteredClients.slice(start, end);
  }

  changePage(page: number): void {
    if (page >= 0 && page < this.totalPages) {
      this.currentPage = page;
    }
  }

  getRangeStart(): number {
    if (this.filteredClients.length === 0) return 0;
    return this.currentPage * this.pageSize + 1;
  }

  getRangeEnd(): number {
    const end = (this.currentPage + 1) * this.pageSize;
    return Math.min(end, this.filteredClients.length);
  }

  getPages(): number[] {
    const total = this.totalPages;
    const current = this.currentPage + 1;
    const pages: number[] = [];

    if (total <= 5) {
      for (let i = 1; i <= total; i++) pages.push(i);
    } else if (current <= 3) {
      pages.push(1, 2, 3, 4, -1, total);
    } else if (current >= total - 2) {
      pages.push(1, -1, total - 3, total - 2, total - 1, total);
    } else {
      pages.push(1, -1, current - 1, current, current + 1, -1, total);
    }
    return pages;
  }

  // ══════════════════════════════════════════════════
  // REPRESENTANTS HELPERS
  // ══════════════════════════════════════════════════

  addRepresentant(): void {
    this.formData.representants = [
      ...this.formData.representants,
      { nom: '', email: '', telephone: '' }
    ];
  }

  removeRepresentant(index: number): void {
    this.formData.representants = this.formData.representants.filter((_, i) => i !== index);
  }

  updateRepresentant(index: number, field: keyof Representant, value: string): void {
    const updated = [...this.formData.representants];
    updated[index] = { ...updated[index], [field]: value };
    this.formData.representants = updated;
  }

  // ══════════════════════════════════════════════════
  // MODAL CRÉER / MODIFIER
  // ══════════════════════════════════════════════════

  openCreateModal(): void {
    this.isEditing = false;
    this.editingId = null;
    this.formData = {
      nom: '',
      representants: [{ nom: '', email: '', telephone: '' }]
    };
    this.errorMessage = '';
    this.showModal = true;
  }

  openEditModal(client: ClientResponse): void {
    this.isEditing = true;
    this.editingId = client.id;
    this.formData = {
      nom: client.nom,
      representants: client.representants?.length
        ? client.representants.map(r => ({ id: r.id, nom: r.nom, email: r.email, telephone: r.telephone }))
        : [{ nom: '', email: '', telephone: '' }]
    };
    this.errorMessage = '';
    this.showModal = true;
  }

  closeModal(): void {
    this.showModal = false;
    this.errorMessage = '';
  }

  saveClient(): void {
    if (!this.formData.nom?.trim()) {
      this.errorMessage = 'Veuillez saisir le nom du client';
      return;
    }

    const validRepresentants = this.formData.representants.filter(
      r => r.nom?.trim() && r.email?.trim() && r.telephone?.trim()
    );

    if (validRepresentants.length === 0) {
      this.errorMessage = 'Veuillez ajouter au moins un représentant avec nom, email et téléphone';
      return;
    }

    const payload = {
      nom: this.formData.nom.trim(),
      representants: validRepresentants.map(r => ({
        ...(r.id !== undefined ? { id: r.id } : {}),
        nom: r.nom.trim(),
        email: r.email.trim(),
        telephone: r.telephone.trim()
      }))
    };

    if (this.isEditing && this.editingId !== null) {
      this.adminService.updateClient(this.editingId, payload).subscribe({
        next: () => {
          this.closeModal();
          this.clearSearchCache(); // Invalider le cache
          this.loadAllClients();
          this.triggerToast('Client modifié avec succès', 'success');
        },
        error: (err) => {
          this.errorMessage = err.error?.message || 'Erreur lors de la modification';
        }
      });
    } else {
      this.adminService.createClient(payload).subscribe({
        next: () => {
          this.closeModal();
          this.clearSearchCache(); // Invalider le cache
          this.loadAllClients();
          this.triggerToast('Client créé avec succès', 'success');
        },
        error: (err) => {
          this.errorMessage = err.error?.message || 'Erreur lors de la création';
        }
      });
    }
  }

  // Invalider le cache de recherche
  private clearSearchCache(): void {
    this.searchCache.clear();
  }

  // ══════════════════════════════════════════════════
  // MODAL VOIR REPRÉSENTANTS
  // ══════════════════════════════════════════════════

  openRepresentantsModal(client: ClientResponse): void {
    this.selectedClient = client;
    this.showRepresentantsModal = true;
  }

  closeRepresentantsModal(): void {
    this.showRepresentantsModal = false;
    this.selectedClient = null;
  }

  // ══════════════════════════════════════════════════
  // SUPPRESSION AVEC CONFIRMATION
  // ══════════════════════════════════════════════════

  confirmDeleteClient(id: number): void {
    this.pendingDeleteId = id;
    this.showConfirmModal = true;
  }

  confirmYes(): void {
    if (this.pendingDeleteId !== null) {
      this.adminService.deleteClient(this.pendingDeleteId).subscribe({
        next: () => {
          this.clearSearchCache(); // Invalider le cache
          this.loadAllClients();
          this.triggerToast('Client supprimé avec succès', 'success');
        },
        error: () => {
          this.triggerToast('Erreur lors de la suppression', 'error');
        }
      });
    }
    this.showConfirmModal = false;
    this.pendingDeleteId = null;
  }

  confirmNo(): void {
    this.showConfirmModal = false;
    this.pendingDeleteId = null;
  }

  // ══════════════════════════════════════════════════
  // TOAST
  // ══════════════════════════════════════════════════

  triggerToast(message: string, type: 'success' | 'error'): void {
    this.toastMessage = message;
    this.toastType = type;
    this.showToast = true;
    setTimeout(() => { this.showToast = false; }, 3500);
  }

  // ══════════════════════════════════════════════════
  // NAVIGATION
  // ══════════════════════════════════════════════════

  goToDashboard(): void {
    this.router.navigate(['/admin/dashboard']);
  }

  setNav(item: string): void {
    this.activeNavItem.set(item);
    const routes: Record<string, string> = {
      dashboard: '/admin/dashboard',
      utilisateurs: '/admin/utilisateurs',
      roles: '/admin/roles',
      clients: '/admin/clients'
    };
    if (routes[item]) this.router.navigate([routes[item]]);
  }

  openProfile(): void {
    // TODO: implement profile panel
  }

  logout(): void {
    this.authService.removeUser();
    this.router.navigate(['/login']);
  }

  // ══════════════════════════════════════════════════
  // HELPERS
  // ══════════════════════════════════════════════════

  getInitials(nom: string): string {
    return nom ? nom.substring(0, 2).toUpperCase() : 'CL';
  }

  getAvatarColor(index: number): string {
    const colors = [
      '#7c3aed', '#3b82f6', '#10b981', '#f59e0b', '#ef4444',
      '#8b5cf6', '#06b6d4', '#84cc16', '#ec4899', '#14b8a6'
    ];
    return colors[index % colors.length];
  }

  getRepresentantsText(count: number): string {
    return count === 1 ? '1 représentant' : `${count} représentants`;
  }
}
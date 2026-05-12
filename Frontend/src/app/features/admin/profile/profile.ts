import { Component, OnInit, ChangeDetectorRef, NgZone } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { ProfileDto, ProfileService } from '../../../core/services/profile.service';

@Component({
  selector: 'app-profiles',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './profile.html',
  styleUrl: './profile.css'
})
export class Profiles implements OnInit {
  profiles: ProfileDto[] = [];
  filteredProfiles: ProfileDto[] = [];

  loading = false;
  error: string | null = null;

  searchTerm = '';
  showModal = false;
  isEditMode = false;
  editingProfileId: number | null = null;
  submitting = false;

  showDeleteModal = false;
  profileToDelete: ProfileDto | null = null;

  currentPage = 1;
  itemsPerPage = 4;

  form: ProfileDto = {
    libelle: '',
    tjm: 0
  };

  constructor(
    private profileService: ProfileService,
    private router: Router,
    private cdr: ChangeDetectorRef,
    private ngZone: NgZone
  ) {}

  ngOnInit(): void {
    this.fetchProfiles();
  }

  fetchProfiles(): void {
    this.loading = true;
    this.error = null;
    this.currentPage = 1;
    this.cdr.detectChanges();

    this.profileService.getAllProfiles().subscribe({
      next: (profiles) => {
        this.ngZone.run(() => {
          this.profiles = [...profiles];
          this.filteredProfiles = [...profiles];
          this.applyFilter();
          this.loading = false;
          this.error = null;
          this.cdr.detectChanges();
        });
      },
      error: () => {
        this.ngZone.run(() => {
          this.error = 'Erreur lors du chargement des profils.';
          this.loading = false;
          this.cdr.detectChanges();
        });
      }
    });
  }

  applyFilter(): void {
    const term = this.searchTerm.trim().toLowerCase();

    if (!term) {
      this.filteredProfiles = [...this.profiles];
      this.currentPage = 1;
      this.cdr.detectChanges();
      return;
    }

    this.filteredProfiles = this.profiles.filter(profile =>
      profile.libelle.toLowerCase().includes(term) ||
      profile.tjm.toString().includes(term)
    );

    this.currentPage = 1;
    this.cdr.detectChanges();
  }

  get paginatedProfiles(): ProfileDto[] {
    const start = (this.currentPage - 1) * this.itemsPerPage;
    return this.filteredProfiles.slice(start, start + this.itemsPerPage);
  }

  get totalPages(): number {
    return Math.ceil(this.filteredProfiles.length / this.itemsPerPage);
  }

  get pageNumbers(): number[] {
    return Array.from({ length: this.totalPages }, (_, i) => i + 1);
  }

  get startItem(): number {
    if (this.filteredProfiles.length === 0) return 0;
    return (this.currentPage - 1) * this.itemsPerPage + 1;
  }

  get endItem(): number {
    return Math.min(this.currentPage * this.itemsPerPage, this.filteredProfiles.length);
  }

  goToPage(page: number): void {
    if (page < 1 || page > this.totalPages) return;
    this.currentPage = page;
    this.cdr.detectChanges();
  }

  openCreateModal(): void {
    this.isEditMode = false;
    this.editingProfileId = null;
    this.form = {
      libelle: '',
      tjm: 0
    };
    this.submitting = false;
    this.showModal = true;
  }

  openEditModal(profile: ProfileDto): void {
    this.isEditMode = true;
    this.editingProfileId = profile.id ?? null;
    this.form = {
      libelle: profile.libelle,
      tjm: profile.tjm
    };
    this.submitting = false;
    this.showModal = true;
  }

  closeModal(): void {
    this.showModal = false;
    this.submitting = false;
    this.isEditMode = false;
    this.editingProfileId = null;
    this.form = {
      libelle: '',
      tjm: 0
    };
    this.cdr.detectChanges();
  }

  saveProfile(): void {
    if (!this.form.libelle.trim()) {
      alert('Le libellé est obligatoire.');
      return;
    }

    if (!this.form.tjm || this.form.tjm <= 0) {
      alert('Le TJM doit être supérieur à 0.');
      return;
    }

    this.submitting = true;
    this.cdr.detectChanges();

    const payload: ProfileDto = {
      libelle: this.form.libelle.trim(),
      tjm: Number(this.form.tjm)
    };

    const request$ =
      this.isEditMode && this.editingProfileId
        ? this.profileService.updateProfile(this.editingProfileId, payload)
        : this.profileService.createProfile(payload);

    request$.subscribe({
      next: () => {
        this.ngZone.run(() => {
          this.closeModal();
          this.fetchProfiles();
          this.cdr.detectChanges();
        });
      },
      error: (err) => {
        this.ngZone.run(() => {
          this.submitting = false;
          this.cdr.detectChanges();
          alert(
            err?.error?.message ||
            (this.isEditMode
              ? 'Erreur lors de la mise à jour du profil.'
              : 'Erreur lors de la création du profil.')
          );
        });
      }
    });
  }

  confirmDelete(profile: ProfileDto): void {
    this.profileToDelete = profile;
    this.showDeleteModal = true;
  }

  cancelDelete(): void {
    this.showDeleteModal = false;
    this.profileToDelete = null;
  }

  deleteProfile(): void {
    if (!this.profileToDelete?.id) return;

    this.profileService.setDeletedStatus(this.profileToDelete.id, true).subscribe({
      next: () => {
        this.ngZone.run(() => {
          this.showDeleteModal = false;
          this.profileToDelete = null;
          this.fetchProfiles();
          this.cdr.detectChanges();
        });
      },
      error: (err) => {
        this.ngZone.run(() => {
          this.showDeleteModal = false;
          this.profileToDelete = null;
          alert(err?.error?.message || 'Erreur lors de la suppression du profil.');
        });
      }
    });
  }

  trackByProfile(index: number, profile: ProfileDto): number | undefined {
    return profile.id;
  }

  goToDashboard(): void {
    this.router.navigate(['/admin/dashboard']);
  }

  getInitials(value: string): string {
    if (!value) return 'P';
    return value
      .split(' ')
      .map(word => word.charAt(0))
      .join('')
      .substring(0, 2)
      .toUpperCase();
  }

  getAvatarColor(index: number): string {
    const colors = ['#7c3aed', '#2563eb', '#0ea5e9', '#10b981', '#f59e0b', '#ef4444'];
    return colors[index % colors.length];
  }
}
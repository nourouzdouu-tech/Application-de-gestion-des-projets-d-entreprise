import { Component, OnInit, ChangeDetectorRef,NgZone } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { finalize } from 'rxjs/operators';
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
    this.cdr.detectChanges();
    return;
  }

  this.filteredProfiles = this.profiles.filter(profile =>
    profile.libelle.toLowerCase().includes(term) ||
    profile.tjm.toString().includes(term)
  );

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
 deleteProfile(profile: ProfileDto): void {
  if (!profile.id) return;

  const confirmed = confirm(`Supprimer le profil "${profile.libelle}" ?`);
  if (!confirmed) return;

  this.profileService.setDeletedStatus(profile.id, true).subscribe({
    next: () => {
      this.ngZone.run(() => {
        this.fetchProfiles();
        this.cdr.detectChanges();
      });
    },
    error: (err) => {
      this.ngZone.run(() => {
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
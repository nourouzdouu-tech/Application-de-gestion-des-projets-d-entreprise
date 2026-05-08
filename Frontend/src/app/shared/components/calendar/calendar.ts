import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AuthService } from '../../../core/services/auth.service';
import { UserService, User } from '../../../core/services/user.service';
import { ProjectService, ProjectDto } from '../../../core/services/project.service';
import { HttpClient } from '@angular/common/http';


interface CalendarEvent {
  id: number;
  title: string;
  date: Date;
  startTime?: string;
  endTime?: string;
  description?: string;

  projectId?: number | null;
  projectName?: string;

  ownerId: number;
  ownerName: string;
  invitedUserIds: number[];
  invitedUserNames: string[];
}

@Component({
  selector: 'app-shared-calendar',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './calendar.html',
  styleUrl: './calendar.css'
})
export class SharedCalendarComponent implements OnInit {
  currentMonth = signal<Date>(new Date());
  selectedDate = signal<Date | null>(null);

  allEvents: CalendarEvent[] = [];
  visibleEvents: CalendarEvent[] = [];

  usersList: User[] = [];
  filteredUsers: User[] = [];
  inviteSearch = '';

  myProjects: ProjectDto[] = [];

  currentUserId = 0;
  currentUserName = '';

  showEventModal = false;
  isEditing = false;
  editingEventId: number | null = null;

  eventForm = {
    title: '',
    description: '',
    date: '',
    startTime: '',
    endTime: '',
    projectId: null as number | null,
    invitedUserIds: [] as number[]
  };

  showToast = false;
  toastMessage = '';
  toastType: 'success' | 'error' = 'success';

  constructor(
    private authService: AuthService,
    private userService: UserService,
    private projectService: ProjectService,
    private http: HttpClient
  ) {}

  ngOnInit(): void {
    const user = this.authService.getUser();
    this.currentUserId = user?.id ?? 0;
    this.currentUserName = `${user?.prenom ?? ''} ${user?.nom ?? ''}`.trim();

    this.loadUsers();
    this.loadMyProjects();
    this.loadEvents();
  }

  private getStorageKey(): string {
    return 'shared_calendar_events';
  }

  loadEvents(): void {
    const raw = localStorage.getItem(this.getStorageKey());

    if (raw) {
      this.allEvents = JSON.parse(raw).map((e: any) => ({
        ...e,
        date: new Date(e.date),
        startTime: e.startTime || e.time || '',
        endTime: e.endTime || e.time || '',
        invitedUserIds: Array.isArray(e.invitedUserIds) ? e.invitedUserIds : [],
        invitedUserNames: Array.isArray(e.invitedUserNames) ? e.invitedUserNames : []
      }));
    } else {
      this.allEvents = [];
      this.saveAllEvents();
    }

    this.refreshVisibleEvents();
  }

  saveAllEvents(): void {
    localStorage.setItem(this.getStorageKey(), JSON.stringify(this.allEvents));
  }

  refreshVisibleEvents(): void {
    this.visibleEvents = this.allEvents.filter(event => this.canSeeEvent(event));
  }

  canSeeEvent(event: CalendarEvent): boolean {
    return event.ownerId === this.currentUserId || event.invitedUserIds.includes(this.currentUserId);
  }

  loadUsers(): void {
    this.userService.getAllUsers().subscribe({
      next: (users) => {
        this.usersList = users.filter(u => u.id !== this.currentUserId);
        this.filteredUsers = [...this.usersList];
      },
      error: () => {
        this.usersList = [];
        this.filteredUsers = [];
      }
    });
  }

  loadMyProjects(): void {
    this.projectService.getMyAssignedProjects().subscribe({
      next: (projects) => {
        this.myProjects = projects ?? [];
      },
      error: () => {
        this.projectService.getMyProjects().subscribe({
          next: (projects) => {
            this.myProjects = projects ?? [];
          },
          error: () => {
            this.myProjects = [];
          }
        });
      }
    });
  }

  onInviteSearch(): void {
    const query = this.inviteSearch.trim().toLowerCase();

    if (!query) {
      this.filteredUsers = [...this.usersList];
      return;
    }

    this.filteredUsers = this.usersList.filter(user => {
      const fullName = `${user.prenom} ${user.nom}`.toLowerCase();
      const email = (user.email || '').toLowerCase();
      return fullName.includes(query) || email.includes(query);
    });
  }

  clearInviteSearch(): void {
    this.inviteSearch = '';
    this.filteredUsers = [...this.usersList];
  }

  toggleInvitedUser(userId: number): void {
    if (this.eventForm.invitedUserIds.includes(userId)) {
      this.eventForm.invitedUserIds = this.eventForm.invitedUserIds.filter(id => id !== userId);
    } else {
      this.eventForm.invitedUserIds = [...this.eventForm.invitedUserIds, userId];
    }
  }

  isUserSelected(userId: number): boolean {
    return this.eventForm.invitedUserIds.includes(userId);
  }

  getSelectedUsersLabel(): string {
    if (this.eventForm.invitedUserIds.length === 0) return 'Aucun invité';

    const names = this.usersList
      .filter(u => this.eventForm.invitedUserIds.includes(u.id))
      .map(u => `${u.prenom} ${u.nom}`);

    return names.join(', ');
  }

  getDaysInMonth(): Date[] {
    const year = this.currentMonth().getFullYear();
    const month = this.currentMonth().getMonth();
    const firstDay = new Date(year, month, 1);
    const lastDay = new Date(year, month + 1, 0);
    const days: Date[] = [];

    const firstDayOfWeek = firstDay.getDay();
    const mondayBasedIndex = firstDayOfWeek === 0 ? 6 : firstDayOfWeek - 1;

    for (let i = mondayBasedIndex - 1; i >= 0; i--) {
      days.push(new Date(year, month, -i));
    }

    for (let i = 1; i <= lastDay.getDate(); i++) {
      days.push(new Date(year, month, i));
    }

    const remainingDays = 42 - days.length;
    for (let i = 1; i <= remainingDays; i++) {
      days.push(new Date(year, month + 1, i));
    }

    return days;
  }

  getEventsForDate(date: Date): CalendarEvent[] {
    return this.visibleEvents.filter(event =>
      event.date.getDate() === date.getDate() &&
      event.date.getMonth() === date.getMonth() &&
      event.date.getFullYear() === date.getFullYear()
    );
  }

  isCurrentMonth(date: Date): boolean {
    return date.getMonth() === this.currentMonth().getMonth();
  }

  isToday(date: Date): boolean {
    const today = new Date();
    return (
      date.getDate() === today.getDate() &&
      date.getMonth() === today.getMonth() &&
      date.getFullYear() === today.getFullYear()
    );
  }

  isSelected(date: Date): boolean {
    const selected = this.selectedDate();
    if (!selected) return false;

    return (
      date.getDate() === selected.getDate() &&
      date.getMonth() === selected.getMonth() &&
      date.getFullYear() === selected.getFullYear()
    );
  }

  selectDate(date: Date): void {
    this.selectedDate.set(date);
  }

  previousMonth(): void {
    const newDate = new Date(this.currentMonth());
    newDate.setMonth(newDate.getMonth() - 1);
    this.currentMonth.set(newDate);
  }

  nextMonth(): void {
    const newDate = new Date(this.currentMonth());
    newDate.setMonth(newDate.getMonth() + 1);
    this.currentMonth.set(newDate);
  }

  goToToday(): void {
    this.currentMonth.set(new Date());
  }

  getMonthName(): string {
    return this.currentMonth().toLocaleString('fr-FR', {
      month: 'long',
      year: 'numeric'
    });
  }

  getEventColor(event: CalendarEvent): string {
    if (event.ownerId === this.currentUserId) {
      return '#e9d5ff';
    }
    return '#d1fae5';
  }

  openAddEventModal(): void {
    this.isEditing = false;
    this.editingEventId = null;

    this.eventForm = {
      title: '',
      description: '',
      date: this.selectedDate() ? this.formatDate(this.selectedDate()!) : '',
      startTime: '',
      endTime: '',
      projectId: null,
      invitedUserIds: []
    };

    this.inviteSearch = '';
    this.filteredUsers = [...this.usersList];
    this.showEventModal = true;
  }

  openEditEventModal(event: CalendarEvent): void {
    if (event.ownerId !== this.currentUserId) {
      this.showToastMessage('Seul le créateur peut modifier cet événement', 'error');
      return;
    }

    this.isEditing = true;
    this.editingEventId = event.id;

    this.eventForm = {
      title: event.title,
      description: event.description || '',
      date: this.formatDate(event.date),
      startTime: event.startTime || '',
      endTime: event.endTime || '',
      projectId: event.projectId ?? null,
      invitedUserIds: [...event.invitedUserIds]
    };

    this.inviteSearch = '';
    this.filteredUsers = [...this.usersList];
    this.showEventModal = true;
  }


saveEvent(): void {
  if (!this.eventForm.title || !this.eventForm.date) {
    this.showToastMessage('Veuillez remplir les champs obligatoires', 'error');
    return;
  }

  if (
    this.eventForm.startTime &&
    this.eventForm.endTime &&
    this.eventForm.endTime < this.eventForm.startTime
  ) {
    this.showToastMessage("L'heure de fin doit être après l'heure de début", 'error');
    return;
  }

  const eventDate = new Date(this.eventForm.date);
  const startTime = this.eventForm.startTime || '00:00';
  const endTime = this.eventForm.endTime || startTime;

  const invitedUsers = this.usersList.filter(user =>
    this.eventForm.invitedUserIds.includes(user.id)
  );

  const selectedProject = this.myProjects.find(p => p.id === this.eventForm.projectId);

  if (this.isEditing && this.editingEventId) {
    const index = this.allEvents.findIndex(e => e.id === this.editingEventId);

    if (index !== -1) {
      const current = this.allEvents[index];

      if (current.ownerId !== this.currentUserId) {
        this.showToastMessage('Modification non autorisée', 'error');
        return;
      }

      this.allEvents[index] = {
        ...current,
        title: this.eventForm.title,
        description: this.eventForm.description,
        date: eventDate,
        startTime,
        endTime,
        projectId: selectedProject?.id ?? null,
        projectName: selectedProject?.name ?? '',
        invitedUserIds: [...this.eventForm.invitedUserIds],
        invitedUserNames: invitedUsers.map(u => `${u.prenom} ${u.nom}`)
      };
    }

    this.showToastMessage('Événement modifié avec succès', 'success');
    this.saveAllEvents();
    
  } else {
    const newEvent: CalendarEvent = {
      id: Date.now(),
      title: this.eventForm.title,
      description: this.eventForm.description,
      date: eventDate,
      startTime,
      endTime,
      projectId: selectedProject?.id ?? null,
      projectName: selectedProject?.name ?? '',
      ownerId: this.currentUserId,
      ownerName: this.currentUserName,
      invitedUserIds: [...this.eventForm.invitedUserIds],
      invitedUserNames: invitedUsers.map(u => `${u.prenom} ${u.nom}`)
    };

    this.allEvents.push(newEvent);
    this.showToastMessage('Événement ajouté avec succès', 'success');
    this.saveAllEvents();
    
    // ✅ ENVOYER LES NOTIFICATIONS EMAIL AU BACKEND
    if (newEvent.invitedUserIds.length > 0) {
      const invitationData = {
        title: newEvent.title,
        description: newEvent.description,
        date: this.formatDateForBackend(newEvent.date),
        startTime: newEvent.startTime,
        endTime: newEvent.endTime,
        projectId: newEvent.projectId,
        projectName: newEvent.projectName,
        ownerId: newEvent.ownerId,
        ownerName: newEvent.ownerName,
        invitedUserIds: newEvent.invitedUserIds
      };
      
      this.http.post('http://localhost:8080/api/calendar/send-invitations', invitationData)
        .subscribe({
          next: (response: any) => {
            console.log('Invitations envoyées:', response);
          },
          error: (error) => {
            console.error('Erreur envoi invitations:', error);
          }
        });
    }
  }

  this.refreshVisibleEvents();
  this.closeEventModal();
}

// Ajoutez cette méthode helper
private formatDateForBackend(date: Date): string {
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, '0');
  const day = String(date.getDate()).padStart(2, '0');
  return `${year}-${month}-${day}`;
}

  deleteEvent(eventId: number): void {
    const event = this.allEvents.find(e => e.id === eventId);

    if (!event) return;

    if (event.ownerId !== this.currentUserId) {
      this.showToastMessage('Seul le créateur peut supprimer cet événement', 'error');
      return;
    }

    if (confirm('Voulez-vous vraiment supprimer cet événement ?')) {
      this.allEvents = this.allEvents.filter(e => e.id !== eventId);
      this.saveAllEvents();
      this.refreshVisibleEvents();
      this.showToastMessage('Événement supprimé avec succès', 'success');
    }
  }

  closeEventModal(): void {
    this.showEventModal = false;
    this.eventForm = {
      title: '',
      description: '',
      date: '',
      startTime: '',
      endTime: '',
      projectId: null,
      invitedUserIds: []
    };
    this.inviteSearch = '';
    this.filteredUsers = [...this.usersList];
  }

  canEditEvent(event: CalendarEvent): boolean {
    return event.ownerId === this.currentUserId;
  }

  formatDate(date: Date): string {
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');
    return `${year}-${month}-${day}`;
  }

  formatFullDate(date: Date): string {
    return date.toLocaleString('fr-FR', {
      day: 'numeric',
      month: 'long',
      year: 'numeric'
    });
  }

  getVisibilityLabel(event: CalendarEvent): string {
    if (event.ownerId === this.currentUserId && event.invitedUserIds.length === 0) {
      return 'Privé';
    }
    if (event.ownerId === this.currentUserId && event.invitedUserIds.length > 0) {
      return 'Partagé avec invités';
    }
    return 'Invitation';
  }

  showToastMessage(message: string, type: 'success' | 'error'): void {
    this.toastMessage = message;
    this.toastType = type;
    this.showToast = true;

    setTimeout(() => {
      this.showToast = false;
    }, 3000);
  }
}
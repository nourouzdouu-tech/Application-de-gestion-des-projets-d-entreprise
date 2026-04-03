import { Component, OnInit, signal, WritableSignal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterModule } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';

interface CalendarEvent {
  id: number;
  title: string;
  date: Date;
  time?: string;        // Heure au format HH:MM
  description?: string;
  projectName?: string;
}

@Component({
  selector: 'app-calendar',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule],
  templateUrl: './calendar.html',
  styleUrl: './calendar.css'
})
export class CalendarComponent implements OnInit {
  
  currentDate = new Date();
  currentMonth = signal<Date>(new Date());
  selectedDate = signal<Date | null>(null);
  events: CalendarEvent[] = [];
  filteredEvents: CalendarEvent[] = [];
  
  // Modal
  showEventModal = false;
  isEditing = false;
  editingEventId: number | null = null;
  eventForm = {
    title: '',
    description: '',
    date: '',
    time: '',            // Nouveau champ
    projectName: ''
  };
  
  // Toast
  showToast = false;
  toastMessage = '';
  toastType: 'success' | 'error' = 'success';
  
  // Navigation
  currentUser = signal<any>(null);
  activeNavItem = signal<string>('calendrier');
  
  constructor(
    private router: Router,
    public authService: AuthService
  ) {
    this.currentUser.set(this.authService.getUser());
  }
  
  ngOnInit(): void {
    this.loadEvents();
  }
  
  loadEvents(): void {
    const savedEvents = localStorage.getItem('calendar_events');
    if (savedEvents) {
      this.events = JSON.parse(savedEvents).map((e: any) => ({
        ...e,
        date: new Date(e.date)
      }));
    } else {
      // Événements par défaut avec heure
      this.events = [
        {
          id: 1,
          title: 'Refonte Site Web - E-Commerce',
          date: new Date(2026, 1, 9),
          time: '10:00',
          description: 'Lancement du projet de refonte du site e-commerce',
          projectName: 'E-Commerce'
        },
        {
          id: 2,
          title: 'Audit Sécurité Réseau',
          date: new Date(2026, 1, 13),
          time: '14:30',
          description: 'Audit complet de la sécurité réseau',
          projectName: 'Sécurité'
        },
        {
          id: 3,
          title: 'Migration Infrastructure Cloud (Phase 2)',
          date: new Date(2026, 1, 16),
          time: '09:00',
          description: 'Migration vers le cloud phase 2',
          projectName: 'Cloud'
        },
        {
          id: 4,
          title: 'Formation Cybersecurity Equipe',
          date: new Date(2026, 1, 23),
          time: '11:00',
          description: 'Formation sur la cybersécurité pour toute l\'équipe',
          projectName: 'Formation'
        }
      ];
      this.saveEvents();
    }
    this.filterEventsByMonth();
  }
  
  saveEvents(): void {
    localStorage.setItem('calendar_events', JSON.stringify(this.events));
  }
  
  filterEventsByMonth(): void {
    const year = this.currentMonth().getFullYear();
    const month = this.currentMonth().getMonth();
    
    this.filteredEvents = this.events.filter(event => 
      event.date.getFullYear() === year && 
      event.date.getMonth() === month
    );
  }
  
  getDaysInMonth(): Date[] {
    const year = this.currentMonth().getFullYear();
    const month = this.currentMonth().getMonth();
    const firstDay = new Date(year, month, 1);
    const lastDay = new Date(year, month + 1, 0);
    const days: Date[] = [];
    
    const firstDayOfWeek = firstDay.getDay();
    for (let i = firstDayOfWeek - 1; i >= 0; i--) {
      const prevDate = new Date(year, month, -i);
      days.push(prevDate);
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
    return this.events.filter(event => 
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
    return date.getDate() === today.getDate() &&
           date.getMonth() === today.getMonth() &&
           date.getFullYear() === today.getFullYear();
  }
  
  isSelected(date: Date): boolean {
    return this.selectedDate() !== null &&
           date.getDate() === this.selectedDate()!.getDate() &&
           date.getMonth() === this.selectedDate()!.getMonth() &&
           date.getFullYear() === this.selectedDate()!.getFullYear();
  }
  
  selectDate(date: Date): void {
    this.selectedDate.set(date);
  }
  
  previousMonth(): void {
    const newDate = new Date(this.currentMonth());
    newDate.setMonth(newDate.getMonth() - 1);
    this.currentMonth.set(newDate);
    this.filterEventsByMonth();
  }
  
  nextMonth(): void {
    const newDate = new Date(this.currentMonth());
    newDate.setMonth(newDate.getMonth() + 1);
    this.currentMonth.set(newDate);
    this.filterEventsByMonth();
  }
  
  goToToday(): void {
    this.currentMonth.set(new Date());
    this.filterEventsByMonth();
  }
  
  getMonthName(): string {
    return this.currentMonth().toLocaleString('fr-FR', { month: 'long', year: 'numeric' });
  }
  
  getEventColor(event: CalendarEvent): string {
    const colors = ['#e0e7ff', '#d1fae5', '#fed7aa', '#fecaca', '#e9d5ff'];
    let hash = 0;
    for (let i = 0; i < event.title.length; i++) {
      hash = ((hash << 5) - hash) + event.title.charCodeAt(i);
      hash |= 0;
    }
    return colors[Math.abs(hash) % colors.length];
  }
  
  openAddEventModal(): void {
    this.isEditing = false;
    this.editingEventId = null;
    this.eventForm = {
      title: '',
      description: '',
      date: this.selectedDate() ? this.formatDate(this.selectedDate()!) : '',
      time: '',
      projectName: ''
    };
    this.showEventModal = true;
  }
  
  openEditEventModal(event: CalendarEvent): void {
    this.isEditing = true;
    this.editingEventId = event.id;
    this.eventForm = {
      title: event.title,
      description: event.description || '',
      date: this.formatDate(event.date),
      time: event.time || '',
      projectName: event.projectName || ''
    };
    this.showEventModal = true;
  }
  
  saveEvent(): void {
    if (!this.eventForm.title || !this.eventForm.date) {
      this.showToastMessage('Veuillez remplir tous les champs obligatoires', 'error');
      return;
    }
    
    const eventDate = new Date(this.eventForm.date);
    const eventTime = this.eventForm.time || '00:00';
    
    // Vérifier les conflits (même date et même heure)
    const conflictingEvent = this.events.find(event => {
      if (this.isEditing && event.id === this.editingEventId) return false;
      return event.date.toDateString() === eventDate.toDateString() &&
             (event.time || '00:00') === eventTime;
    });
    
    if (conflictingEvent) {
      this.showToastMessage(`Conflit : un événement existe déjà le ${this.formatDate(eventDate)} à ${eventTime}`, 'error');
      return;
    }
    
    if (this.isEditing && this.editingEventId) {
      // Modification
      const index = this.events.findIndex(e => e.id === this.editingEventId);
      if (index !== -1) {
        this.events[index] = {
          ...this.events[index],
          title: this.eventForm.title,
          description: this.eventForm.description,
          date: eventDate,
          time: eventTime,
          projectName: this.eventForm.projectName
        };
      }
      this.showToastMessage('Événement modifié avec succès', 'success');
    } else {
      // Ajout
      const newEvent: CalendarEvent = {
        id: Date.now(),
        title: this.eventForm.title,
        description: this.eventForm.description,
        date: eventDate,
        time: eventTime,
        projectName: this.eventForm.projectName
      };
      this.events.push(newEvent);
      this.showToastMessage('Événement ajouté avec succès', 'success');
    }
    
    this.saveEvents();
    this.filterEventsByMonth();
    this.closeEventModal();
  }
  
  deleteEvent(eventId: number): void {
    if (confirm('Voulez-vous vraiment supprimer cet événement ?')) {
      this.events = this.events.filter(e => e.id !== eventId);
      this.saveEvents();
      this.filterEventsByMonth();
      this.showToastMessage('Événement supprimé avec succès', 'success');
    }
  }
  
  closeEventModal(): void {
    this.showEventModal = false;
    this.eventForm = { title: '', description: '', date: '', time: '', projectName: '' };
  }
  
  formatDate(date: Date): string {
    return date.toISOString().split('T')[0];
  }
  
  formatDay(date: Date): string {
    return date.toLocaleString('fr-FR', { weekday: 'short' }).toUpperCase();
  }
  
  formatFullDate(date: Date): string {
    return date.toLocaleString('fr-FR', { 
      day: 'numeric', 
      month: 'long', 
      year: 'numeric' 
    });
  }
  
  showToastMessage(message: string, type: 'success' | 'error'): void {
    this.toastMessage = message;
    this.toastType = type;
    this.showToast = true;
    setTimeout(() => {
      this.showToast = false;
    }, 3000);
  }
  
  // Navigation
  logout(): void {
    this.authService.removeUser();
    this.router.navigate(['/login']);
  }
  
  setNav(item: string): void {
    this.activeNavItem.set(item);
    if (item === 'dashboard') this.router.navigate(['/chef-projet']);
    else if (item === 'projets') this.router.navigate(['/chef-projet/projets']);
    else if (item === 'equipes') this.router.navigate(['/chef-projet/equipes']);
    else if (item === 'calendrier') this.router.navigate(['/chef-projet/calendrier']);
  }
  
  getInitials(): string {
    const user = this.currentUser();
    if (user?.prenom && user?.nom) {
      return (user.prenom.charAt(0) + user.nom.charAt(0)).toUpperCase();
    }
    return 'U';
  }
}
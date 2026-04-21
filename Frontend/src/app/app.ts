import { Component, signal, OnInit, OnDestroy, HostListener } from '@angular/core';
import { RouterOutlet, Router, NavigationEnd } from '@angular/router';
import { Location } from '@angular/common';
import { filter } from 'rxjs/operators';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet],
  templateUrl: './app.html',
  styleUrl: './app.css'
})
export class App implements OnInit, OnDestroy {
  protected readonly title = signal('gestion-projet');
  private popStateHandler: (() => void) | null = null;

  constructor(private router: Router, private location: Location) {}

  ngOnInit() {
    // Désactiver la navigation arrière
    this.disableBackNavigation();
    
    // Écouter les changements de route
    this.router.events.pipe(
      filter(event => event instanceof NavigationEnd)
    ).subscribe(() => {
      this.disableBackNavigation();
    });
  }

  @HostListener('window:popstate', ['$event'])
  onPopState(event: PopStateEvent) {
    this.disableBackNavigation();
  }

  private disableBackNavigation(): void {
    const token = localStorage.getItem('token');
    const currentUrl = this.router.url;
    
    console.log('URL actuelle:', currentUrl, 'Token:', !!token);
    
    // Si l'utilisateur est connecté et n'est pas sur login
    if (token && currentUrl !== '/login') {
      // Empêcher de revenir en arrière
      history.pushState(null, '', currentUrl);
    }
    
    // Si sur la page login (non connecté)
    if (!token && currentUrl === '/login') {
      history.pushState(null, '', '/login');
    }
  }

  ngOnDestroy() {
    // Nettoyage si nécessaire
    if (this.popStateHandler) {
      window.removeEventListener('popstate', this.popStateHandler);
    }
  }
}
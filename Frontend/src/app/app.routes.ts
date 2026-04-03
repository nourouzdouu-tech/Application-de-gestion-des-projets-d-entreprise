import { Routes } from '@angular/router';
import { Login } from './auth/login/login';
import { ChefProjet } from './features/chef-projet/chef-projet';
import { Projets } from './features/chef-projet/projets/projets';
import { Equipes } from './features/chef-projet/equipes/equipes';
import { Utilisateurs } from './features/admin/utilisateurs/utilisateurs';
import { Clients } from './features/admin/clients/clients';
import { CalendarComponent } from './features/chef-projet/calendar/calendar';

export const routes: Routes = [
  {
    path: 'login',
    component: Login
  },
  {
    path: 'change-password',
    loadComponent: () => import('./auth/change-password/change-password')
      .then(m => m.ChangePassword)
  },
  {
    path: 'admin',
    component: Utilisateurs
  },
  {
    path: 'admin/clients',
    component: Clients
  },
  
   
  {
    path: 'chef-projet',
    loadComponent: () =>
      import('./features/chef-projet/chef-projet')
        .then(m => m.ChefProjet)
  },
  {
    path: 'chef-projet/projets',
    loadComponent: () =>
      import('./features/chef-projet/projets/projets')
        .then(m => m.Projets)
  },
  {
    path: 'chef-projet/equipes',
    loadComponent: () =>
      import('./features/chef-projet/equipes/equipes')
        .then(m => m.Equipes)
  },
  {
    path: 'chef-projet',
    component: ChefProjet,
    children: [
      { path: 'projets', component: Projets },
      { path: 'equipes', component: Equipes },
      { path: 'calendrier', component: CalendarComponent },
      { path: '', redirectTo: 'projets', pathMatch: 'full' }
    ]
  },
  {
    path: '',
    redirectTo: '/login',
    pathMatch: 'full'
  }
];
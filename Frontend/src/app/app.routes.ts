import { Routes } from '@angular/router';
import { Login } from './auth/login/login';
import { ChefProjet } from './features/chef-projet/chef-projet';
import { Projets } from './features/chef-projet/projets/projets';
import { Equipes } from './features/chef-projet/equipes/equipes';
import { Utilisateurs } from './features/admin/utilisateurs/utilisateurs';
import { Clients } from './features/admin/clients/clients';
import { CalendarComponent } from './features/chef-projet/calendar/calendar';
import { ResponsableContract } from './features/responsable-contract/responsable-contract';
import { Projets as ResponsableProjets } from './features/responsable-contract/projets/projets';
import { TjmCalculatorComponent } from './features/responsable-contract/tjm-calculator/tjm-calculator';
import { TachesComponent } from './features/chef-projet/taches/taches';
export const routes: Routes = [
  {
    path: 'login',
    component: Login
  },
  {
    path: 'change-password',
    loadComponent: () =>
      import('./auth/change-password/change-password')
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
    component: ChefProjet,
    children: [
      { path: 'projets', component: Projets },
      { path: 'taches', component: TachesComponent },
      { path: 'equipes', component: Equipes },
      { path: 'calendrier', component: CalendarComponent },
      { path: '', redirectTo: 'projets', pathMatch: 'full' }
    ]
  },

  {
    path: 'responsable-contrat',
    component: ResponsableContract,
    children: [
      { path: 'projets',     component: ResponsableProjets },
      { path: 'facturation', component: TjmCalculatorComponent },
      { path: '',            redirectTo: 'projets', pathMatch: 'full' }
    ]
  },

  {
    path: '',
    redirectTo: '/login',
    pathMatch: 'full'
  }
];
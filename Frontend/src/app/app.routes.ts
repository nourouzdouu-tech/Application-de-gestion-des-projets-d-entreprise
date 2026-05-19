import { Routes } from '@angular/router';

import { Login } from './auth/login/login';
import { Admin } from './features/admin/admin';
import { Dashboard } from './shared/components/dashboard/dashboard';
import { Utilisateurs } from './features/admin/utilisateurs/utilisateurs';
import { Clients } from './features/admin/clients/clients';
import { Roles } from './features/admin/roles/roles';
import { Profiles } from './features/admin/profile/profile';

import { ChefProjet } from './features/chef-projet/chef-projet';
import { Projets } from './features/chef-projet/projets/projets';
import { Equipes } from './features/chef-projet/equipes/equipes';
import { TachesComponent } from './features/chef-projet/taches/taches';

import { ResponsableContract } from './features/responsable-contract/responsable-contract';
import { Projets as ResponsableProjets } from './features/responsable-contract/projets/projets';
import { TjmCalculatorComponent } from './features/responsable-contract/tjm-calculator/tjm-calculator';

import { ManagerLayoutComponent } from './features/manager/manager';
import { ReviewProjetsComponent } from './features/manager/projets/projets';

import { MembreEquipe } from './features/membre-equipe/membre-equipe';
import { MembreEquipeProjets } from './features/membre-equipe/projets/projets';
import { MembreEquipeTaches } from './features/membre-equipe/taches/taches';

import { SharedMessagesComponent } from './shared/components/messages/messages';
import { SharedCalendarComponent } from './shared/components/calendar/calendar';

// ✅ NOUVEAU : Import du composant AUDIT partagé (plus les anciens imports supprimés)
import { AuditComponent } from './shared/components/audit/audit';




export const routes: Routes = [
  {
    path: 'login',
    component: Login
  },
  {
    path: 'change-password',
    loadComponent: () =>
      import('./auth/change-password/change-password').then(
        m => m.ChangePassword
      )
  },
  {
    path: 'admin',
    component: Admin,
    children: [
      { path: 'dashboard', component: Dashboard },
  
      { path: 'utilisateurs', component: Utilisateurs },
      { path: 'roles', component: Roles },
      { path: 'clients', component: Clients },
      { path: 'profile', component: Profiles },
      // ✅ MODIFIÉ : utilise le composant partagé avec viewType 'security'
      { path: 'audit', component: AuditComponent, data: { viewType: 'security' } },
      { path: '', redirectTo: 'dashboard', pathMatch: 'full' }
    ]
  },
  {
    path: 'chef-projet',
    component: ChefProjet,
    children: [
      { path: 'dashboard', component: Dashboard },
  
      { path: 'projets', component: Projets },
      { path: 'taches', component: TachesComponent },
      { path: 'equipes', component: Equipes },
      { path: 'calendrier', component: SharedCalendarComponent },
      { path: 'messages', component: SharedMessagesComponent },
      { path: '', redirectTo: 'dashboard', pathMatch: 'full' }
    ]
  },
  {
    path: 'responsable-contrat',
    component: ResponsableContract,
    children: [
      { path: 'dashboard', component: Dashboard },

      { path: 'projets', component: ResponsableProjets },
      { path: 'facturation/:projectId', component: TjmCalculatorComponent },
      { path: '', redirectTo: 'dashboard', pathMatch: 'full' }
    ]
  },
  {
    path: 'manager',
    component: ManagerLayoutComponent,
    children: [
      { path: 'dashboard', component: Dashboard },

      { path: 'projets', component: ReviewProjetsComponent },
      { path: 'calendrier', component: SharedCalendarComponent },
      { path: 'messages', component: SharedMessagesComponent },
      // ✅ MODIFIÉ : utilise le composant partagé avec viewType 'workflow'
      { path: 'audit', component: AuditComponent, data: { viewType: 'workflow' } },
      { path: '', redirectTo: 'dashboard', pathMatch: 'full' }
    ]
  },
  {
    path: 'membre-equipe',
    component: MembreEquipe,
    children: [
      { path: 'dashboard', component: Dashboard },

      { path: 'projets', component: MembreEquipeProjets },
      { path: 'taches', component: MembreEquipeTaches },
      { path: 'calendrier', component: SharedCalendarComponent },
      { path: 'messages', component: SharedMessagesComponent },
      { path: '', redirectTo: 'dashboard', pathMatch: 'full' }
    ]
  },
  {
    path: '',
    redirectTo: '/login',
    pathMatch: 'full'
  },
  {
    path: '**',
    redirectTo: '/login'
  }
];
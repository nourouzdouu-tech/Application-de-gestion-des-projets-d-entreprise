import { Routes } from '@angular/router';
import { Login } from './auth/login/login';
import { ChefProjet } from './features/chef-projet/chef-projet';
import { Projets } from './features/chef-projet/projets/projets';
import { Equipes } from './features/chef-projet/equipes/equipes';

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
  loadComponent: () => import('./features/admin/utilisateurs/utilisateurs')
    .then(m => m.Utilisateurs)
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
  component: ChefProjet, // layout
  children: [
    { path: 'projets', component: Projets },
    { path: 'equipes', component: Equipes},
    { path: '', redirectTo: 'projets', pathMatch: 'full' }
  ]
},

  {
    path: '',
    redirectTo: '/login',
    pathMatch: 'full'
  }
  
];
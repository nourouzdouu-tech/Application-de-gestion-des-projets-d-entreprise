import { Routes } from '@angular/router';
import { Login } from './auth/login/login';

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
    path: '',
    redirectTo: '/login',
    pathMatch: 'full'
  }
  
];
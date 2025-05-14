import { Routes } from '@angular/router';

export const routes: Routes = [
  {
    path: '/register',
    loadComponent: () => import('./auth/register.component').then(m => m.RegisterComponent),
  },
  {
    path: '/login',
    loadComponent: () => import('./auth/login.component').then(m => m.LoginComponent),
  },
  {
    path: '**',
    redirectTo: 'login',
  },
];


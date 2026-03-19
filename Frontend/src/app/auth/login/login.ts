import { Component, ChangeDetectionStrategy, signal, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '../../core/services/auth.service';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './login.html',
  styleUrl: './login.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class Login {
  private fb = inject(FormBuilder);
  private authService = inject(AuthService);
  private router = inject(Router);

  loginForm: FormGroup;
  showPassword = signal(false);
  isLoading = signal(false);
  errorMessage = signal('');

  dxcLogo = 'https://www.figma.com/api/mcp/asset/8810bca1-a80c-4e40-aed6-0f49a6cd8abd';
  emailIcon = 'https://www.figma.com/api/mcp/asset/5851c189-fccc-44e6-8d8b-1c97f6397e5d';
  lockIcon = 'https://www.figma.com/api/mcp/asset/7ff44673-75fe-43c5-a0dc-93ab259d71a8';
  eyeIcon = 'https://www.figma.com/api/mcp/asset/18e393ad-0583-494f-b0a2-876e80095851';
  submitIcon = 'https://www.figma.com/api/mcp/asset/d0c2a9f2-a95d-4ceb-b349-84a1a28e767b';

  constructor() {
    this.loginForm = this.fb.group({
      email: ['', [Validators.required, Validators.email]],
      password: ['', [Validators.required, Validators.minLength(8)]],
    });
  }

  togglePasswordVisibility(): void {
    this.showPassword.update((value) => !value);
  }

  onSubmit(): void {
    if (this.loginForm.valid) {
      this.isLoading.set(true);
      this.errorMessage.set('');
      const { email, password } = this.loginForm.value;
      this.authService.login(email, password).subscribe({
        next: (response) => {
          console.log('Login réussi:', response);
          this.authService.saveToken(response.accessToken);
          this.authService.saveUser(response);
          this.isLoading.set(false);
          this.router.navigateByUrl(response.redirectTo);
        },
        
        error: (error) => {
          console.error('Erreur login:', error);
          this.isLoading.set(false);
          if (error.status === 401) {
            this.errorMessage.set('Email ou mot de passe incorrect');
          } else if (error.status === 403) {
            this.errorMessage.set('Compte verrouillé ou désactivé');
          } else {
            this.errorMessage.set(error.error?.message || 'Erreur de connexion');
          }
        },
      });
    } else {
      this.errorMessage.set('Veuillez remplir correctement tous les champs');
    }
  }

  get email() {
    return this.loginForm.get('email');
  }

  get password() {
    return this.loginForm.get('password');
  }
  
}
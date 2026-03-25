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

  dxcLogo = '/assets/DXC_logo.png';

  // Email Icon SVG
  emailIconSvg = `<path d="M3 8l9 6 9-6M3 8v10a2 2 0 002 2h14a2 2 0 002-2V8M3 8l6 4m12-4l-6 4" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>`;

  // Lock Icon SVG
  lockIconSvg = `<path d="M12 1C6.477 1 2 5.477 2 11v2c0 5.523 4.477 10 10 10s10-4.477 10-10v-2c0-5.523-4.477-10-10-10zm0 2c4.418 0 8 3.582 8 8v2c0 4.418-3.582 8-8 8s-8-3.582-8-8v-2c0-4.418 3.582-8 8-8z" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>`;

  // Eye Icon SVG (show)
  eyeShowSvg = `<path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/><circle cx="12" cy="12" r="3" stroke-width="2"/>`;

  // Eye Icon SVG (hide)
  eyeHideSvg = `<path d="M17.94 17.94A10.07 10.07 0 0 1 12 20c-7 0-11-8-11-8a18.45 18.45 0 0 1 5.06-5.94M9.9 4A9.97 9.97 0 0 1 12 4c7 0 11 8 11 8a18.5 18.5 0 0 1-2.16 3.19m-6.72-1.07a3 3 0 1 1-4.24-4.24" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/><line x1="1" y1="1" x2="23" y2="23" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>`;

  // Submit Button Icon SVG
  submitIconSvg = `<path d="M13.5 0h-12v24h12V0zm8.5 11h-8v2h8v-2zM14.5 3v2h8V3h-8z"/>`;

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
import { Component, ChangeDetectionStrategy, ChangeDetectorRef, signal, inject, ViewChild, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '../../core/services/auth.service';
import { WebAuthnService } from '../../core/services/webauthn.service';
import { BiometricSetupModalComponent } from '../biometric-setup-modal/biometric-setup-modal';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, BiometricSetupModalComponent],
  templateUrl: './login.html',
  styleUrl: './login.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class Login implements OnInit {
  private fb = inject(FormBuilder);
  private authService = inject(AuthService);
  private webAuthnService = inject(WebAuthnService);
  private router = inject(Router);
  private cdr = inject(ChangeDetectorRef);

  @ViewChild(BiometricSetupModalComponent) setupModal!: BiometricSetupModalComponent;

  loginForm: FormGroup;
  showPassword = signal(false);
  isLoading = signal(false);
  errorMessage = signal('');
  showSetupModal = signal(false);
  pendingEmail = signal('');

  biometricAvailable = false;
  biometricLabel = 'Empreinte digitale';

  dxcLogo = '/assets/DXC_logo.png';

  emailIconSvg = `<path d="M3 8l9 6 9-6M3 8v10a2 2 0 002 2h14a2 2 0 002-2V8M3 8l6 4m12-4l-6 4" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>`;
  lockIconSvg = `<path d="M12 1C6.477 1 2 5.477 2 11v2c0 5.523 4.477 10 10 10s10-4.477 10-10v-2c0-5.523-4.477-10-10-10zm0 2c4.418 0 8 3.582 8 8v2c0 4.418-3.582 8-8 8s-8-3.582-8-8v-2c0-4.418 3.582-8 8-8z" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>`;
  eyeShowSvg = `<path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/><circle cx="12" cy="12" r="3" stroke-width="2"/>`;
  eyeHideSvg = `<path d="M17.94 17.94A10.07 10.07 0 0 1 12 20c-7 0-11-8-11-8a18.45 18.45 0 0 1 5.06-5.94M9.9 4A9.97 9.97 0 0 1 12 4c7 0 11 8 11 8a18.5 18.5 0 0 1-2.16 3.19m-6.72-1.07a3 3 0 1 1-4.24-4.24" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/><line x1="1" y1="1" x2="23" y2="23" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>`;
  submitIconSvg = `<path d="M13.5 0h-12v24h12V0zm8.5 11h-8v2h8v-2zM14.5 3v2h8V3h-8z"/>`;

  constructor() {
    this.loginForm = this.fb.group({
      email: ['', [Validators.required, Validators.email]],
      password: ['', [Validators.required, Validators.minLength(8)]],
    });
  }

  async ngOnInit(): Promise<void> {
    if (window.PublicKeyCredential) {
      this.biometricAvailable =
        await PublicKeyCredential.isUserVerifyingPlatformAuthenticatorAvailable();
    }
    // ✅ Détecte correctement FaceID sur Windows (Windows Hello)
    const isIOS = /iPhone|iPad|iPod/.test(navigator.userAgent);
    const isWindows = /Windows/.test(navigator.userAgent);
    
    if (isIOS) {
      this.biometricLabel = 'Face ID';
    } else if (isWindows && this.biometricAvailable) {
      // Windows Hello peut supporter Face ID ou empreinte
      this.biometricLabel = 'Windows Hello (Face ID ou Empreinte)';
    } else {
      this.biometricLabel = 'Empreinte digitale';
    }
    this.cdr.markForCheck();
  }

  togglePasswordVisibility(): void {
    this.showPassword.update(v => !v);
  }

  // ← MÉTHODE UNIQUE navigateByRoles avec logging ET NORMALISATION
  private navigateByRoles(roles: any[]): void {
    console.log('[Login] navigateByRoles - raw roles:', roles);
    
    const normalized = roles.map((r: any) => {
      let roleStr = typeof r === 'string' ? r : (r?.nom || r?.authority || '');
      roleStr = roleStr.trim().toUpperCase();
      // ✅ Supprimer le préfixe 'ROLE_' si présent pour normaliser
      if (roleStr.startsWith('ROLE_')) {
        roleStr = roleStr.substring(5);
      }
      return roleStr;
    });
    
    console.log('[Login] navigateByRoles - normalized roles:', normalized);

    if (normalized.some(r => r === 'CHEF_PROJET')) {
      console.log('[Login] Redirecting to: /chef-projet/dashboard');
      this.router.navigateByUrl('/chef-projet/dashboard');
    } else if (normalized.some(r => r === 'RESPONSABLE_CONTRAT')) {
      console.log('[Login] Redirecting to: /responsable-contrat/dashboard');
      this.router.navigateByUrl('/responsable-contrat/dashboard');
    } else if (normalized.some(r => r === 'ADMIN')) {
      console.log('[Login] Redirecting to: /admin/dashboard');
      this.router.navigateByUrl('/admin/dashboard');
    } else if (normalized.some(r => r === 'MANAGER')) {
      console.log('[Login] Redirecting to: /manager/dashboard');
      this.router.navigateByUrl('/manager/dashboard');
    } else if (normalized.some(r => r === 'MEMBRE_EQUIPE')) {
      console.log('[Login] Redirecting to: /membre-equipe/dashboard');
      this.router.navigateByUrl('/membre-equipe/dashboard');
    } else {
      console.error('[Login] ❌ Unrecognized roles:', normalized);
      this.errorMessage.set('Rôle non reconnu : ' + normalized.join(', '));
    }
  }

  onSubmit(): void {
    if (!this.loginForm.valid) {
      this.errorMessage.set('Veuillez remplir correctement tous les champs');
      return;
    }

    this.isLoading.set(true);
    this.errorMessage.set('');

    const { email, password } = this.loginForm.value;
    const emailNormalized = email.trim().toLowerCase();

    this.authService.login(email, password).subscribe({
      next: async (response) => {
        this.authService.saveToken(response.accessToken);
        this.authService.saveUser(response);
        this.isLoading.set(false);

        const bioAvailable = window.PublicKeyCredential &&
          await PublicKeyCredential.isUserVerifyingPlatformAuthenticatorAvailable();

        if (bioAvailable) {
          try {
            const hasCredential = await this.webAuthnService.hasCredential(emailNormalized);
            if (!hasCredential) {
              this.pendingEmail.set(emailNormalized);
              this.showSetupModal.set(true);
              this.cdr.detectChanges();
              return;
            }
          } catch {
            // En cas d'erreur réseau ou 401, on navigue normalement
          }
        }

        this.navigateByRoles(response.roles || []);
      },

      error: (error) => {
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
  }

  async onModalAccepted(): Promise<void> {
    try {
      await this.webAuthnService.registerWithWebAuthn(this.pendingEmail());
      this.setupModal.showSuccess();
    } catch (e: any) {
      const cancelled = e?.name === 'NotAllowedError';
      this.setupModal.showError(cancelled);
    } finally {
      this.cdr.detectChanges();
    }
  }

  onModalDeclined(): void {
    this.showSetupModal.set(false);
    this.cdr.detectChanges();
    const user = this.authService.getUser();
    this.navigateByRoles(user?.roles || []);
  }

  async loginWithBiometrics(): Promise<void> {
    const emailValue = this.email?.value?.trim();
    if (!emailValue || !this.email?.valid) {
      this.errorMessage.set('Veuillez entrer votre adresse email avant d\'utiliser la biométrie');
      return;
    }

    if (!('credentials' in navigator)) {
      this.errorMessage.set('Navigateur ne supporte pas WebAuthn');
      return;
    }

    this.isLoading.set(true);
    this.errorMessage.set('');

    try {
      console.log('[Login] Starting WebAuthn login for:', emailValue);
      
      const me = await this.webAuthnService.loginWithWebAuthn(emailValue);
      
      console.log('[Login] WebAuthn success, user data:', me);
      console.log('[Login] Roles received:', me.roles);
      
      if (!me.roles || me.roles.length === 0) {
        console.error('[Login] ❌ No roles found in response!');
        this.errorMessage.set('Aucun rôle trouvé pour cet utilisateur');
        return;
      }
      
      this.navigateByRoles(me.roles);
    } catch (err: any) {
      console.error('[Login] WebAuthn error:', err);
      this.errorMessage.set(err?.error?.message || err?.message || 'Erreur d\'authentification biométrique');
    } finally {
      this.isLoading.set(false);
      this.cdr.detectChanges();
    }
  }

  get email() { return this.loginForm.get('email'); }
  get password() { return this.loginForm.get('password'); }
}
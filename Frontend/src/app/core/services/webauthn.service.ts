import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { firstValueFrom } from 'rxjs';
import { AuthService } from './auth.service';

function base64ToUint8Array(base64: string): Uint8Array {
  const pad = '='.repeat((4 - (base64.length % 4)) % 4);
  const base64Fixed = (base64 + pad).replace(/-/g, '+').replace(/_/g, '/');
  const raw = window.atob(base64Fixed);
  const arr = new Uint8Array(raw.length);
  for (let i = 0; i < raw.length; ++i) arr[i] = raw.charCodeAt(i);
  return arr;
}

function uint8ArrayToBase64url(buffer: ArrayBuffer | Uint8Array): string {
  const bytes = buffer instanceof ArrayBuffer ? new Uint8Array(buffer) : buffer;
  let binary = '';
  for (let i = 0; i < bytes.byteLength; i++) {
    binary += String.fromCharCode(bytes[i]);
  }
  return window.btoa(binary)
    .replace(/\+/g, '-')
    .replace(/\//g, '_')
    .replace(/=+$/, '');
}

function serializeRegistrationCredential(cred: PublicKeyCredential): any {
  const response = cred.response as AuthenticatorAttestationResponse;

  const clientDataJSON = new Uint8Array(response.clientDataJSON);
  const attestationObject = new Uint8Array(response.attestationObject);

  console.log('[WebAuthn] Registration - clientDataJSON length:', clientDataJSON.length);
  console.log('[WebAuthn] Registration - attestationObject length:', attestationObject.length);
  console.log('[WebAuthn] Registration - Raw ID length:', cred.rawId.byteLength);

  return {
    id: cred.id,
    rawId: uint8ArrayToBase64url(cred.rawId),
    type: cred.type,
    response: {
      clientDataJSON: uint8ArrayToBase64url(response.clientDataJSON),
      attestationObject: uint8ArrayToBase64url(response.attestationObject),
      transports: (response as any).getTransports?.() || [],
    },
    clientExtensionResults: cred.getClientExtensionResults?.() ?? {},
  };
}

function serializeAssertionCredential(cred: PublicKeyCredential): any {
  const response = cred.response as AuthenticatorAssertionResponse;

  console.log('[WebAuthn] Assertion - clientDataJSON length:', response.clientDataJSON.byteLength);
  console.log('[WebAuthn] Assertion - authenticatorData length:', response.authenticatorData.byteLength);
  console.log('[WebAuthn] Assertion - signature length:', response.signature.byteLength);

  return {
    id: cred.id,
    rawId: uint8ArrayToBase64url(cred.rawId),
    type: cred.type,
    response: {
      clientDataJSON: uint8ArrayToBase64url(response.clientDataJSON),
      authenticatorData: uint8ArrayToBase64url(response.authenticatorData),
      signature: uint8ArrayToBase64url(response.signature),
      userHandle: response.userHandle ? uint8ArrayToBase64url(response.userHandle) : null,
    },
    clientExtensionResults: cred.getClientExtensionResults?.() ?? {},
  };
}

@Injectable({ providedIn: 'root' })
export class WebAuthnService {
  private apiUrl = 'http://localhost:8080/api/auth';

  constructor(private http: HttpClient, private auth: AuthService) {}

  private convertCreationOptions(options: any): any {
    const o: any = { ...options };
    const publicKey = o.publicKey ? { ...o.publicKey } : { ...o };
    publicKey.challenge = base64ToUint8Array(publicKey.challenge);
    if (publicKey.user && publicKey.user.id)
      publicKey.user.id = base64ToUint8Array(publicKey.user.id);
    if (publicKey.excludeCredentials) {
      publicKey.excludeCredentials = publicKey.excludeCredentials.map((c: any) => ({
        ...c,
        id: base64ToUint8Array(c.id),
      }));
    }
    publicKey.authenticatorSelection = {
      authenticatorAttachment: 'platform',
      userVerification: 'required',
      residentKey: 'preferred',
      requireResidentKey: false,
    };
    return publicKey;
  }

  private convertRequestOptions(options: any): PublicKeyCredentialRequestOptions {
    const publicKey = options.publicKey ? { ...options.publicKey } : { ...options };
    publicKey.challenge = base64ToUint8Array(publicKey.challenge);
    if (publicKey.allowCredentials) {
      publicKey.allowCredentials = publicKey.allowCredentials.map((c: any) => ({
        ...c,
        id: base64ToUint8Array(c.id),
        transports: ['internal'] as AuthenticatorTransport[],
      }));
    }
    publicKey.userVerification = 'required' as UserVerificationRequirement;
    return publicKey;
  }

  async registerWithWebAuthn(email: string): Promise<void> {
    const emailNormalized = email.trim().toLowerCase();

    console.log('[WebAuthn] Starting registration for:', emailNormalized);

    const optionsStr = await firstValueFrom(
      this.http.post(`${this.apiUrl}/webauthn/register/options`,
        { email: emailNormalized },
        { responseType: 'text' as 'json' })
    );

    console.log('[WebAuthn] Raw options from server:', optionsStr);

    const options = typeof optionsStr === 'string' ? JSON.parse(optionsStr) : optionsStr;
    console.log('[WebAuthn] Parsed options:', options);

    const publicKey = this.convertCreationOptions(options);
    console.log('[WebAuthn] Converted public key options:', publicKey);

    const credential = await navigator.credentials.create({ publicKey }) as PublicKeyCredential;
    console.log('[WebAuthn] Credential created:', credential);

    if (!credential) throw new Error('Aucune credential créée');

    const credentialData = serializeRegistrationCredential(credential);
    console.log('[WebAuthn] Serialized credential data:', JSON.stringify(credentialData, null, 2));

    const response = await firstValueFrom(
      this.http.post(`${this.apiUrl}/webauthn/register/verify`, {
        email: emailNormalized,
        credential: credentialData,
      })
    );

    console.log('[WebAuthn] Registration response:', response);
  }

  async loginWithWebAuthn(email: string): Promise<any> {
    const emailNormalized = email.trim().toLowerCase();

    console.log('[WebAuthn] Login attempt for:', emailNormalized);

    const optionsStr = await firstValueFrom(
      this.http.post(`${this.apiUrl}/webauthn/login/options`,
        { email: emailNormalized },
        { responseType: 'text' as 'json' })
    );
    const options = typeof optionsStr === 'string' ? JSON.parse(optionsStr) : optionsStr;
    console.log('[WebAuthn] Options received:', options);

    const publicKey = this.convertRequestOptions(options);
    console.log('[WebAuthn] Public key converted:', publicKey);

    const assertion = await navigator.credentials.get({ publicKey }) as PublicKeyCredential;
    console.log('[WebAuthn] Assertion obtained:', assertion);

    if (!assertion) throw new Error('Aucune assertion obtenue');

    const assertionData = serializeAssertionCredential(assertion);
    console.log('[WebAuthn] Assertion data serialized:', JSON.stringify(assertionData, null, 2));

    const res: any = await firstValueFrom(
      this.http.post(`${this.apiUrl}/webauthn/login/verify`, {
        email: emailNormalized,
        assertion: assertionData,
      })
    );

    const token = res?.token;
    if (!token) throw new Error('Aucun token reçu du serveur');

    // 1. Sauvegarder le token en premier
    this.auth.saveToken(token);

    // 2. Appeler /me avec le header Authorization explicite pour éviter tout timing issue
    const me = await firstValueFrom(
      this.http.get<any>(`${this.apiUrl}/me`, {
        headers: { 'Authorization': `Bearer ${token}` }
      })
    );

    console.log('[WebAuthn] /me response:', JSON.stringify(me));
    console.log('[WebAuthn] roles from /me:', me.roles);

    // ✅ NORMALISE LES RÔLES POUR ASSURER LA COHÉRENCE
    const normalizedRoles = (me.roles || []).map((r: any) => {
      let roleStr = typeof r === 'string' ? r : (r?.nom || r?.authority || '');
      roleStr = roleStr.trim().toUpperCase();
      // Supprimer le préfixe 'ROLE_' si présent pour normaliser
      if (roleStr.startsWith('ROLE_')) {
        roleStr = roleStr.substring(5);
      }
      return roleStr;
    });

    console.log('[WebAuthn] Normalized roles:', normalizedRoles);

    // 3. Sauvegarder l'utilisateur avec accessToken inclus (requis par AuthGuard)
    const userObj = {
      accessToken: token,
      tokenType: 'Bearer',
      id: me.id || 0,
      email: me.email,
      prenom: me.prenom || '',
      nom: me.nom || '',
      roles: normalizedRoles,
      redirectTo: '',
      mustChangePassword: false,
    };

    this.auth.saveUser(userObj as any);
    console.log('[WebAuthn] User saved, token in localStorage:', !!this.auth.getToken());

    return me;
  }

  async hasCredential(email: string): Promise<boolean> {
    const emailNormalized = email.trim().toLowerCase();
    const res = await firstValueFrom(
      this.http.get<{ registered: boolean }>(`${this.apiUrl}/webauthn/has-credential`, {
        params: { email: emailNormalized },
      })
    );
    return !!res?.registered;
  }
}
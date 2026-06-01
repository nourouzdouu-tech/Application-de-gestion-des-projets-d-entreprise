import { Component, Output, EventEmitter, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-biometric-setup-modal',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './biometric-setup-modal.html',
  styleUrls: ['./biometric-setup-modal.css']
})
export class BiometricSetupModalComponent implements OnInit {
  @Output() accepted = new EventEmitter<void>();
  @Output() declined = new EventEmitter<void>();

  state: 'ask' | 'scanning' | 'success' | 'error' = 'ask';
  biometricLabel = 'Face ID';

  constructor(private cdr: ChangeDetectorRef) {}

ngOnInit() {
  const ua = navigator.userAgent.toLowerCase();

  if (/iphone|ipad|macintosh/.test(ua)) {
    this.biometricLabel = 'Face ID';
  } else if (/android/.test(ua)) {
    this.biometricLabel = 'Empreinte digitale';
  } else if (/windows/.test(ua)) {
    this.biometricLabel = 'Windows Hello';  // ← plus générique
  } else {
    this.biometricLabel = 'Biométrie';
  }
}

  confirm() {
    this.state = 'scanning';
    this.cdr.detectChanges();
    this.accepted.emit();
  }

  decline() {
    this.declined.emit();
  }

  showSuccess() {
    this.state = 'success';
    this.cdr.detectChanges();
  }

  showError(cancelled = false) {
  this.errorCancelled = cancelled;
  this.state = 'error';
  this.cdr.detectChanges();
}

errorCancelled = false;
}
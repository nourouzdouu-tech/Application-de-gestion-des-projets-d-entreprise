import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';

interface ConsultantProfile {
  id: number;
  title: string;
  subtitle: string;
  baseRate: number;
  icon: string;
}

@Component({
  selector: 'app-tjm-calculator',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './tjm-calculator.html',
  styleUrls: ['./tjm-calculator.css']
})
export class TjmCalculatorComponent implements OnInit {

  profiles: ConsultantProfile[] = [
    { id: 1, title: 'Senior Consultant', subtitle: 'Expertise métier confirmée +8 ans d\'XP', baseRate: 4500, icon: 'medal' },
    { id: 2, title: 'Junior Consultant', subtitle: 'Profil évolutif 0–3 ans d\'XP', baseRate: 2500, icon: 'graduation' },
    { id: 3, title: 'Expert Technique', subtitle: 'Architecture & Spécialisation Tech', baseRate: 8500, icon: 'cpu' },
    { id: 4, title: 'Chef de Projet', subtitle: 'Gestion de delivery & Coordination', baseRate: 7500, icon: 'briefcase' }
  ];

  selectedProfile: ConsultantProfile = this.profiles[0];
  isForfait = true;
  nombreJours = 0;
  tjmApplique = 0;
  fraisGestion = 0;

  coutTotal = 0;
  remunerationConsultant = 0;
  margeOperationnelle = 0;
  simulationSaved = false;

  ngOnInit(): void {
    this.calculateCost();
  }

  selectProfile(profile: ConsultantProfile): void {
    this.selectedProfile = profile;
    this.tjmApplique = profile.baseRate;
    this.calculateCost();
  }

  calculateCost(): void {
    this.remunerationConsultant = this.nombreJours * this.tjmApplique;
    const marge = this.remunerationConsultant * (this.fraisGestion / 100);
    this.margeOperationnelle = Math.round(marge);
    this.coutTotal = Math.round(this.remunerationConsultant + marge);
  }

  resetForm(): void {
    this.selectedProfile = this.profiles[0];
    this.nombreJours = 220;
    this.tjmApplique = this.profiles[0].baseRate;
    this.fraisGestion = 15;
    this.isForfait = true;
    this.simulationSaved = false;
    this.calculateCost();
  }

  saveSimulation(): void {
    this.simulationSaved = true;
    setTimeout(() => this.simulationSaved = false, 3000);
  }

  exportPdf(): void {
    window.print();
  }

  formatNumber(value: number): string {
    return value.toLocaleString('fr-FR');
  }

  get profileLabel(): string {
    return `${this.selectedProfile.title} (Base ${this.selectedProfile.baseRate} MAD)`;
  }
}
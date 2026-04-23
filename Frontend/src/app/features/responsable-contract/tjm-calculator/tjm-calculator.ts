import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { switchMap, retry, take, catchError } from 'rxjs/operators';
import { of, forkJoin } from 'rxjs';
import { jsPDF } from 'jspdf';
import autoTable from 'jspdf-autotable';
import { ProjectService, ProjectDto } from '../../../core/services/project.service';
import { TeamService, TeamDto } from '../../../core/services/team.service';
import { UserService } from '../../../core/services/user.service';

export type BillingRole = 'MEMBER' | 'MANAGER' | 'CHEF_PROJET';

interface BillingLine {
  id: number | string;
  fullName: string;
  email?: string;
  roleName: string;
  profileLibelle?: string;
  tjm: number;
  nombreJours: number;
  montant: number;
  source: BillingRole;
}

interface SavedBillingData {
  projectId: number;
  savedAt: string;
  billingLines: BillingLine[];
  totalGeneral: number;
  tvaAmount: number;
  totalTTC: number;
}

@Component({
  selector: 'app-tjm-calculator',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule],
  templateUrl: './tjm-calculator.html',
  styleUrls: ['./tjm-calculator.css']
})
export class TjmCalculatorComponent implements OnInit {
  loading = true;
  error: string | null = null;
  successMessage: string | null = null;

  projectId!: number;
  project: ProjectDto | null = null;
  team: TeamDto | null = null;

  billingLines: BillingLine[] = [];

  totalGeneral = 0; // HT
  tvaRate = 0.2; // 20%
  tvaAmount = 0;
  totalTTC = 0;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private projectService: ProjectService,
    private teamService: TeamService,
    private userService: UserService,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.route.paramMap.pipe(take(1)).subscribe(params => {
      const idParam = params.get('projectId');

      if (!idParam) {
        this.error = 'Aucun projectId dans la route';
        this.loading = false;
        this.cdr.detectChanges();
        return;
      }

      this.projectId = Number(idParam);

      if (Number.isNaN(this.projectId)) {
        this.error = 'projectId invalide';
        this.loading = false;
        this.cdr.detectChanges();
        return;
      }

      this.loadBillingData();
    });
  }

  loadBillingData(): void {
    this.loading = true;
    this.error = null;

    this.projectService.getProjectById(this.projectId).pipe(
      retry(2),
      switchMap((project: any) => {
        if (!project) {
          throw new Error('Projet introuvable');
        }

        this.project = project;

        const team$ = project.teamId
          ? this.teamService.getTeamById(project.teamId).pipe(catchError(() => of(null)))
          : of(null);

        const manager$ = project.managerId
          ? this.userService.getUserById(project.managerId).pipe(catchError(() => of(null)))
          : of(null);

        const chef$ = project.chefProjetId
          ? this.userService.getUserById(project.chefProjetId).pipe(catchError(() => of(null)))
          : of(null);

        return forkJoin({ team: team$, manager: manager$, chef: chef$ });
      })
    ).subscribe({
      next: ({ team, manager, chef }: any) => {
        this.team = team;
        const lines: BillingLine[] = [];

        if (manager) {
          lines.push({
            id: manager.id,
            fullName: `${manager.prenom} ${manager.nom}`,
            email: manager.email,
            roleName: 'MANAGER',
            profileLibelle: manager.profileLibelle ?? 'Profil non défini',
            tjm: Number(manager.tjm ?? 0),
            nombreJours: 0,
            montant: 0,
            source: 'MANAGER'
          });
        }

        if (chef) {
          lines.push({
            id: chef.id,
            fullName: `${chef.prenom} ${chef.nom}`,
            email: chef.email,
            roleName: 'CHEF_PROJET',
            profileLibelle: chef.profileLibelle ?? 'Profil non défini',
            tjm: Number(chef.tjm ?? 0),
            nombreJours: 0,
            montant: 0,
            source: 'CHEF_PROJET'
          });
        }

        (team?.members ?? []).forEach((m: any) => {
          lines.push({
            id: m.id,
            fullName: m.fullName,
            email: m.email,
            roleName: m.roleName,
            profileLibelle: m.profileLibelle ?? 'Profil non défini',
            tjm: Number(m.tjm ?? 0),
            nombreJours: 0,
            montant: 0,
            source: 'MEMBER'
          });
        });

        this.billingLines = lines;
        this.restoreSavedBilling();
        this.calculateTotals();
        this.loading = false;
        this.cdr.detectChanges();
      },
      error: (err) => {
        console.error('ERROR GLOBAL =', err);
        this.error = 'Erreur lors du chargement des données';
        this.loading = false;
        this.cdr.detectChanges();
      }
    });
  }

  updateLineAmount(line: BillingLine): void {
    line.nombreJours = Number(line.nombreJours ?? 0);
    line.montant = Number(line.tjm ?? 0) * Number(line.nombreJours ?? 0);
    this.calculateTotals();
  }

  calculateTotals(): void {
    this.totalGeneral = this.billingLines.reduce(
      (sum, line) => sum + Number(line.montant ?? 0),
      0
    );

    this.tvaAmount = this.totalGeneral * this.tvaRate;
    this.totalTTC = this.totalGeneral + this.tvaAmount;
  }

  get profileTotals(): { profile: string; total: number }[] {
    const totalsMap = new Map<string, number>();

    this.billingLines.forEach(line => {
      const profile = line.profileLibelle || 'Profil non défini';
      const current = totalsMap.get(profile) || 0;
      totalsMap.set(profile, current + Number(line.montant ?? 0));
    });

    return Array.from(totalsMap.entries()).map(([profile, total]) => ({
      profile,
      total
    }));
  }

  resetJours(): void {
    this.billingLines = this.billingLines.map(line => ({
      ...line,
      nombreJours: 0,
      montant: 0
    }));
    this.calculateTotals();
    this.clearMessage();
  }

  saveBilling(): void {
    const payload: SavedBillingData = {
      projectId: this.projectId,
      savedAt: new Date().toISOString(),
      billingLines: this.billingLines.map(line => ({
        ...line,
        nombreJours: Number(line.nombreJours ?? 0),
        montant: Number(line.montant ?? 0)
      })),
      totalGeneral: this.totalGeneral,
      tvaAmount: this.tvaAmount,
      totalTTC: this.totalTTC
    };

    localStorage.setItem(this.storageKey, JSON.stringify(payload));
    this.successMessage = 'Facture sauvegardée avec succès.';
    this.autoHideMessage();
  }

  restoreSavedBilling(): void {
    const raw = localStorage.getItem(this.storageKey);
    if (!raw) return;

    try {
      const saved: SavedBillingData = JSON.parse(raw);

      const savedMap = new Map(
        saved.billingLines.map(line => [String(line.id), line])
      );

      this.billingLines = this.billingLines.map(line => {
        const existing = savedMap.get(String(line.id));
        if (!existing) return line;

        const nombreJours = Number(existing.nombreJours ?? 0);
        return {
          ...line,
          nombreJours,
          montant: Number(line.tjm ?? 0) * nombreJours
        };
      });

      this.calculateTotals();
    } catch (e) {
      console.error('Erreur restauration facture sauvegardée', e);
    }
  }

  generatePdf(): void {
    const doc = new jsPDF('p', 'mm', 'a4');

    const projectName = this.project?.name || 'Projet';
    const description = this.project?.description || 'Aucune description disponible.';
    const client = this.project?.client || '-';
    const manager = (this.project as any)?.managerName || '-';
    const chef = (this.project as any)?.chefProjetName || '-';
    const teamName = this.team?.name || (this.project as any)?.teamName || 'Non affectée';
    const status = this.project?.status || 'N/A';
    const generatedAt = new Date().toLocaleString('fr-FR');

    doc.setFont('helvetica', 'bold');
    doc.setFontSize(18);
    doc.text('Facture du projet', 14, 18);

    doc.setFontSize(14);
    doc.text(projectName, 14, 28);

    doc.setFont('helvetica', 'normal');
    doc.setFontSize(10);
    doc.text(`Description : ${description}`, 14, 36);
    doc.text(`Client : ${client}`, 14, 43);
    doc.text(`Statut : ${status}`, 14, 50);
    doc.text(`Manager : ${manager}`, 14, 57);
    doc.text(`Chef de projet : ${chef}`, 14, 64);
    doc.text(`Équipe : ${teamName}`, 14, 71);
    doc.text(`Date de génération : ${generatedAt}`, 14, 78);

    const body = this.billingLines.map(line => [
      line.fullName,
      line.roleName,
      line.profileLibelle || '-',
      `${this.formatMoney(line.tjm)} MAD`,
      `${Number(line.nombreJours ?? 0)}`,
      `${this.formatMoney(line.montant)} MAD`
    ]);

    autoTable(doc, {
      startY: 88,
      head: [[
        'Ressource',
        'Rôle',
        'Profil',
        'TJM',
        'Nombre de jours',
        'Montant HT'
      ]],
      body: body.length ? body : [[
        'Aucune ressource', '-', '-', '0,00 MAD', '0', '0,00 MAD'
      ]],
      styles: {
        fontSize: 9
      },
      headStyles: {
        fillColor: [124, 58, 237]
      }
    });

    const finalY = (doc as any).lastAutoTable.finalY || 100;

    const profileRows = this.profileTotals.map(item => [
      `Total ${item.profile}`,
      `${this.formatMoney(item.total)} MAD`
    ]);

    autoTable(doc, {
      startY: finalY + 10,
      body: [
        ...profileRows,
        ['Total HT', `${this.formatMoney(this.totalGeneral)} MAD`],
        ['TVA (20%)', `${this.formatMoney(this.tvaAmount)} MAD`],
        ['Total TTC', `${this.formatMoney(this.totalTTC)} MAD`]
      ],
      theme: 'grid',
      styles: {
        fontSize: 10
      },
      columnStyles: {
        0: { fontStyle: 'bold' },
        1: { halign: 'right' }
      }
    });

    doc.save(this.buildPdfFileName());
  }

  buildPdfFileName(): string {
    const safeName = (this.project?.name || 'projet')
      .toLowerCase()
      .trim()
      .replace(/\s+/g, '-')
      .replace(/[^a-z0-9-]/g, '');

    return `facture-${safeName}.pdf`;
  }

  escapeHtml(value: string): string {
    return value
      .replace(/&/g, '&amp;')
      .replace(/</g, '&lt;')
      .replace(/>/g, '&gt;')
      .replace(/"/g, '&quot;')
      .replace(/'/g, '&#039;');
  }

  formatMoney(value: number): string {
    return Number(value || 0).toLocaleString('fr-FR', {
      minimumFractionDigits: 2,
      maximumFractionDigits: 2
    });
  }

  get storageKey(): string {
    return `billing_project_${this.projectId}`;
  }

  get memberLines(): BillingLine[] {
    return this.billingLines.filter(l => l.source === 'MEMBER');
  }

  get managerLines(): BillingLine[] {
    return this.billingLines.filter(l => l.source === 'MANAGER');
  }

  get chefLines(): BillingLine[] {
    return this.billingLines.filter(l => l.source === 'CHEF_PROJET');
  }

  goBack(): void {
    this.router.navigate(['/responsable-contrat/projets']);
  }

  clearMessage(): void {
    this.successMessage = null;
  }

  autoHideMessage(): void {
    setTimeout(() => {
      this.successMessage = null;
    }, 2500);
  }
}
import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { switchMap, retry, take, catchError } from 'rxjs/operators';
import { of, forkJoin } from 'rxjs';
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
  totalMembres: number;
  totalManager: number;
  totalChefProjet: number;
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

  totalMembres = 0;
  totalManager = 0;
  totalChefProjet = 0;
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
    this.totalMembres = this.billingLines
      .filter(l => l.source === 'MEMBER')
      .reduce((sum, l) => sum + Number(l.montant ?? 0), 0);

    this.totalManager = this.billingLines
      .filter(l => l.source === 'MANAGER')
      .reduce((sum, l) => sum + Number(l.montant ?? 0), 0);

    this.totalChefProjet = this.billingLines
      .filter(l => l.source === 'CHEF_PROJET')
      .reduce((sum, l) => sum + Number(l.montant ?? 0), 0);

    this.totalGeneral = this.totalMembres + this.totalManager + this.totalChefProjet; // HT
    this.tvaAmount = this.totalGeneral * this.tvaRate;
    this.totalTTC = this.totalGeneral + this.tvaAmount;
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
      totalMembres: this.totalMembres,
      totalManager: this.totalManager,
      totalChefProjet: this.totalChefProjet,
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

  printPage(): void {
    const printWindow = window.open('', '_blank', 'width=1200,height=800');

    if (!printWindow) {
      this.error = "Impossible d'ouvrir la fenêtre d'impression.";
      return;
    }

    const html = this.buildInvoiceHtml();

    printWindow.document.open();
    printWindow.document.write(html);
    printWindow.document.close();

    printWindow.focus();

    setTimeout(() => {
      printWindow.print();
    }, 500);
  }

  buildInvoiceHtml(): string {
    const projectName = this.escapeHtml(this.project?.name || 'Projet');
    const description = this.escapeHtml(this.project?.description || 'Aucune description disponible');
    const client = this.escapeHtml(this.project?.client || '-');
    const manager = this.escapeHtml((this.project as any)?.managerName || '-');
    const chef = this.escapeHtml((this.project as any)?.chefProjetName || '-');
    const teamName = this.escapeHtml(this.team?.name || (this.project as any)?.teamName || 'Non affectée');
    const status = this.escapeHtml(this.project?.status || 'N/A');
    const generatedAt = new Date().toLocaleString('fr-FR');

    const rows = this.billingLines.map(line => `
      <tr>
        <td>${this.escapeHtml(line.fullName)}</td>
        <td>${this.escapeHtml(line.roleName)}</td>
        <td>${this.escapeHtml(line.profileLibelle || '-')}</td>
        <td>${this.formatMoney(line.tjm)} MAD</td>
        <td>${Number(line.nombreJours ?? 0)}</td>
        <td>${this.formatMoney(line.montant)} MAD</td>
      </tr>
    `).join('');

    return `
<!DOCTYPE html>
<html lang="fr">
<head>
  <meta charset="UTF-8">
  <title>Facture - ${projectName}</title>
  <style>
    body {
      font-family: Arial, sans-serif;
      margin: 32px;
      color: #1f2937;
      background: #ffffff;
    }
    h1, h2, h3 { margin: 0 0 12px; }
    .top {
      margin-bottom: 24px;
      padding-bottom: 16px;
      border-bottom: 2px solid #7c3aed;
    }
    .meta {
      display: grid;
      grid-template-columns: repeat(2, 1fr);
      gap: 12px 24px;
      margin: 20px 0 28px;
    }
    .box {
      background: #f9fafb;
      border: 1px solid #e5e7eb;
      border-radius: 10px;
      padding: 12px 14px;
    }
    .label {
      font-size: 12px;
      color: #6b7280;
      margin-bottom: 4px;
      text-transform: uppercase;
    }
    .value {
      font-size: 14px;
      font-weight: 600;
      color: #111827;
    }
    table {
      width: 100%;
      border-collapse: collapse;
      margin-top: 16px;
    }
    th, td {
      border: 1px solid #e5e7eb;
      padding: 10px 12px;
      text-align: left;
      font-size: 14px;
    }
    th {
      background: #f3f4f6;
      text-transform: uppercase;
      font-size: 12px;
      color: #6b7280;
    }
    .summary {
      margin-top: 28px;
      width: 420px;
      margin-left: auto;
    }
    .summary-row {
      display: flex;
      justify-content: space-between;
      padding: 10px 12px;
      border: 1px solid #e5e7eb;
      border-bottom: none;
      background: #fff;
    }
    .summary-row:last-child {
      border-bottom: 1px solid #e5e7eb;
      background: #7c3aed;
      color: white;
      font-weight: bold;
    }
    .generated {
      margin-top: 28px;
      font-size: 12px;
      color: #6b7280;
      text-align: right;
    }
    @media print {
      body {
        margin: 16px;
      }
    }
  </style>
</head>
<body>
  <div class="top">
    <h1>Facture du projet</h1>
    <h2>${projectName}</h2>
    <p>${description}</p>
  </div>

  <div class="meta">
    <div class="box"><div class="label">Client</div><div class="value">${client}</div></div>
    <div class="box"><div class="label">Statut</div><div class="value">${status}</div></div>
    <div class="box"><div class="label">Manager</div><div class="value">${manager}</div></div>
    <div class="box"><div class="label">Chef de projet</div><div class="value">${chef}</div></div>
    <div class="box"><div class="label">Équipe</div><div class="value">${teamName}</div></div>
    <div class="box"><div class="label">Date de génération</div><div class="value">${generatedAt}</div></div>
  </div>

  <h3>Lignes de facturation</h3>

  <table>
    <thead>
      <tr>
        <th>Ressource</th>
        <th>Rôle</th>
        <th>Profil</th>
        <th>TJM</th>
        <th>Nombre de jours</th>
        <th>Montant</th>
      </tr>
    </thead>
    <tbody>
      ${rows || `<tr><td colspan="6">Aucune ligne de facturation</td></tr>`}
    </tbody>
  </table>

  <div class="summary">
    <div class="summary-row"><span>Total HT</span><span>${this.formatMoney(this.totalGeneral)} MAD</span></div>
    <div class="summary-row"><span>TVA (20%)</span><span>${this.formatMoney(this.tvaAmount)} MAD</span></div>
    <div class="summary-row"><span>Total TTC</span><span>${this.formatMoney(this.totalTTC)} MAD</span></div>
  </div>

  <div class="generated">Document généré le ${generatedAt}</div>
</body>
</html>
    `;
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
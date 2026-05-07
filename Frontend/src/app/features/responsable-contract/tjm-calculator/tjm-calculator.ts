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

  totalGeneral = 0;
  tvaRate = 0.2;
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

  getRoleLabel(role: string | undefined | null): string {
    switch ((role || '').toUpperCase()) {
      case 'MANAGER':
        return 'Manager';
      case 'CHEF_PROJET':
        return 'Chef de projet';
      case 'MEMBER':
        return 'Membre équipe';
      case 'MEMBRE_EQUIPE':
        return 'Membre équipe';
      case 'ADMIN':
        return 'Administrateur';
      case 'RESPONSABLE_CONTRACT':
        return 'Responsable contrat';
      default:
        return role || '-';
    }
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
            roleName: this.getRoleLabel('MANAGER'),
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
            roleName: this.getRoleLabel('CHEF_PROJET'),
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
            roleName: this.getRoleLabel(m.roleName),
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
    const pageWidth = doc.internal.pageSize.getWidth();
    const margin = 14;
    const contentWidth = pageWidth - margin * 2;

    const projectName = this.project?.name || 'Projet';
    const description = this.project?.description || 'Aucune description disponible.';
    const client = this.project?.client || '-';
    const manager = (this.project as any)?.managerName || '-';
    const chef = (this.project as any)?.chefProjetName || '-';
    const teamName = this.team?.name || (this.project as any)?.teamName || 'Non affectée';
    const status = this.getStatusLabel(this.project?.status);
    const generatedAt = new Date().toLocaleString('fr-FR');

    const MAUVE   = [124, 58, 237] as [number, number, number];
    const WHITE   = [255, 255, 255] as [number, number, number];
    const DARK    = [15, 23, 42]   as [number, number, number];
    const MUTED   = [100, 116, 139] as [number, number, number];
    const LIGHT   = [248, 250, 252] as [number, number, number];
    const BORDER  = [226, 232, 240] as [number, number, number];

    // ── EN-TÊTE ──────────────────────────────────────────────────────────────
    doc.setFillColor(...MAUVE);
    doc.rect(0, 0, pageWidth, 28, 'F');

    doc.setTextColor(...WHITE);
    doc.setFont('helvetica', 'bold');
    doc.setFontSize(16);
    doc.text('FACTURE PROJET', margin, 13);

    doc.setFont('helvetica', 'normal');
    doc.setFontSize(8);
    doc.text(`Générée le ${generatedAt}`, margin, 21);

    doc.setFont('helvetica', 'bold');
    doc.setFontSize(12);
    doc.text(projectName, pageWidth - margin, 13, { align: 'right' });

    doc.setFont('helvetica', 'normal');
    doc.setFontSize(8);
    doc.text(`Statut : ${status}`, pageWidth - margin, 21, { align: 'right' });

    // ── CARTE INFOS PROJET (2 colonnes) ───────────────────────────────────────
    const cardY = 34;
    const cardH = 48;
    const col1X = margin;
    const col2X = margin + contentWidth / 2 + 4;
    const colW  = contentWidth / 2 - 4;

    doc.setFillColor(...LIGHT);
    doc.setDrawColor(...BORDER);
    doc.setLineWidth(0.3);
    doc.roundedRect(margin, cardY, contentWidth, cardH, 3, 3, 'FD');

    doc.setDrawColor(...BORDER);
    doc.line(margin + contentWidth / 2, cardY + 6, margin + contentWidth / 2, cardY + cardH - 6);

    const leftItems = [
      ['Client', client],
      ['Manager', manager],
      ['Chef de projet', chef],
    ];

    leftItems.forEach(([label, value], i) => {
      const rowY = cardY + 12 + i * 12;
      doc.setFont('helvetica', 'normal');
      doc.setFontSize(7.5);
      doc.setTextColor(...MUTED);
      doc.text(label, col1X + 6, rowY);

      doc.setFont('helvetica', 'bold');
      doc.setFontSize(8.5);
      doc.setTextColor(...DARK);
      doc.text(value, col1X + 6, rowY + 5);
    });

    const rightItems = [
      ['Équipe', teamName],
      ['Description', description],
    ];

    rightItems.forEach(([label, value], i) => {
      const rowY = cardY + 12 + i * 18;
      doc.setFont('helvetica', 'normal');
      doc.setFontSize(7.5);
      doc.setTextColor(...MUTED);
      doc.text(label, col2X, rowY);

      doc.setFont('helvetica', 'bold');
      doc.setFontSize(8.5);
      doc.setTextColor(...DARK);
      const lines = doc.splitTextToSize(value, colW - 4);
      doc.text(lines.slice(0, 2), col2X, rowY + 5);
    });

    const tableY = cardY + cardH + 8;

    const body = this.billingLines.map(line => [
      line.fullName,
      this.getRoleLabel(line.roleName),
      line.profileLibelle || '-',
      `${this.formatMoney(line.tjm)} MAD`,
      `${Number(line.nombreJours ?? 0)}`,
      `${this.formatMoney(line.montant)} MAD`,
    ]);

    autoTable(doc, {
      startY: tableY,
      margin: { left: margin, right: margin },
      head: [['Ressource', 'Rôle', 'Profil', 'TJM', 'Jours', 'Montant HT']],
      body: body.length ? body : [['Aucune ressource', '-', '-', '0,00 MAD', '0', '0,00 MAD']],
      theme: 'striped',
      headStyles: {
        fillColor: MAUVE,
        textColor: WHITE,
        fontStyle: 'bold',
        fontSize: 8,
        halign: 'center',
      },
      styles: {
        fontSize: 8.5,
        textColor: DARK,
        cellPadding: 4,
      },
      columnStyles: {
        3: { halign: 'right' },
        4: { halign: 'center' },
        5: { halign: 'right', fontStyle: 'bold' },
      },
    });

    const finalY = (doc as any).lastAutoTable.finalY + 10;

    const totalsBody = [
      ...this.profileTotals.map(item => [`Total ${item.profile}`, `${this.formatMoney(item.total)} MAD`]),
      ['Total HT',  `${this.formatMoney(this.totalGeneral)} MAD`],
      ['TVA (20%)', `${this.formatMoney(this.tvaAmount)} MAD`],
      ['Total TTC', `${this.formatMoney(this.totalTTC)} MAD`],
    ];

    autoTable(doc, {
      startY: finalY,
      margin: { left: pageWidth / 2, right: margin },
      body: totalsBody,
      theme: 'plain',
      styles: {
        fontSize: 9,
        cellPadding: 3,
        textColor: DARK,
      },
      columnStyles: {
        0: { fontStyle: 'bold', textColor: MUTED },
        1: { halign: 'right', fontStyle: 'bold' },
      },
      didParseCell: (data: any) => {
        if (data.row.raw?.[0] === 'Total TTC') {
          data.cell.styles.fillColor = MAUVE;
          data.cell.styles.textColor = WHITE;
          data.cell.styles.fontStyle = 'bold';
          data.cell.styles.fontSize = 10;
        }
      },
    });

    const pageCount = doc.getNumberOfPages();
    for (let i = 1; i <= pageCount; i++) {
      doc.setPage(i);
      doc.setFontSize(7.5);
      doc.setTextColor(148, 163, 184);
      doc.text(
        `Page ${i}/${pageCount} — Document généré automatiquement`,
        pageWidth / 2, 289, { align: 'center' }
      );
    }

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
    return Number(value || 0)
      .toFixed(2)
      .replace('.', ',')
      .replace(/\B(?=(\d{3})+(?!\d))/g, ' ');
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

 getStatusLabel(status: string | undefined | null): string {
  switch ((status || '').toUpperCase()) {
    case 'EN_VALIDATION':
      return 'En cours de validation';  // ← Changé
    case 'PRE_VALIDE':
      return 'Pré-validé';
    case 'EN_COURS':
      return 'En cours de réalisation';  // ← Changé
    case 'VALIDE':
      return 'Validé';
    case 'REJETE':
    case 'REJETÉ':
      return 'Rejeté';
    case 'CLOTURE':
      return 'Clôturé';
    default:
      return status || '-';
  }
}
}
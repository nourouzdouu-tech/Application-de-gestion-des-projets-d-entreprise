package com.dxc.dxc_platform.controller;

import com.dxc.dxc_platform.dto.ReportingDataDto;
import com.dxc.dxc_platform.entity.User;
import com.dxc.dxc_platform.repository.UserRepository;
import com.dxc.dxc_platform.service.ExportPdfService;
import com.dxc.dxc_platform.service.ReportingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/reporting")
@CrossOrigin(origins = "http://localhost:4200")
public class ReportingController {

    private final ReportingService reportingService;
    private final UserRepository userRepository;
    private final ExportPdfService exportPdfService;  // ← AJOUTEZ CETTE LIGNE

    // Constructeur avec les 3 services
    public ReportingController(ReportingService reportingService,
                               UserRepository userRepository,
                               ExportPdfService exportPdfService) {  // ← AJOUTEZ CE PARAMÈTRE
        this.reportingService = reportingService;
        this.userRepository = userRepository;
        this.exportPdfService = exportPdfService;  // ← AJOUTEZ CETTE LIGNE
    }

    @GetMapping("/complete")
    public ResponseEntity<ReportingDataDto> getCompleteReporting() {
        UserDetails userDetails = (UserDetails) SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();

        String email = userDetails.getUsername();
        User currentUser = userRepository.findByEmailAndDeletedFalse(email)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

        return ResponseEntity.ok(reportingService.getCompleteReporting(currentUser));
    }

    @GetMapping("/export-pdf")
    public ResponseEntity<byte[]> exportAdminPdf() {
        UserDetails userDetails = (UserDetails) SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();
        String email = userDetails.getUsername();
        User currentUser = userRepository.findByEmailAndDeletedFalse(email)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

        ReportingDataDto data = reportingService.getCompleteReporting(currentUser);
        byte[] pdfBytes = exportPdfService.exportAdminReport(data);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment",
                "rapport_admin_" + LocalDate.now() + ".pdf");

        return ResponseEntity.ok()
                .headers(headers)
                .body(pdfBytes);
    }
    @GetMapping("/export-chef-pdf")
    public ResponseEntity<byte[]> exportChefProjetPdf() {
        UserDetails userDetails = (UserDetails) SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();
        String email = userDetails.getUsername();
        User currentUser = userRepository.findByEmailAndDeletedFalse(email)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouve"));

        ReportingDataDto data = reportingService.getCompleteReporting(currentUser);
        byte[] pdfBytes = exportPdfService.exportChefProjetReport(data);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment",
                "rapport_chef_projet_" + LocalDate.now() + ".pdf");

        return ResponseEntity.ok()
                .headers(headers)
                .body(pdfBytes);
    }
    @GetMapping("/export-manager-pdf")
    public ResponseEntity<byte[]> exportManagerPdf() {
        UserDetails userDetails = (UserDetails) SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();
        String email = userDetails.getUsername();
        User currentUser = userRepository.findByEmailAndDeletedFalse(email)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouve"));

        ReportingDataDto data = reportingService.getCompleteReporting(currentUser);
        byte[] pdfBytes = exportPdfService.exportManagerReport(data);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment",
                "rapport_manager_" + LocalDate.now() + ".pdf");

        return ResponseEntity.ok()
                .headers(headers)
                .body(pdfBytes);
    }
    @GetMapping("/export-membre-pdf")
    public ResponseEntity<byte[]> exportMembreEquipePdf() {
        UserDetails userDetails = (UserDetails) SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();
        String email = userDetails.getUsername();
        User currentUser = userRepository.findByEmailAndDeletedFalse(email)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouve"));

        ReportingDataDto data = reportingService.getCompleteReporting(currentUser);
        byte[] pdfBytes = exportPdfService.exportMembreEquipeReport(data);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment",
                "rapport_membre_equipe_" + LocalDate.now() + ".pdf");

        return ResponseEntity.ok()
                .headers(headers)
                .body(pdfBytes);
    }
    @GetMapping("/export-responsable-pdf")
    public ResponseEntity<byte[]> exportResponsableContratPdf() {
        UserDetails userDetails = (UserDetails) SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();
        String email = userDetails.getUsername();
        User currentUser = userRepository.findByEmailAndDeletedFalse(email)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouve"));

        ReportingDataDto data = reportingService.getCompleteReporting(currentUser);
        byte[] pdfBytes = exportPdfService.exportResponsableContratReport(data);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment",
                "rapport_responsable_contrat_" + LocalDate.now() + ".pdf");

        return ResponseEntity.ok()
                .headers(headers)
                .body(pdfBytes);
    }
}
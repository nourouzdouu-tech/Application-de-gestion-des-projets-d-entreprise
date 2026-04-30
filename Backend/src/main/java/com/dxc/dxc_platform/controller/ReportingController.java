package com.dxc.dxc_platform.controller;

import com.dxc.dxc_platform.dto.ReportingDataDto;
import com.dxc.dxc_platform.entity.User;
import com.dxc.dxc_platform.repository.UserRepository;
import com.dxc.dxc_platform.service.ReportingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/reporting")
@CrossOrigin(origins = "http://localhost:4200")
@RequiredArgsConstructor
public class ReportingController {

    private final ReportingService reportingService;
    private final UserRepository userRepository;

    @GetMapping("/complete")
    public ResponseEntity<ReportingDataDto> getCompleteReporting() {
        // Récupérer l'utilisateur connecté via Spring Security
        UserDetails userDetails = (UserDetails) SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();

        String email = userDetails.getUsername();
        User currentUser = userRepository.findByEmailAndDeletedFalse(email)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

        return ResponseEntity.ok(reportingService.getCompleteReporting(currentUser));
    }
}

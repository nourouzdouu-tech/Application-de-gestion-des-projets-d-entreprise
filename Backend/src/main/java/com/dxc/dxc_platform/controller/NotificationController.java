package com.dxc.dxc_platform.controller;

import com.dxc.dxc_platform.service.EmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
@CrossOrigin(origins = "http://localhost:4200")

public class NotificationController {

    private final EmailService emailService;
    public NotificationController(EmailService emailService) {
        this.emailService = emailService;
    }

    @PostMapping("/test")
    public ResponseEntity<?> testEmail(@RequestBody Map<String, String> request) {
        String to = request.get("email");
        emailService.sendSimpleEmail(to, "Test Notification", "Ceci est un test de notification par email.");
        return ResponseEntity.ok(Map.of("message", "Email envoyé avec succès à " + to));
    }
}
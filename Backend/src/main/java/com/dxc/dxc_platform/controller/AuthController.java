package com.dxc.dxc_platform.controller;
import com.dxc.dxc_platform.repository.UserRepository;
import com.dxc.dxc_platform.dto.AuthDto;
import com.dxc.dxc_platform.service.AuditService;
import com.dxc.dxc_platform.service.AuthService;
import com.dxc.dxc_platform.service.WebAuthnService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;


import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    private final AuthService authService;
    private final AuditService auditService;
    private final WebAuthnService webAuthnService;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final UserRepository userRepository;

    public AuthController(AuthService authService,
                          AuditService auditService,
                          WebAuthnService webAuthnService,
                          UserRepository userRepository
    ) {
        this.authService = authService;
        this.auditService = auditService;
        this.webAuthnService = webAuthnService;
        this.userRepository = userRepository;
    }

    // ── LOGIN CLASSIQUE ───────────────────────────────────────────

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody AuthDto.LoginRequest request) {
        try {
            AuthDto.Response response = authService.login(request);
            return ResponseEntity.ok(response);
        } catch (LockedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of(
                    "status", 403, "error", "ACCOUNT_LOCKED", "message", e.getMessage()));
        } catch (DisabledException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of(
                    "status", 403, "error", "ACCOUNT_DISABLED", "message", e.getMessage()));
        } catch (BadCredentialsException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                    "status", 401, "error", "UNAUTHORIZED", "message", e.getMessage()));
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        auditService.log("LOGOUT", "USER", null, "Déconnexion de " + email, email, null);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/change-password")
    public ResponseEntity<?> changePassword(
            @Valid @RequestBody AuthDto.ChangePasswordRequest request,
            Authentication authentication) {
        try {
            authService.changePassword(authentication.getName(), request);
            return ResponseEntity.ok(Map.of("message", "Mot de passe modifié avec succès."));
        } catch (BadCredentialsException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                    "status", 401, "error", "WRONG_PASSWORD", "message", e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                    "status", 400, "error", "INVALID_REQUEST", "message", e.getMessage()));
        }
    }

    @GetMapping("/me")
    public ResponseEntity<Map<String, Object>> me(Authentication authentication) {
        var user = userRepository.findByEmailIgnoreCaseAndDeletedFalse(
                authentication.getName()).orElseThrow();

        return ResponseEntity.ok(Map.of(
                "email", authentication.getName(),
                "roles", authentication.getAuthorities().stream()
                        .map(a -> a.getAuthority()).collect(Collectors.toList()),
                "prenom", user.getPrenom(),  // ← ajouter
                "nom", user.getNom(),         // ← ajouter
                "id", user.getId()            // ← ajouter
        ));
    }
    @PutMapping("/update-profile")
    public ResponseEntity<AuthDto.Response> updateProfile(
            @RequestBody AuthDto.UpdateProfileRequest request,
            Authentication authentication) {
        return ResponseEntity.ok(authService.updateProfile(authentication.getName(), request));
    }

    // ── WEBAUTHN — INSCRIPTION ────────────────────────────────────

    @PostMapping("/webauthn/register/options")
    public ResponseEntity<?> webAuthnRegisterOptions(@RequestBody Map<String, String> body) {
        try {
            return ResponseEntity.ok(webAuthnService.startRegistration(body.get("email")));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                    "status", 400, "error", "WEBAUTHN_ERROR", "message", e.getMessage()));
        }
    }

    @PostMapping("/webauthn/register/verify")
    public ResponseEntity<?> webAuthnRegisterVerify(@RequestBody Map<String, Object> body) {
        try {
            String email = (String) body.get("email");
            Object credentialRaw = body.get("credential");
            String credentialJson = credentialRaw instanceof String s
                    ? s
                    : objectMapper.writeValueAsString(credentialRaw);

            webAuthnService.finishRegistration(email, credentialJson);
            return ResponseEntity.ok(Map.of("message", "Biométrie enregistrée avec succès"));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.REQUEST_TIMEOUT).body(Map.of(
                    "status", 408, "error", "SESSION_EXPIRED", "message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                    "status", 400, "error", "REGISTRATION_FAILED", "message", e.getMessage()));
        }
    }

    // ── WEBAUTHN — CONNEXION ──────────────────────────────────────

    @PostMapping("/webauthn/login/options")
    public ResponseEntity<?> webAuthnLoginOptions(@RequestBody Map<String, String> body) {
        try {
            return ResponseEntity.ok(webAuthnService.startLogin(body.get("email")));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                    "status", 400, "error", "WEBAUTHN_ERROR", "message", e.getMessage()));
        }
    }

    @PostMapping("/webauthn/login/verify")
    public ResponseEntity<?> webAuthnLoginVerify(@RequestBody Map<String, Object> body) {
        try {
            String email = (String) body.get("email");
            Object assertionRaw = body.get("assertion");
            String assertionJson = assertionRaw instanceof String s
                    ? s
                    : objectMapper.writeValueAsString(assertionRaw);

            String token = webAuthnService.finishLogin(email, assertionJson);
            return ResponseEntity.ok(Map.of("token", token));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.REQUEST_TIMEOUT).body(Map.of(
                    "status", 408, "error", "SESSION_EXPIRED", "message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                    "status", 401, "error", "AUTHENTICATION_FAILED",
                    "message", "Échec de l'authentification biométrique"));
        }
    }

    @GetMapping("/webauthn/has-credential")
    public ResponseEntity<?> hasCredential(@RequestParam String email) {
        return ResponseEntity.ok(Map.of("registered", webAuthnService.hasCredential(email)));
    }
}
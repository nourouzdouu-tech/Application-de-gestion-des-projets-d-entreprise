package com.dxc.dxc_platform.modules.auth.web;

import com.dxc.dxc_platform.modules.auth.service.AuthService;
import com.dxc.dxc_platform.modules.auth.web.dto.AuthResponse;
import com.dxc.dxc_platform.modules.auth.web.dto.ChangePasswordRequest;
import com.dxc.dxc_platform.modules.auth.web.dto.LoginRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // POST /api/auth/login
    // ─────────────────────────────────────────────────────────────────────────
    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {
        try {
            AuthResponse response = authService.login(request);
            return ResponseEntity.ok(response);

        } catch (LockedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of(
                    "status", 403,
                    "error", "ACCOUNT_LOCKED",
                    "message", e.getMessage()
            ));
        } catch (DisabledException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of(
                    "status", 403,
                    "error", "ACCOUNT_DISABLED",
                    "message", e.getMessage()
            ));
        } catch (BadCredentialsException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                    "status", 401,
                    "error", "UNAUTHORIZED",
                    "message", e.getMessage()
            ));
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // POST /api/auth/logout
    // JWT est stateless → le logout côté serveur vide le contexte de sécurité
    // Le frontend doit supprimer le token de son côté (localStorage / cookie)
    // ─────────────────────────────────────────────────────────────────────────
    @PostMapping("/logout")
    public ResponseEntity<?> logout(Authentication authentication) {
        SecurityContextHolder.clearContext();
        return ResponseEntity.ok(Map.of(
                "message", "Déconnexion réussie. "
        ));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // POST /api/auth/change-password
    // ─────────────────────────────────────────────────────────────────────────
    @PostMapping("/change-password")
    public ResponseEntity<?> changePassword(
            @Valid @RequestBody ChangePasswordRequest request,
            Authentication authentication) {
        try {
            authService.changePassword(authentication.getName(), request);
            return ResponseEntity.ok(Map.of(
                    "message", "Mot de passe modifié avec succès."
            ));
        } catch (BadCredentialsException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                    "status", 401,
                    "error", "WRONG_PASSWORD",
                    "message", e.getMessage()
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                    "status", 400,
                    "error", "INVALID_REQUEST",
                    "message", e.getMessage()
            ));
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // GET /api/auth/me
    // ─────────────────────────────────────────────────────────────────────────
    @GetMapping("/me")
    public ResponseEntity<Map<String, Object>> me(Authentication authentication) {
        return ResponseEntity.ok(Map.of(
                "email", authentication.getName(),
                "roles", authentication.getAuthorities().stream()
                        .map(a -> a.getAuthority())
                        .toList()
        ));
    }
}
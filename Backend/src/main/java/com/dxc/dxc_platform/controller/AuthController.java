package com.dxc.dxc_platform.controller;

import com.dxc.dxc_platform.dto.AuthDto;
import com.dxc.dxc_platform.repository.UserRepository;
import com.dxc.dxc_platform.service.AuthService;
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

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody AuthDto.LoginRequest request) {
        try {
            AuthDto.Response response = authService.login(request);
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

    @PostMapping("/logout")
    public ResponseEntity<?> logout(Authentication authentication) {
        SecurityContextHolder.clearContext();
        return ResponseEntity.ok(Map.of(
                "message", "Déconnexion réussie."
        ));
    }

    @PostMapping("/change-password")
    public ResponseEntity<?> changePassword(
            @Valid @RequestBody AuthDto.ChangePasswordRequest request,
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

    @GetMapping("/me")
    public ResponseEntity<Map<String, Object>> me(Authentication authentication) {
        return ResponseEntity.ok(Map.of(
                "email", authentication.getName(),
                "roles", authentication.getAuthorities().stream()
                        .map(a -> a.getAuthority())
                        .toList()
        ));
    }
    @PutMapping("/update-profile")
    public ResponseEntity<AuthDto.Response> updateProfile(
            @RequestBody AuthDto.UpdateProfileRequest request,
            Authentication authentication) {

        AuthDto.Response response =
                authService.updateProfile(authentication.getName(), request);

        return ResponseEntity.ok(response);
    }

}
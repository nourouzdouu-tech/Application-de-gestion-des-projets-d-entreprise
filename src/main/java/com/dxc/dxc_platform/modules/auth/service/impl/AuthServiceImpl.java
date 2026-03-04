package com.dxc.dxc_platform.modules.auth.service.impl;

import com.dxc.dxc_platform.modules.admin.domain.entity.Role;
import com.dxc.dxc_platform.modules.admin.domain.entity.User;
import com.dxc.dxc_platform.modules.admin.repository.UserRepository;
import com.dxc.dxc_platform.modules.auth.service.AuthService;
import com.dxc.dxc_platform.modules.auth.web.dto.AuthResponse;
import com.dxc.dxc_platform.modules.auth.web.dto.ChangePasswordRequest;
import com.dxc.dxc_platform.modules.auth.web.dto.LoginRequest;
import com.dxc.dxc_platform.security.jwt.JwtService;
import com.dxc.dxc_platform.shared.exception.NotFoundException;
import com.dxc.dxc_platform.shared.util.RoleNormalizer;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.stream.Collectors;

@Service
public class AuthServiceImpl implements AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;
    private final PasswordEncoder passwordEncoder;
    private final LoginAttemptService loginAttemptService;

    public AuthServiceImpl(AuthenticationManager authenticationManager,
                           UserRepository userRepository,
                           JwtService jwtService,
                           UserDetailsService userDetailsService,
                           PasswordEncoder passwordEncoder,
                           LoginAttemptService loginAttemptService) {
        this.authenticationManager = authenticationManager;
        this.userRepository = userRepository;
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
        this.passwordEncoder = passwordEncoder;
        this.loginAttemptService = loginAttemptService;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // LOGIN
    // ─────────────────────────────────────────────────────────────────────────
    @Override
    public AuthResponse login(LoginRequest request) {

        // 1 ─ Charger le user
        User user = userRepository.findByEmailAndDeletedFalse(request.email())
                .orElseThrow(() -> new NotFoundException(
                        "USER_NOT_FOUND", "Utilisateur introuvable"));

        // 2 ─ Vérifier verrouillage / désactivation
        if (user.isLocked()) {
            throw new LockedException(
                    "Compte verrouillé après 3 tentatives. Contactez l'administrateur.");
        }
        if (!user.isEnabled()) {
            throw new DisabledException("Compte désactivé. Contactez l'administrateur.");
        }

        // 3 ─ Tenter l'authentification
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.email(),
                            request.password()
                    )
            );
        } catch (BadCredentialsException e) {
            // Incrémenter dans une transaction SÉPARÉE → committé immédiatement
            // registerFailedAttempt lance LockedException si MAX atteint
            loginAttemptService.registerFailedAttempt(request.email());

            // Si on arrive ici → pas encore verrouillé
            int remaining = loginAttemptService.getRemainingAttempts(request.email());
            throw new BadCredentialsException(
                    "Mot de passe incorrect. " + remaining
                            + " tentative(s) restante(s) avant verrouillage.");
        }

        // 4 ─ Succès → reset tentatives dans transaction séparée
        loginAttemptService.resetAttempts(request.email());

        // 5 ─ Générer JWT
        UserDetails userDetails = userDetailsService.loadUserByUsername(request.email());
        String token = jwtService.generateToken(userDetails);

        // 6 ─ Rôles depuis la DB
        Set<String> roles = user.getRoles().stream()
                .map(Role::getNom)
                .collect(Collectors.toSet());

        // 7 ─ Redirection dynamique depuis la DB
        String redirectTo = buildRedirectFromDb(user);

        return new AuthResponse(
                token,
                "Bearer",
                user.getEmail(),
                user.getPrenom(),
                user.getNom(),
                roles,
                redirectTo,
                user.isMustChangePassword()
        );
    }

    // ─────────────────────────────────────────────────────────────────────────
    // CHANGER MOT DE PASSE
    // ─────────────────────────────────────────────────────────────────────────
    @Override
    @Transactional
    public void changePassword(String email, ChangePasswordRequest request) {

        User user = userRepository.findByEmailAndDeletedFalse(email)
                .orElseThrow(() -> new NotFoundException(
                        "USER_NOT_FOUND", "Utilisateur introuvable"));

        if (!passwordEncoder.matches(request.oldPassword(), user.getPasswordHash())) {
            throw new BadCredentialsException("Ancien mot de passe incorrect.");
        }

        if (passwordEncoder.matches(request.newPassword(), user.getPasswordHash())) {
            throw new IllegalArgumentException(
                    "Le nouveau mot de passe doit être différent de l'ancien.");
        }

        if (!request.newPassword().equals(request.confirmPassword())) {
            throw new IllegalArgumentException(
                    "La confirmation ne correspond pas au nouveau mot de passe.");
        }

        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        user.setMustChangePassword(false);
        userRepository.saveAndFlush(user);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // REDIRECTION DEPUIS LA DB
    // ─────────────────────────────────────────────────────────────────────────
    private String buildRedirectFromDb(User user) {
        return user.getRoles().stream()
                .findFirst()
                .map(role -> "/" + RoleNormalizer.normalize(role.getNom())
                        .toLowerCase().replace("_", "-"))
                .orElse("/dashboard");
    }
}
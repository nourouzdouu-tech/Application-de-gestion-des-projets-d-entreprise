package com.dxc.dxc_platform.service.impl;

import com.dxc.dxc_platform.dto.AuthDto;
import com.dxc.dxc_platform.entity.User;
import com.dxc.dxc_platform.mapper.AuthMapper;
import com.dxc.dxc_platform.repository.UserRepository;
import com.dxc.dxc_platform.security.jwt.JwtService;
import com.dxc.dxc_platform.service.AuthService;
import com.dxc.dxc_platform.shared.exception.NotFoundException;
import com.dxc.dxc_platform.shared.util.RoleNormalizer;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.dxc.dxc_platform.service.AuditService;

import java.util.stream.Collectors;

@Service
public class AuthServiceImpl implements AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;
    private final PasswordEncoder passwordEncoder;
    private final LoginAttemptService loginAttemptService;
    private final AuthMapper authMapper;
    private final AuditService auditService;

    public AuthServiceImpl(AuthenticationManager authenticationManager,
                           UserRepository userRepository,
                           JwtService jwtService,
                           UserDetailsService userDetailsService,
                           PasswordEncoder passwordEncoder,
                           LoginAttemptService loginAttemptService,
                           AuthMapper authMapper, AuditService auditService) {
        this.authenticationManager = authenticationManager;
        this.userRepository = userRepository;
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
        this.passwordEncoder = passwordEncoder;
        this.loginAttemptService = loginAttemptService;
        this.authMapper = authMapper;
        this.auditService = auditService;
    }

    @Override
    public AuthDto.Response login(AuthDto.LoginRequest request) {
        String email = request.email();
        String ipAddress = getClientIp(); // À implémenter

        try {
            User user = userRepository.findByEmailAndDeletedFalse(email)
                    .orElseThrow(() -> new NotFoundException(
                            "USER_NOT_FOUND", "Utilisateur introuvable"));

            if (user.isLocked()) {
                // ✅ Audit pour compte verrouillé
                auditService.log("ACCOUNT_LOCKED", "USER", user.getId(),
                        "Tentative de connexion sur compte verrouillé: " + email,
                        email, ipAddress);
                throw new LockedException(
                        "Compte verrouillé après 3 tentatives. Contactez l'administrateur.");
            }

            try {
                authenticationManager.authenticate(
                        new UsernamePasswordAuthenticationToken(email, request.password())
                );
            } catch (BadCredentialsException e) {
                int remaining = loginAttemptService.registerFailedAttempt(email);

                // ✅ Audit pour échec de connexion
                auditService.log("LOGIN_FAILED", "AUTH", user.getId(),
                        "Échec de connexion pour " + email + ". Mot de passe incorrect. Tentatives restantes: " + remaining,
                        email, ipAddress);

                throw new BadCredentialsException(
                        "Mot de passe incorrect. " + remaining
                                + " tentative(s) restante(s) avant verrouillage.");
            }

            loginAttemptService.resetAttempts(email);

            UserDetails userDetails = userDetailsService.loadUserByUsername(email);
            String token = jwtService.generateToken(userDetails);

            String redirectTo = buildRedirectFromDb(user);

            var roles = user.getRoles().stream()
                    .map(role -> role.getNom())
                    .collect(Collectors.toSet());

            // ✅ Audit pour connexion réussie
            auditService.log("LOGIN_SUCCESS", "AUTH", user.getId(),
                    "Connexion réussie pour " + email ,
                    email, ipAddress);

            AuthDto.Response finalResponse = new AuthDto.Response(
                    token,
                    "Bearer",
                    user.getId(),
                    user.getEmail(),
                    user.getPrenom(),
                    user.getNom(),
                    roles,
                    redirectTo,
                    user.isMustChangePassword()
            );

            return finalResponse;

        } catch (NotFoundException e) {
            // ✅ Audit pour utilisateur inexistant
            auditService.log("LOGIN_FAILED", "AUTH", null,
                    "Tentative de connexion avec email inexistant: " + email,
                    email, ipAddress);
            throw e;
        }
    }

    // ✅ Méthode pour récupérer l'IP du client
    private String getClientIp() {
        try {
            jakarta.servlet.http.HttpServletRequest request =
                    ((org.springframework.web.context.request.ServletRequestAttributes)
                            org.springframework.web.context.request.RequestContextHolder.currentRequestAttributes())
                            .getRequest();
            String ip = request.getHeader("X-Forwarded-For");
            if (ip == null || ip.isEmpty()) {
                ip = request.getRemoteAddr();
            }
            return ip;
        } catch (Exception e) {
            return "unknown";
        }
    }

    @Override
    @Transactional
    public void changePassword(String email, AuthDto.ChangePasswordRequest request) {
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

        // ✅ Audit pour changement de mot de passe
        auditService.log("RESET_PASSWORD", "USER", user.getId(),
                "Changement de mot de passe pour " + email,
                email, getClientIp());
    }

    private String buildRedirectFromDb(User user) {
        return user.getRoles().stream()
                .findFirst()
                .map(role -> "/" + RoleNormalizer.normalize(role.getNom())
                        .toLowerCase().replace("_", "-"))
                .orElse("/dashboard");
    }

    @Override
    @Transactional
    public AuthDto.Response updateProfile(String email, AuthDto.UpdateProfileRequest request) {
        User user = userRepository.findByEmailAndDeletedFalse(email)
                .orElseThrow(() -> new NotFoundException(
                        "USER_NOT_FOUND", "Utilisateur introuvable"));

        if (!user.getEmail().equalsIgnoreCase(request.email())
                && userRepository.existsByEmailAndDeletedFalse(request.email())) {
            throw new RuntimeException("Email déjà utilisé");
        }

        user.setNom(request.nom());
        user.setPrenom(request.prenom());
        user.setEmail(request.email());

        userRepository.save(user);

        // ✅ Audit pour mise à jour du profil
        auditService.log("UPDATE_USER", "USER", user.getId(),
                "Mise à jour du profil pour " + email + " (nouvel email: " + request.email() + ")",
                email, getClientIp());

        return new AuthDto.Response(
                null,
                null,
                user.getId(),
                user.getEmail(),
                user.getPrenom(),
                user.getNom(),
                user.getRoles().stream().map(r -> r.getNom()).collect(Collectors.toSet()),
                null,
                user.isMustChangePassword()
        );
    }
}
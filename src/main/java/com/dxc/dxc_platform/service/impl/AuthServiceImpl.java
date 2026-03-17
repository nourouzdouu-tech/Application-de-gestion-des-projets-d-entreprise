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

@Service
public class AuthServiceImpl implements AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;
    private final PasswordEncoder passwordEncoder;
    private final LoginAttemptService loginAttemptService;
    private final AuthMapper authMapper;

    public AuthServiceImpl(AuthenticationManager authenticationManager,
                           UserRepository userRepository,
                           JwtService jwtService,
                           UserDetailsService userDetailsService,
                           PasswordEncoder passwordEncoder,
                           LoginAttemptService loginAttemptService,
                           AuthMapper authMapper) {
        this.authenticationManager = authenticationManager;
        this.userRepository = userRepository;
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
        this.passwordEncoder = passwordEncoder;
        this.loginAttemptService = loginAttemptService;
        this.authMapper = authMapper;
    }

    @Override
    public AuthDto.Response login(AuthDto.LoginRequest request) {

        User user = userRepository.findByEmailAndDeletedFalse(request.email())
                .orElseThrow(() -> new NotFoundException(
                        "USER_NOT_FOUND", "Utilisateur introuvable"));

        if (user.isLocked()) {
            throw new LockedException(
                    "Compte verrouillé après 3 tentatives. Contactez l'administrateur.");
        }

        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.email(),
                            request.password()
                    )
            );
        } catch (BadCredentialsException e) {
            int remaining = loginAttemptService.registerFailedAttempt(request.email());
            throw new BadCredentialsException(
                    "Mot de passe incorrect. " + remaining
                            + " tentative(s) restante(s) avant verrouillage.");
        }

        loginAttemptService.resetAttempts(request.email());

        UserDetails userDetails = userDetailsService.loadUserByUsername(request.email());
        String token = jwtService.generateToken(userDetails);

        String redirectTo = buildRedirectFromDb(user);

        AuthDto.Response base = authMapper.toResponse(user);

        return new AuthDto.Response(
                token,
                base.tokenType(),
                base.email(),
                base.prenom(),
                base.nom(),
                base.roles(),
                redirectTo,
                base.mustChangePassword()
        );
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
    }

    private String buildRedirectFromDb(User user) {
        return user.getRoles().stream()
                .findFirst()
                .map(role -> "/" + RoleNormalizer.normalize(role.getNom())
                        .toLowerCase().replace("_", "-"))
                .orElse("/dashboard");
    }
}
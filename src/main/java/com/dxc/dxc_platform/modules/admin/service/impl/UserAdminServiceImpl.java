package com.dxc.dxc_platform.modules.admin.service.impl;

import com.dxc.dxc_platform.modules.admin.domain.entity.Role;
import com.dxc.dxc_platform.modules.admin.domain.entity.User;
import com.dxc.dxc_platform.modules.admin.repository.RoleRepository;
import com.dxc.dxc_platform.modules.admin.repository.UserRepository;
import com.dxc.dxc_platform.modules.admin.service.UserAdminService;
import com.dxc.dxc_platform.modules.admin.web.dto.user.*;
import com.dxc.dxc_platform.shared.exception.ConflictException;
import com.dxc.dxc_platform.shared.exception.NotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
public class UserAdminServiceImpl implements UserAdminService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    public UserAdminServiceImpl(UserRepository userRepository,
                                RoleRepository roleRepository,
                                PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public UserResponse create(CreateUserRequest req) {
        if (userRepository.existsByEmailAndDeletedFalse(req.email())) {
            throw new ConflictException("EMAIL_ALREADY_USED", "Email déjà utilisé");
        }

        Role role = roleRepository.findByNomAndDeletedFalse(req.roleCode())
                .orElseThrow(() -> new NotFoundException("ROLE_NOT_FOUND", "Rôle introuvable: " + req.roleCode()));

        User user = new User(
                req.email(),
                req.prenom(),
                req.nom(),
                req.genre(),
                passwordEncoder.encode(req.password())
        );

        user.setEnabled(true);
        user.setFailedAttempts(0);
        user.setLocked(false);
        user.setMustChangePassword(false);
        user.setDeleted(false);
        user.setRoles(Set.of(role));

        user = userRepository.save(user);
        return toResponse(user);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<UserResponse> search(String q, String role, Boolean enabled, Pageable pageable) {

        String qLike = "%";
        if (q != null && !q.isBlank()) {
            qLike = "%" + q.toLowerCase() + "%";
        }

        String roleLower = "";
        if (role != null && !role.isBlank()) {
            roleLower = role.toLowerCase();
        }

        return userRepository.search(qLike, enabled, roleLower, pageable)
                .map(this::toResponse);
    }
    @Override
    @Transactional(readOnly = true)
    public UserResponse getById(Long id) {
        User user = userRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new NotFoundException("USER_NOT_FOUND", "Utilisateur introuvable: " + id));
        return toResponse(user);
    }

    @Override
    public UserResponse update(Long id, UpdateUserRequest req) {
        User user = userRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new NotFoundException("USER_NOT_FOUND", "Utilisateur introuvable: " + id));

        if (!user.getEmail().equalsIgnoreCase(req.email())
                && userRepository.existsByEmailAndDeletedFalse(req.email())) {
            throw new ConflictException("EMAIL_ALREADY_USED", "Email déjà utilisé");
        }

        Role role = roleRepository.findByNomAndDeletedFalse(req.roleCode())
                .orElseThrow(() -> new NotFoundException("ROLE_NOT_FOUND", "Rôle introuvable: " + req.roleCode()));

        user.setPrenom(req.prenom());
        user.setNom(req.nom());
        user.setEmail(req.email());
        user.setGenre(req.genre());
        user.setRoles(Set.of(role));

        return toResponse(user);
    }

    @Override
    public void disable(Long id) {
        User user = userRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new NotFoundException("USER_NOT_FOUND", "Utilisateur introuvable: " + id));
        user.setEnabled(false);
    }

    @Override
    public void enable(Long id) {
        User user = userRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new NotFoundException("USER_NOT_FOUND", "Utilisateur introuvable: " + id));
        user.setEnabled(true);
        user.setFailedAttempts(0);
        user.setLocked(false);
    }

    @Override
    public ResetPasswordResponse resetPassword(Long id, ResetPasswordRequest req) {
        User user = userRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new NotFoundException("USER_NOT_FOUND", "Utilisateur introuvable: " + id));

        String tempPassword =
                (req != null && req.tempPassword() != null && !req.tempPassword().isBlank())
                        ? req.tempPassword()
                        : generateTempPassword();

        user.setPasswordHash(passwordEncoder.encode(tempPassword));
        user.setEnabled(true);
        user.setFailedAttempts(0);
        user.setLocked(false);
        user.setMustChangePassword(true);

        return new ResetPasswordResponse(user.getId(), tempPassword, true);
    }

    @Override
    public void softDelete(Long id) {
        User user = userRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new NotFoundException("USER_NOT_FOUND", "Utilisateur introuvable: " + id));
        user.setDeleted(true);
        user.setEnabled(false);
    }

    private UserResponse toResponse(User u) {
        Set<String> roleNames = u.getRoles().stream()
                .map(Role::getNom)
                .collect(Collectors.toSet());

        return new UserResponse(
                u.getId(),
                u.getEmail(),
                u.getPrenom(),
                u.getNom(),
                u.getGenre(),
                u.isEnabled(),
                u.getFailedAttempts(),
                u.isMustChangePassword(),
                roleNames
        );
    }

    private String generateTempPassword() {
        return "Temp@" + UUID.randomUUID().toString().substring(0, 8);
    }
}
package com.dxc.dxc_platform.service.impl;

import com.dxc.dxc_platform.dto.ManagerSelectDto;
import com.dxc.dxc_platform.dto.UserDto;
import com.dxc.dxc_platform.entity.Profile;
import com.dxc.dxc_platform.entity.Role;
import com.dxc.dxc_platform.entity.User;
import com.dxc.dxc_platform.mapper.UserMapper;
import com.dxc.dxc_platform.repository.ProfileRepository;
import com.dxc.dxc_platform.repository.RoleRepository;
import com.dxc.dxc_platform.repository.UserRepository;
import com.dxc.dxc_platform.service.AuditService;
import com.dxc.dxc_platform.service.UserAdminService;
import com.dxc.dxc_platform.shared.exception.ConflictException;
import com.dxc.dxc_platform.shared.exception.NotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
public class UserAdminServiceImpl implements UserAdminService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final ProfileRepository profileRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;
    private final AuditService auditService;

    public UserAdminServiceImpl(UserRepository userRepository,
                                RoleRepository roleRepository,
                                ProfileRepository profileRepository,
                                PasswordEncoder passwordEncoder,
                                UserMapper userMapper,
                                AuditService auditService) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.profileRepository = profileRepository;
        this.passwordEncoder = passwordEncoder;
        this.userMapper = userMapper;
        this.auditService = auditService;
    }

    private String getCurrentUserEmail() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }

    @Override
    public UserDto.Response create(UserDto.CreateRequest req) {
        if (userRepository.existsByEmailAndDeletedFalse(req.email())) {
            throw new ConflictException("EMAIL_ALREADY_USED", "Email déjà utilisé");
        }

        Set<Role> roles = new HashSet<>();
        for (String code : req.roleCodes()) {
            Role role = roleRepository.findByNom(code)
                    .orElseThrow(() -> new NotFoundException("ROLE_NOT_FOUND", "Rôle introuvable: " + code));
            roles.add(role);
        }

        Profile profile = profileRepository.findByIdAndDeletedFalse(req.profileId())
                .orElseThrow(() -> new NotFoundException("PROFILE_NOT_FOUND", "Profil introuvable: " + req.profileId()));

        User user = new User(
                req.email(),
                req.prenom(),
                req.nom(),
                req.genre(),
                passwordEncoder.encode(req.password())
        );

        user.setFailedAttempts(0);
        user.setLocked(false);
        user.setMustChangePassword(false);
        user.setDeleted(false);
        user.setRoles(roles);
        user.setProfile(profile);

        User saved = userRepository.save(user);

        // Audit log
        auditService.log("CREATE_USER", "USER", saved.getId(),
                "Création de l'utilisateur " + saved.getEmail(),
                getCurrentUserEmail(),  null);
        return userMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<UserDto.Response> search(String q, String role, Boolean locked, Pageable pageable) {
        String qLike = "%";
        if (q != null && !q.isBlank()) {
            qLike = "%" + q.toLowerCase() + "%";
        }

        String roleLower = "";
        if (role != null && !role.isBlank()) {
            roleLower = role.toLowerCase();
        }

        return userRepository.search(qLike, locked, roleLower, pageable)
                .map(userMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public UserDto.Response getById(Long id) {
        User user = userRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new NotFoundException("USER_NOT_FOUND", "Utilisateur introuvable: " + id));
        return userMapper.toResponse(user);
    }

    @Override
    public UserDto.Response update(Long id, UserDto.UpdateRequest req) {
        User user = userRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new NotFoundException("USER_NOT_FOUND", "Utilisateur introuvable: " + id));

        if (!user.getEmail().equalsIgnoreCase(req.email())
                && userRepository.existsByEmailAndDeletedFalse(req.email())) {
            throw new ConflictException("EMAIL_ALREADY_USED", "Email déjà utilisé");
        }

        Set<Role> roles = new HashSet<>();
        for (String code : req.roleCodes()) {
            Role role = roleRepository.findByNom(code)
                    .orElseThrow(() -> new NotFoundException("ROLE_NOT_FOUND", "Rôle introuvable: " + code));
            roles.add(role);
        }

        Profile profile = profileRepository.findByIdAndDeletedFalse(req.profileId())
                .orElseThrow(() -> new NotFoundException("PROFILE_NOT_FOUND", "Profil introuvable: " + req.profileId()));

        String oldEmail = user.getEmail();
        user.setPrenom(req.prenom());
        user.setNom(req.nom());
        user.setEmail(req.email());
        user.setGenre(req.genre());
        user.setRoles(roles);
        user.setProfile(profile);

        userRepository.save(user);

        // Audit log
        auditService.log("UPDATE_USER", "USER", id,
                "Modification de l'utilisateur " + oldEmail + " → " + req.email(),
                getCurrentUserEmail(),  null);

        return userMapper.toResponse(user);
    }

    @Override
    public void disable(Long id) {
        User user = userRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new NotFoundException("USER_NOT_FOUND", "Utilisateur introuvable: " + id));
        user.setLocked(true);
        userRepository.save(user);

        auditService.log("DISABLE_USER", "USER", id,
                "Désactivation de l'utilisateur " + user.getEmail(),
                getCurrentUserEmail(),  null);
    }

    @Override
    public void enable(Long id) {
        User user = userRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new NotFoundException("USER_NOT_FOUND", "Utilisateur introuvable: " + id));
        user.setFailedAttempts(0);
        user.setLocked(false);
        userRepository.save(user);

        auditService.log("ACCOUNT_UNLOCKED", "USER", id,
                "Compte déverrouillé de l'utilisateur " + user.getEmail(),
                getCurrentUserEmail(), null);
    }

    @Override
    public UserDto.ResetPasswordResponse resetPassword(Long id, UserDto.ResetPasswordRequest req) {
        User user = userRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new NotFoundException("USER_NOT_FOUND", "Utilisateur introuvable: " + id));

        String tempPassword =
                (req != null && req.tempPassword() != null && !req.tempPassword().isBlank())
                        ? req.tempPassword()
                        : generateTempPassword();

        user.setPasswordHash(passwordEncoder.encode(tempPassword));
        user.setFailedAttempts(0);
        user.setLocked(false);
        user.setMustChangePassword(true);
        userRepository.save(user);

        auditService.log("RESET_PASSWORD", "USER", id,
                "Réinitialisation du mot de passe pour " + user.getEmail(),
                getCurrentUserEmail(), null);
        return null;
    }

    @Override
    public void softDelete(Long id) {
        User user = userRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new NotFoundException("USER_NOT_FOUND", "Utilisateur introuvable: " + id));
        user.setDeleted(true);
        user.setLocked(true);
        userRepository.save(user);

        auditService.log("DELETE_USER", "USER", id,
                "Suppression de l'utilisateur " + user.getEmail(),
                getCurrentUserEmail(), null);
    }

    private String generateTempPassword() {
        return "Temp@" + UUID.randomUUID().toString().substring(0, 8);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ManagerSelectDto> getManagersForSelect() {
        return userRepository.findAllActiveManagers()
                .stream()
                .map(user -> new ManagerSelectDto(
                        user.getId(),
                        ((user.getPrenom() != null ? user.getPrenom() : "") + " " +
                                (user.getNom() != null ? user.getNom() : "")).trim(),
                        user.getEmail()
                ))
                .collect(Collectors.toList());
    }
}
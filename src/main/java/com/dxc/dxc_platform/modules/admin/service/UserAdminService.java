package com.dxc.dxc_platform.modules.admin.service;

import com.dxc.dxc_platform.modules.admin.domain.entity.Role;
import com.dxc.dxc_platform.modules.admin.domain.entity.User;
import com.dxc.dxc_platform.modules.admin.repository.RoleRepository;
import com.dxc.dxc_platform.modules.admin.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class UserAdminService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    public User createUser(String nom, String prenom, String email, String password, String genre, Set<Long> roleIds) {

        if (userRepository.existsByEmail(email)) {
            throw new RuntimeException("Email already exists");
        }

        Set<Role> roles = new HashSet<>(roleRepository.findAllById(roleIds));

        User user = User.builder()
                .nom(nom)
                .prenom(prenom)
                .email(email)
                .passwordHash(passwordEncoder.encode(password))
                .genre(Enum.valueOf(com.dxc.dxc_platform.modules.admin.domain.enums.Genre.class, genre))
                .roles(roles)
                .createdAt(LocalDateTime.now())
                .enabled(true)
                .locked(false)
                .failedAttempts(0)
                .mustChangePassword(false)
                .build();

        return userRepository.save(user);
    }

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    public User getUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    public void disableUser(Long id) {
        User user = getUserById(id);
        user.setEnabled(false);
        userRepository.save(user);
    }

    public void unlockUser(Long id, String newPassword) {
        User user = getUserById(id);
        user.setEnabled(true);
        user.setLocked(false);
        user.setFailedAttempts(0);
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }
}
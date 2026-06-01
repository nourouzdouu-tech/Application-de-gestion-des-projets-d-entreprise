package com.dxc.dxc_platform.security;

import com.dxc.dxc_platform.entity.Permission;
import com.dxc.dxc_platform.entity.Role;
import com.dxc.dxc_platform.entity.User;
import com.dxc.dxc_platform.repository.UserRepository;
import com.dxc.dxc_platform.shared.util.RoleNormalizer;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmailIgnoreCaseAndDeletedFalse(email)
                .orElseThrow(() -> new UsernameNotFoundException(
                        "Utilisateur introuvable: " + email));

        // Ajoutez des logs pour vérifier l'état de l'utilisateur
        System.out.println("=== Loading user: " + email);
        System.out.println("=== User locked: " + user.isLocked());
        System.out.println("=== User deleted: " + user.isDeleted());

        List<GrantedAuthority> authorities = new ArrayList<>();
        boolean hasAdminRole = false;

        for (Role role : user.getRoles()) {
            String roleName = RoleNormalizer.toSpringRole(role.getNom());
            System.out.println("=== Adding role authority: " + roleName);
            authorities.add(new SimpleGrantedAuthority(roleName));

            // Vérifier si c'est le rôle ADMIN
            if ("ROLE_ADMIN".equals(roleName)) {
                hasAdminRole = true;
            }

            for (Permission permission : role.getPermissions()) {
                System.out.println("=== Adding permission authority: " + permission.getNom());
                authorities.add(new SimpleGrantedAuthority(permission.getNom()));
            }
        }

        // Ajouter explicitement l'autorité "ADMIN" si l'utilisateur a ROLE_ADMIN
        if (hasAdminRole) {
            authorities.add(new SimpleGrantedAuthority("ADMIN"));
            System.out.println("=== Added ADMIN authority explicitly");
        }

        System.out.println("=== Final authorities: " + authorities);

        // Vérifier si l'utilisateur est bloqué
        boolean isLocked = user.isLocked();
        boolean isDeleted = user.isDeleted();

        return org.springframework.security.core.userdetails.User.builder()
                .username(user.getEmail())
                .password(user.getPasswordHash())
                .authorities(authorities)
                .accountExpired(false)
                .accountLocked(isLocked)      // ← Utilisez la valeur réelle de la base de données
                .credentialsExpired(false)
                .disabled(isDeleted)          // ← Utilisez la valeur réelle de la base de données
                .build();
    }
}
package com.dxc.dxc_platform.security;

import com.dxc.dxc_platform.modules.admin.domain.entity.Permission;
import com.dxc.dxc_platform.modules.admin.domain.entity.Role;
import com.dxc.dxc_platform.modules.admin.domain.entity.User;
import com.dxc.dxc_platform.modules.admin.repository.UserRepository;
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
        User user = userRepository.findByEmailAndDeletedFalse(email)
                .orElseThrow(() -> new UsernameNotFoundException(
                        "Utilisateur introuvable: " + email));

        List<GrantedAuthority> authorities = new ArrayList<>();

        for (Role role : user.getRoles()) {
            authorities.add(new SimpleGrantedAuthority(
                    RoleNormalizer.toSpringRole(role.getNom())));

            for (Permission permission : role.getPermissions()) {
                authorities.add(new SimpleGrantedAuthority(permission.getNom()));
            }
        }

        // ⚠️ accountLocked et disabled toujours FALSE ici
        // La vérification du verrouillage est gérée manuellement dans AuthServiceImpl
        // Si on passe locked=true ici, Spring Security intercepte AVANT registerFailedAttempt
        // et la 3ème tentative ne compte jamais
        return org.springframework.security.core.userdetails.User.builder()
                .username(user.getEmail())
                .password(user.getPasswordHash())
                .authorities(authorities)
                .accountExpired(false)
                .accountLocked(false)    // ← toujours false ici
                .credentialsExpired(false)
                .disabled(false)         // ← toujours false ici
                .build();
    }
}

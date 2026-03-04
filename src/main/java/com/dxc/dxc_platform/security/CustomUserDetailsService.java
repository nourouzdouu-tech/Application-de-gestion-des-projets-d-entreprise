package com.dxc.dxc_platform.security;

import com.dxc.dxc_platform.modules.admin.domain.entity.Permission;
import com.dxc.dxc_platform.modules.admin.domain.entity.Role;
import com.dxc.dxc_platform.modules.admin.domain.entity.User;
import com.dxc.dxc_platform.modules.admin.repository.UserRepository;
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
                .orElseThrow(() -> new UsernameNotFoundException("Utilisateur introuvable: " + email));

        List<GrantedAuthority> authorities = new ArrayList<>();

        for (Role role : user.getRoles()) {
            // Ajouter le rôle avec préfixe ROLE_
            authorities.add(new SimpleGrantedAuthority("ROLE_" + role.getNom().toUpperCase()));

            // Ajouter aussi toutes les permissions du rôle
            for (Permission permission : role.getPermissions()) {
                authorities.add(new SimpleGrantedAuthority(permission.getNom()));
            }
        }

        return org.springframework.security.core.userdetails.User.builder()
                .username(user.getEmail())
                .password(user.getPasswordHash())
                .authorities(authorities)
                .accountExpired(false)
                .accountLocked(user.isLocked())
                .credentialsExpired(false)
                .disabled(!user.isEnabled())
                .build();
    }
}

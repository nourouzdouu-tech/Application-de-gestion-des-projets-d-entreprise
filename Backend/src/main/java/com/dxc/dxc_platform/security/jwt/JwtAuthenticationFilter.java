package com.dxc.dxc_platform.security.jwt;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;

    public JwtAuthenticationFilter(JwtService jwtService,
                                   UserDetailsService userDetailsService) {
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        final String authHeader = request.getHeader("Authorization");
        System.out.println("=== [JWT Filter] Auth Header: " + (authHeader != null ? authHeader.substring(0, Math.min(50, authHeader.length())) + "..." : "null"));

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            System.out.println("=== [JWT Filter] No Bearer token found");
            filterChain.doFilter(request, response);
            return;
        }

        final String jwt = authHeader.substring(7);
        final String userEmail;

        try {
            userEmail = jwtService.extractUsername(jwt);
            System.out.println("=== [JWT Filter] Extracted email: " + userEmail);

            // Vérifier les rôles dans le token
            List<String> roles = jwtService.extractRoles(jwt);
            System.out.println("=== [JWT Filter] Roles in token: " + roles);

        } catch (Exception e) {
            System.out.println("=== [JWT Filter] Error extracting JWT: " + e.getMessage());
            filterChain.doFilter(request, response);
            return;
        }

        if (userEmail != null
                && SecurityContextHolder.getContext().getAuthentication() == null) {

            System.out.println("=== [JWT Filter] Loading UserDetails for: " + userEmail);
            UserDetails userDetails =
                    this.userDetailsService.loadUserByUsername(userEmail);

            System.out.println("=== [JWT Filter] UserDetails authorities: " + userDetails.getAuthorities());
            System.out.println("=== [JWT Filter] UserDetails isAccountNonLocked: " + userDetails.isAccountNonLocked());
            System.out.println("=== [JWT Filter] UserDetails isEnabled: " + userDetails.isEnabled());

            if (jwtService.isTokenValid(jwt, userDetails)) {
                UsernamePasswordAuthenticationToken authToken =
                        new UsernamePasswordAuthenticationToken(
                                userDetails,
                                null,
                                userDetails.getAuthorities()
                        );
                authToken.setDetails(
                        new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authToken);

                System.out.println("=== [JWT Filter] Authentication set successfully");
                System.out.println("=== [JWT Filter] Has ADMIN authority: " +
                        SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream()
                                .anyMatch(a -> a.getAuthority().equals("ADMIN")));
            } else {
                System.out.println("=== [JWT Filter] Token is invalid for user: " + userEmail);
            }
        }

        filterChain.doFilter(request, response);
    }
}
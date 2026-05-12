package com.dxc.dxc_platform.security.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

@Service
public class JwtService {

    @Value("${jwt.secret}")
    private String secretKey;

    @Value("${jwt.expiration:86400000}")
    private long jwtExpiration;

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    @SuppressWarnings("unchecked")
    public List<String> extractRoles(String token) {
        return extractAllClaims(token).get("roles", List.class);
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        return claimsResolver.apply(extractAllClaims(token));
    }

    public String generateToken(UserDetails userDetails) {
        Map<String, Object> extraClaims = new HashMap<>();

        List<String> roles = userDetails.getAuthorities().stream()
                .map(a -> a.getAuthority())
                .toList();
        List<String> finalRoles = new ArrayList<>(roles);
        if (roles.contains("ROLE_ADMIN") && !roles.contains("ADMIN")) {
            finalRoles.add("ADMIN");
        }
        extraClaims.put("roles", finalRoles);

        return Jwts.builder()
                .claims(extraClaims)                                          // replaces setClaims()
                .subject(userDetails.getUsername())                           // replaces setSubject()
                .issuedAt(new Date(System.currentTimeMillis()))               // replaces setIssuedAt()
                .expiration(new Date(System.currentTimeMillis() + jwtExpiration)) // replaces setExpiration()
                .signWith(getSignInKey())                                     // no algorithm arg needed
                .compact();
    }

    public boolean isTokenValid(String token, UserDetails userDetails) {
        final String username = extractUsername(token);
        return username.equals(userDetails.getUsername()) && !isTokenExpired(token);
    }

    private boolean isTokenExpired(String token) {
        return extractClaim(token, Claims::getExpiration).before(new Date());
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSignInKey())            // replaces setSigningKey()
                .build()
                .parseSignedClaims(token)              // replaces parseClaimsJws()
                .getPayload();                         // replaces getBody()
    }

    private SecretKey getSignInKey() {
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);  // plus propre que Base64.getDecoder()
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
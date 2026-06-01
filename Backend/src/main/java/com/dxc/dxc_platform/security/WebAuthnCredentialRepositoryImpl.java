package com.dxc.dxc_platform.security;

import com.dxc.dxc_platform.repository.UserRepository;
import com.dxc.dxc_platform.repository.WebAuthnCredentialRepository;
import com.yubico.webauthn.CredentialRepository;
import com.yubico.webauthn.RegisteredCredential;
import com.yubico.webauthn.data.ByteArray;
import com.yubico.webauthn.data.PublicKeyCredentialDescriptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import com.dxc.dxc_platform.entity.User;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

  // ← MANQUAIT
public class WebAuthnCredentialRepositoryImpl implements CredentialRepository {

    private static final Logger log = LoggerFactory.getLogger(WebAuthnCredentialRepositoryImpl.class);

    private final WebAuthnCredentialRepository repo;
    private final UserRepository userRepository;

    public WebAuthnCredentialRepositoryImpl(WebAuthnCredentialRepository repo,
                                            UserRepository userRepository) {
        this.repo = repo;
        this.userRepository = userRepository;
    }

    private String normalize(String email) {
        return email == null ? null : email.trim().toLowerCase();
    }

    @Override
    public Set<PublicKeyCredentialDescriptor> getCredentialIdsForUsername(String email) {
        String n = normalize(email);
        log.info("[WebAuthn] getCredentialIdsForUsername for: {}", n);
        return repo.findByUserEmailIgnoreCase(n).stream()   // ← IgnoreCase
                .map(c -> {
                    try {
                        return PublicKeyCredentialDescriptor.builder()
                                .id(ByteArray.fromBase64Url(c.getCredentialId()))
                                .build();
                    } catch (Exception e) {
                        log.error("Invalid credentialId for email: {}", n, e);
                        throw new RuntimeException("Invalid credentialId", e);
                    }
                })
                .collect(Collectors.toSet());
    }

    @Override
    public Optional<ByteArray> getUserHandleForUsername(String email) {
        String n = normalize(email);
        return userRepository.findByEmailIgnoreCaseAndDeletedFalse(n)  // ← IgnoreCase
                .map(u -> new ByteArray(
                        Base64.getEncoder().encode(
                                u.getId().toString().getBytes(StandardCharsets.UTF_8))
                ));
    }

    @Override
    public Optional<String> getUsernameForUserHandle(ByteArray userHandle) {
        try {
            byte[] bytes = userHandle.getBytes();
            log.debug("[WebAuthn] getUsernameForUserHandle - raw bytes: {}", Arrays.toString(bytes));

            String userIdStr;
            try {
                byte[] decoded = Base64.getDecoder().decode(bytes);
                userIdStr = new String(decoded, StandardCharsets.UTF_8);
            } catch (IllegalArgumentException e) {
                userIdStr = new String(bytes, StandardCharsets.UTF_8);
            }

            log.debug("[WebAuthn] looking up user with ID: {}", userIdStr);
            Optional<User> user = userRepository.findById(Long.valueOf(userIdStr));

            return user.filter(u -> !u.isDeleted())
                    .map(u -> normalize(u.getEmail()));

        } catch (Exception e) {
            log.error("[WebAuthn] Failed to resolve username for userHandle: {}",
                    userHandle.getBase64Url(), e);
            return Optional.empty();
        }
    }

    @Override
    public Optional<RegisteredCredential> lookup(ByteArray credentialId, ByteArray userHandle) {
        log.info("[WebAuthn] lookup credentialId: {}", credentialId.getBase64Url());
        return repo.findByCredentialId(credentialId.getBase64Url())
                .map(c -> {
                    try {
                        return RegisteredCredential.builder()
                                .credentialId(ByteArray.fromBase64Url(c.getCredentialId()))
                                .userHandle(userHandle)
                                .publicKeyCose(decodePublicKey(c.getPublicKeyCose()))
                                .signatureCount(c.getSignatureCount())
                                .build();
                    } catch (Exception e) {
                        log.error("[WebAuthn] Invalid credential data: {}", credentialId.getBase64Url(), e);
                        throw new RuntimeException("Invalid credential data", e);
                    }
                });
    }

      @Override
      public Set<RegisteredCredential> lookupAll(ByteArray credentialId) {
          return repo.findByCredentialId(credentialId.getBase64Url())
                  .map(c -> {
                      try {
                          // Reconstruire le vrai userHandle depuis l'email
                          Optional<User> user = userRepository
                                  .findByEmailIgnoreCaseAndDeletedFalse(c.getUserEmail());
                          ByteArray userHandle = user.map(u -> new ByteArray(
                                  Base64.getEncoder().encode(
                                          u.getId().toString().getBytes(StandardCharsets.UTF_8))
                          )).orElse(credentialId); // fallback

                          return RegisteredCredential.builder()
                                  .credentialId(ByteArray.fromBase64Url(c.getCredentialId()))
                                  .userHandle(userHandle) // ← userHandle correct
                                  .publicKeyCose(decodePublicKey(c.getPublicKeyCose()))
                                  .signatureCount(c.getSignatureCount())
                                  .build();
                      } catch (Exception e) {
                          throw new RuntimeException("Invalid credential data", e);
                      }
                  })
                  .map(Set::of)
                  .orElse(Set.of());
      }

    // Méthode mutualisée pour décoder la clé publique
    private ByteArray decodePublicKey(String pk) throws Exception {
        if (pk.contains("-") || pk.contains("_")) {
            String std = pk.replace('-', '+').replace('_', '/');
            int padding = (4 - std.length() % 4) % 4;
            std += "=".repeat(padding);
            return ByteArray.fromBase64(std);
        }
        return ByteArray.fromBase64(pk);
    }
}
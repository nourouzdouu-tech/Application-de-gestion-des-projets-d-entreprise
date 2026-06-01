package com.dxc.dxc_platform.repository;

import com.dxc.dxc_platform.entity.WebAuthnCredential;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface WebAuthnCredentialRepository extends JpaRepository<WebAuthnCredential, String> {
    List<WebAuthnCredential> findByUserEmailIgnoreCase(String email);  // ← IgnoreCase
    List<WebAuthnCredential> findByUserEmail(String email);
    Optional<WebAuthnCredential> findByCredentialId(String credentialId);
}
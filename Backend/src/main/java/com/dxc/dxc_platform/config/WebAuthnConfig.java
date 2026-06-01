package com.dxc.dxc_platform.config;

import com.dxc.dxc_platform.repository.UserRepository;
import com.dxc.dxc_platform.repository.WebAuthnCredentialRepository;
import com.dxc.dxc_platform.security.WebAuthnCredentialRepositoryImpl;
import com.yubico.webauthn.RelyingParty;
import com.yubico.webauthn.data.RelyingPartyIdentity;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Set;

@Configuration
public class WebAuthnConfig {

    @Bean("webAuthnCredRepo")  // ← nom différent du nom de classe
    public WebAuthnCredentialRepositoryImpl webAuthnCredRepo(
            WebAuthnCredentialRepository repo,
            UserRepository userRepository) {
        return new WebAuthnCredentialRepositoryImpl(repo, userRepository);
    }

    @Bean
    public RelyingParty relyingParty(WebAuthnCredentialRepositoryImpl credRepo) {
        return RelyingParty.builder()
                .identity(RelyingPartyIdentity.builder()
                        .id("localhost")
                        .name("DXC Platform")
                        .build())
                .credentialRepository(credRepo)
                .allowOriginPort(true)
                .origins(Set.of(
                        "http://localhost:4200",
                        "https://localhost:4200"
                ))
                .build();
    }
}
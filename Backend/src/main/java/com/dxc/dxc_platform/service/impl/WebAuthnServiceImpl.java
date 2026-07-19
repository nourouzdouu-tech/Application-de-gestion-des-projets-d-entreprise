package com.dxc.dxc_platform.service.impl;

import com.dxc.dxc_platform.entity.WebAuthnCredential;
import com.dxc.dxc_platform.repository.UserRepository;
import com.dxc.dxc_platform.repository.WebAuthnCredentialRepository;
import com.dxc.dxc_platform.security.CustomUserDetailsService;
import com.dxc.dxc_platform.security.jwt.JwtService;
import com.dxc.dxc_platform.service.WebAuthnService;
import com.yubico.webauthn.*;
import com.yubico.webauthn.data.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
public class WebAuthnServiceImpl implements WebAuthnService {

    private static final Logger log = LoggerFactory.getLogger(WebAuthnServiceImpl.class);

    private final RelyingParty relyingParty;
    private final WebAuthnCredentialRepository credentialRepo;
    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final CustomUserDetailsService userDetailsService;

    private final Map<String, Object> pending = new ConcurrentHashMap<>();

    public WebAuthnServiceImpl(RelyingParty relyingParty,
                               WebAuthnCredentialRepository credentialRepo,
                               UserRepository userRepository,
                               JwtService jwtService,
                               CustomUserDetailsService userDetailsService) {
        this.relyingParty = relyingParty;
        this.credentialRepo = credentialRepo;
        this.userRepository = userRepository;
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
    }

    private String normalize(String email) {
        return email == null ? null : email.trim().toLowerCase();
    }

    @Override
    public String startRegistration(String email) throws Exception {
        final String n = normalize(email);
        log.info("[WebAuthn] startRegistration for: {}", n);

        var user = userRepository.findByEmailIgnoreCaseAndDeletedFalse(n)
                .orElseThrow(() -> new UsernameNotFoundException("Introuvable: " + n));

        var options = relyingParty.startRegistration(
                StartRegistrationOptions.builder()
                        .user(UserIdentity.builder()
                                .name(n)
                                .displayName(user.getPrenom() + " " + user.getNom())

                                .id(new ByteArray(Base64.getEncoder()
                                        .encode(user.getId().toString().getBytes(StandardCharsets.UTF_8))))
                                .build())
                        .authenticatorSelection(AuthenticatorSelectionCriteria.builder()
                                .authenticatorAttachment(AuthenticatorAttachment.PLATFORM)
                                .userVerification(UserVerificationRequirement.REQUIRED)
                                .build())
                        .build());

        pending.put("reg_" + n, options);
        log.info("[WebAuthn] Registration options created for: {}", n);
        return options.toCredentialsCreateJson();
    }

    @Override
    @Transactional
    public void finishRegistration(String email, String credentialJson) throws Exception {
        final String n = normalize(email);
        log.info("========================================");
        log.info("[WebAuthn] finishRegistration for: {}", n);

        var options = (PublicKeyCredentialCreationOptions) pending.remove("reg_" + n);
        if (options == null) {
            log.error("[WebAuthn] ❌ Session not found for key: reg_{}", n);
            throw new IllegalStateException("Session expirée.");
        }

        var result = relyingParty.finishRegistration(
                FinishRegistrationOptions.builder()
                        .request(options)
                        .response(PublicKeyCredential.parseRegistrationResponseJson(credentialJson))
                        .build());

        byte[] publicKeyBytes = result.getPublicKeyCose().getBytes();
        String publicKeyBase64 = Base64.getEncoder().encodeToString(publicKeyBytes);

        log.info("[WebAuthn] 🔑 PublicKeyCose bytes length: {}", publicKeyBytes.length);
        log.info("[WebAuthn] 🔑 Base64 length: {}", publicKeyBase64.length());

        var cred = new WebAuthnCredential();
        cred.setCredentialId(result.getKeyId().getId().getBase64Url());
        cred.setUserEmail(n);
        cred.setPublicKeyCose(publicKeyBase64);
        cred.setSignatureCount(result.getSignatureCount());

        log.info("[WebAuthn] 💾 Saving - publicKeyCose Base64 length: {}", publicKeyBase64.length());

        credentialRepo.save(cred);

        var saved = credentialRepo.findById(cred.getId()).orElse(null);
        if (saved != null) {
            log.info("[WebAuthn] ✅ Saved - DB publicKeyCose length: {}", saved.getPublicKeyCose().length());
        }

        log.info("[WebAuthn] ✅ Registration complete for: {}", n);
        log.info("========================================");
    }

    @Override
    @Transactional(readOnly = true)
    public String startLogin(String email) throws Exception {
        final String n = normalize(email);
        log.info("[WebAuthn] startLogin for: {}", n);

        var request = relyingParty.startAssertion(
                StartAssertionOptions.builder()
                        .username(n)
                        .userVerification(UserVerificationRequirement.REQUIRED)
                        .build());

        pending.put("login_" + n, request);
        log.info("[WebAuthn] Login options created for: {}. Pending keys: {}", n, pending.keySet());
        return request.toCredentialsGetJson();
    }

    @Override
    public String finishLogin(String email, String assertionJson) throws Exception {
        final String n = normalize(email);
        log.info("[WebAuthn] finishLogin - email: {}", n);
        log.info("[WebAuthn] pending keys: {}", pending.keySet());

        String key = "login_" + n;
        var request = (AssertionRequest) pending.remove(key);

        if (request == null) {
            log.error("[WebAuthn] ❌ Session NOT FOUND for key: '{}'", key);
            log.error("[WebAuthn] Available keys: {}", pending.keySet());
            throw new IllegalStateException("Session expirée. Veuillez réessayer.");
        }

        log.info("[WebAuthn] Session found, verifying assertion...");

        try {
            var result = relyingParty.finishAssertion(
                    FinishAssertionOptions.builder()
                            .request(request)
                            .response(PublicKeyCredential.parseAssertionResponseJson(assertionJson))
                            .build());

            log.info("[WebAuthn] ✅ Assertion successful for: {}", n);

            UserDetails userDetails = userDetailsService.loadUserByUsername(n);
            return jwtService.generateToken(userDetails);

        } catch (Exception e) {
            log.error("[WebAuthn] ❌ Assertion verification failed for: {}", n, e);
            throw e;
        }
    }

    @Override
    public boolean hasCredential(String email) {
        boolean has = !credentialRepo.findByUserEmailIgnoreCase(normalize(email)).isEmpty();
        log.info("[WebAuthn] hasCredential for {}: {}", email, has);
        return has;
    }

}
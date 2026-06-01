package com.dxc.dxc_platform.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.LocalDateTime;

@Entity
@Table(name = "webauthn_credentials")
public class WebAuthnCredential {

    @Id
    @Column(name = "id", length = 36)
    private String id;

    @Column(nullable = false, unique = true, length = 500)
    private String credentialId;

    @Column(nullable = false, length = 255)
    private String userEmail;

    // ← IMPORTANT : Utiliser TEXT en minuscule et éviter @Lob pour String
    @Column(nullable = false, columnDefinition = "text")
    private String publicKeyCose;

    @Column(nullable = false)
    private long signatureCount;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        if (this.id == null) {
            this.id = java.util.UUID.randomUUID().toString();
        }
    }

    // Getters et Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getCredentialId() { return credentialId; }
    public void setCredentialId(String v) { this.credentialId = v; }

    public String getUserEmail() { return userEmail; }
    public void setUserEmail(String v) { this.userEmail = v; }

    public String getPublicKeyCose() { return publicKeyCose; }
    public void setPublicKeyCose(String v) { this.publicKeyCose = v; }

    public long getSignatureCount() { return signatureCount; }
    public void setSignatureCount(long v) { this.signatureCount = v; }

    public LocalDateTime getCreatedAt() { return createdAt; }
}
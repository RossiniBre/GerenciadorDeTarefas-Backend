package com.taskmanager.domain.security;

import java.time.Instant;
import java.util.UUID;

public class PasswordResetToken {
    private final String id;
    private final String userId;
    private final String tokenHash;
    private final Instant expiresAt;
    private final Instant usedAt;

    private PasswordResetToken(String id, String userId, String tokenHash, Instant expiresAt, Instant usedAt) {
        if (userId == null || userId.isBlank()) {
            throw new IllegalArgumentException("userId é obrigatório!");
        }
        if (tokenHash == null || tokenHash.isBlank()) {
            throw new IllegalArgumentException("tokenHash é obrigatório!");
        }
        if (expiresAt == null) {
            throw new IllegalArgumentException("expiresAt é obrigatório!");
        }
        this.id = id;
        this.userId = userId;
        this.tokenHash = tokenHash;
        this.expiresAt = expiresAt;
        this.usedAt = usedAt;
    }

    public static PasswordResetToken newToken(String userId, String tokenHash, Instant expiresAt) {
        return new PasswordResetToken(UUID.randomUUID().toString(), userId, tokenHash, expiresAt, null);
    }

    public static PasswordResetToken rebuiltToken(String id, String userId, String tokenHash, Instant expiresAt, Instant usedAt) {
        return new PasswordResetToken(id, userId, tokenHash, expiresAt, usedAt);
    }

    public boolean isExpired(Instant now) {
        return now.isAfter(expiresAt);
    }

    public boolean isUsed() {
        return usedAt != null;
    }

    public String getId() { return id; }
    public String getUserId() { return userId; }
    public String getTokenHash() { return tokenHash; }
    public Instant getExpiresAt() { return expiresAt; }
    public Instant getUsedAt() { return usedAt; }
}
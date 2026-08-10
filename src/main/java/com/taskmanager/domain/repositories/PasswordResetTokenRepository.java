package com.taskmanager.domain.repositories;

import com.taskmanager.domain.security.PasswordResetToken;

import java.util.Optional;

public interface PasswordResetTokenRepository {
    PasswordResetToken save(PasswordResetToken token);
    Optional<PasswordResetToken> findByTokenHash(String tokenHash);
    void markAsUsed(String id);
    void invalidateAllForUser(String userId);
}
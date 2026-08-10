package com.taskmanager.infrastructure.security;

import com.taskmanager.domain.security.PasswordResetTokenGenerator;

import java.security.SecureRandom;
import java.util.Base64;

public class SecureRandomPasswordResetTokenGenerator implements PasswordResetTokenGenerator {

    private static final int TOKEN_BYTES = 32;

    private final SecureRandom secureRandom = new SecureRandom();

    @Override
    public String generate() {
        byte[] randomBytes = new byte[TOKEN_BYTES];
        secureRandom.nextBytes(randomBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }
}
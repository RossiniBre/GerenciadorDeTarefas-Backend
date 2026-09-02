package com.taskmanager.infrastructure.security;

import com.taskmanager.domain.security.PasswordResetTokenGenerator;

import java.security.SecureRandom;

public class SecureRandomPasswordResetTokenGenerator
        implements PasswordResetTokenGenerator {

    private final SecureRandom secureRandom = new SecureRandom();

    @Override
    public String generate() {
        return String.format("%06d", secureRandom.nextInt(1_000_000));
    }
}
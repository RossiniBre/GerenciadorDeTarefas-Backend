package com.taskmanager.infrastructure.security;

import com.taskmanager.domain.security.PasswordResetTokenSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConsolePasswordResetTokenSender implements PasswordResetTokenSender {

    private static final Logger log = LoggerFactory.getLogger(ConsolePasswordResetTokenSender.class);

    @Override
    public void send(String email, String rawToken) {
        log.info("[STUB] Link de recuperação de senha para {}: /reset-password?token={}", email, rawToken);
    }
}
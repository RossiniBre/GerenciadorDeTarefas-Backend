package com.taskmanager.domain.security;

public interface PasswordResetTokenSender {
    void send(String email, String rawToken);
}
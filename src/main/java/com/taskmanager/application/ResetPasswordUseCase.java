package com.taskmanager.application;

import com.taskmanager.domain.exceptions.InvalidFieldException;
import com.taskmanager.domain.exceptions.InvalidOrExpiredTokenException;
import com.taskmanager.domain.security.PasswordResetToken;
import com.taskmanager.domain.model.User;
import com.taskmanager.domain.repositories.PasswordResetTokenRepository;
import com.taskmanager.domain.repositories.UserRepository;
import com.taskmanager.domain.security.PasswordHasher;
import com.taskmanager.domain.security.TokenHasher;

import java.time.Clock;
import java.time.Instant;

public class ResetPasswordUseCase {

    private final PasswordResetTokenRepository tokenRepository;
    private final UserRepository userRepository;
    private final PasswordHasher passwordHasher;
    private final TokenHasher tokenHasher;
    private final Clock clock;

    public ResetPasswordUseCase(PasswordResetTokenRepository tokenRepository, UserRepository userRepository,
                                PasswordHasher passwordHasher, TokenHasher tokenHasher, Clock clock) {
        this.tokenRepository = tokenRepository;
        this.userRepository = userRepository;
        this.passwordHasher = passwordHasher;
        this.tokenHasher = tokenHasher;
        this.clock = clock;
    }

    public void execute(String rawToken, String newPassword) {
        if (newPassword == null || newPassword.isBlank()) {
            throw new InvalidFieldException("Nova senha é obrigatória!");
        }

        String tokenHash = tokenHasher.hash(rawToken);
        PasswordResetToken resetToken = tokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(InvalidOrExpiredTokenException::new);

        if (resetToken.isUsed() || resetToken.isExpired(Instant.now(clock))) {
            throw new InvalidOrExpiredTokenException();
        }

        User user = userRepository.findById(resetToken.getUserId())
                .orElseThrow(InvalidOrExpiredTokenException::new);

        String hashedPassword = passwordHasher.hash(newPassword);
        User updatedUser = User.rebuiltUser(user.getId(), user.getEmail(), user.getUsername(), hashedPassword);
        userRepository.save(updatedUser);

        tokenRepository.markAsUsed(resetToken.getId());
        tokenRepository.invalidateAllForUser(user.getId());
    }
}
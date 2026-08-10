package com.taskmanager.application;

import com.taskmanager.domain.security.*;
import com.taskmanager.domain.model.User;
import com.taskmanager.domain.repositories.PasswordResetTokenRepository;
import com.taskmanager.domain.repositories.UserRepository;
import com.taskmanager.domain.security.PasswordResetTokenGenerator;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

public class ForgotPasswordUseCase {

    private static final Duration TOKEN_DURATION = Duration.ofMinutes(30);

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final TokenHasher tokenHasher;
    private final PasswordResetTokenSender tokenSender;
    private final Clock clock;
    private final PasswordResetTokenGenerator tokenGenerator; // era TokenGenerator

    public ForgotPasswordUseCase(UserRepository userRepository, PasswordResetTokenRepository tokenRepository,
                                 PasswordResetTokenGenerator tokenGenerator, TokenHasher tokenHasher,
                                 PasswordResetTokenSender tokenSender, Clock clock) {
        this.userRepository = userRepository;
        this.tokenRepository = tokenRepository;
        this.tokenGenerator = tokenGenerator;
        this.tokenHasher = tokenHasher;
        this.tokenSender = tokenSender;
        this.clock = clock;
    }

    public void execute(String email) {
        Optional<User> foundUser = userRepository.findByEmail(email);
        if (foundUser.isEmpty()) {
            return;
        }

        User user = foundUser.get();

        tokenRepository.invalidateAllForUser(user.getId());

        String rawToken = tokenGenerator.generate();
        String tokenHash = tokenHasher.hash(rawToken);
        Instant expiresAt = Instant.now(clock).plus(TOKEN_DURATION);

        PasswordResetToken resetToken = PasswordResetToken.newToken(user.getId(), tokenHash, expiresAt);
        tokenRepository.save(resetToken);

        tokenSender.send(user.getEmail(), rawToken);
    }
}
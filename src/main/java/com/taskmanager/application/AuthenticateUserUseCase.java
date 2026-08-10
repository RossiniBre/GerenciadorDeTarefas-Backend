package com.taskmanager.application;

import com.taskmanager.domain.security.LoginRateLimiter;
import com.taskmanager.domain.repositories.SessionRepository;
import com.taskmanager.domain.repositories.UserRepository;
import com.taskmanager.domain.security.TokenGenerator;
import com.taskmanager.domain.model.User;
import com.taskmanager.domain.exceptions.InvalidCredentialsException;
import com.taskmanager.domain.exceptions.TooManyAttemptsException;

import java.time.Duration;
import java.time.Instant;

public class AuthenticateUserUseCase {

    private static final Duration SESSION_DURATION = Duration.ofHours(2);

    private final LoginUseCase loginUseCase;
    private final UserRepository userRepository;
    private final SessionRepository sessionRepository;
    private final TokenGenerator tokenGenerator;
    private final LoginRateLimiter rateLimiter;

    public AuthenticateUserUseCase(LoginUseCase loginUseCase, UserRepository userRepository,
                                   SessionRepository sessionRepository, TokenGenerator tokenGenerator,
                                   LoginRateLimiter rateLimiter) {
        this.loginUseCase = loginUseCase;
        this.userRepository = userRepository;
        this.sessionRepository = sessionRepository;
        this.tokenGenerator = tokenGenerator;
        this.rateLimiter = rateLimiter;
    }

    public Session execute(String identifier, String password) {
        String rateLimitKey = resolveRateLimitKey(identifier);

        if (rateLimiter.isBlocked(rateLimitKey)) {
            throw new TooManyAttemptsException(identifier);
        }

        User user;
        try {
            user = loginUseCase.execute(identifier, password);
        } catch (InvalidCredentialsException e) {
            rateLimiter.registerFailure(rateLimitKey);
            throw e;
        }

        rateLimiter.registerSuccess(rateLimitKey);

        String token = tokenGenerator.generate();
        Instant expiresAt = Instant.now().plus(SESSION_DURATION);
        sessionRepository.save(token, user.getId(), expiresAt);

        return new Session(token, user);
    }

    private String resolveRateLimitKey(String identifier) {
        return userRepository.findByEmail(identifier)
                .or(() -> userRepository.findByUsername(identifier))
                .map(User::getId)
                .orElse(identifier);
    }

    public static class Session {
        private final String token;
        private final User user;

        public Session(String token, User user) {
            this.token = token;
            this.user = user;
        }

        public String getToken() { return token; }
        public User getUser() { return user; }
    }
}
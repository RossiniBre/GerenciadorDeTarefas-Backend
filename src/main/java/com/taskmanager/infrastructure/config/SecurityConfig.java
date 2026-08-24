package com.taskmanager.infrastructure.config;

import com.taskmanager.application.AuthenticateUserUseCase;
import com.taskmanager.application.LoginUseCase;
import com.taskmanager.application.RegisterUserUseCase;
import com.taskmanager.application.UpdateUserProfileUseCase;
import com.taskmanager.domain.repositories.SessionRepository;
import com.taskmanager.domain.repositories.UserRepository;
import com.taskmanager.domain.security.LoginRateLimiter;
import com.taskmanager.domain.security.PasswordHasher;
import com.taskmanager.domain.security.TokenGenerator;
import com.taskmanager.infrastructure.persistence.InMemoryLoginRateLimiter;
import com.taskmanager.infrastructure.persistence.InMemorySessionRepository;
import com.taskmanager.infrastructure.security.Pbkdf2PasswordHasher;
import com.taskmanager.infrastructure.security.UuidTokenGenerator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SecurityConfig {

    @Bean
    public SessionRepository sessionRepository() {
        return new InMemorySessionRepository();
    }

    @Bean
    public TokenGenerator tokenGenerator() {
        return new UuidTokenGenerator();
    }

    @Bean
    public LoginRateLimiter loginRateLimiter() {
        return new InMemoryLoginRateLimiter();
    }

    @Bean
    public PasswordHasher passwordHasher() {
        return new Pbkdf2PasswordHasher();
    }

    @Bean
    public LoginUseCase loginUseCase(UserRepository userRepository, PasswordHasher passwordHasher) {
        return new LoginUseCase(userRepository, passwordHasher);
    }

    @Bean
    public RegisterUserUseCase registerUserUseCase(UserRepository userRepository, PasswordHasher passwordHasher) {
        return new RegisterUserUseCase(userRepository, passwordHasher);
    }

    @Bean
    public UpdateUserProfileUseCase updateUserProfileUseCase(UserRepository userRepository) {
        return new UpdateUserProfileUseCase(userRepository);
    }

    @Bean
    public AuthenticateUserUseCase authenticateUserUseCase(LoginUseCase loginUseCase,
                                                           UserRepository userRepository,
                                                           SessionRepository sessionRepository,
                                                           TokenGenerator tokenGenerator,
                                                           LoginRateLimiter loginRateLimiter) {
        return new AuthenticateUserUseCase(loginUseCase, userRepository, sessionRepository, tokenGenerator,
                loginRateLimiter);
    }
}
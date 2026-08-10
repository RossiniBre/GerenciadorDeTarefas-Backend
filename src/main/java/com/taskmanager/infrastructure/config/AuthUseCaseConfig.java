package com.taskmanager.infrastructure.config;

import com.taskmanager.application.ForgotPasswordUseCase;
import com.taskmanager.application.ResetPasswordUseCase;
import com.taskmanager.domain.notification.SmtpPasswordResetTokenSender;
import com.taskmanager.domain.repositories.PasswordResetTokenRepository;
import com.taskmanager.domain.repositories.UserRepository;
import com.taskmanager.domain.security.PasswordHasher;
import com.taskmanager.domain.security.PasswordResetTokenGenerator;
import com.taskmanager.domain.security.PasswordResetTokenSender;
import com.taskmanager.domain.security.TokenHasher;
import com.taskmanager.infrastructure.security.SecureRandomPasswordResetTokenGenerator;
import com.taskmanager.infrastructure.security.Sha256TokenHasher;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;

import java.time.Clock;

@Configuration
public class AuthUseCaseConfig {

    @Bean
    public TokenHasher tokenHasher() {
        return new Sha256TokenHasher();
    }

    @Bean
    public PasswordResetTokenGenerator passwordResetTokenGenerator() {
        return new SecureRandomPasswordResetTokenGenerator();
    }

    @Bean
    public PasswordResetTokenSender passwordResetTokenSender(JavaMailSender mailSender,
                                                             @Value("${mail.from}") String fromAddress) {
        return new SmtpPasswordResetTokenSender(mailSender, fromAddress);
    }

    @Bean
    public ForgotPasswordUseCase forgotPasswordUseCase(UserRepository userRepository,
                                                       PasswordResetTokenRepository tokenRepository,
                                                       PasswordResetTokenGenerator tokenGenerator,
                                                       TokenHasher tokenHasher,
                                                       PasswordResetTokenSender tokenSender,
                                                       Clock clock) {
        return new ForgotPasswordUseCase(userRepository, tokenRepository, tokenGenerator, tokenHasher,
                tokenSender, clock);
    }

    @Bean
    public ResetPasswordUseCase resetPasswordUseCase(PasswordResetTokenRepository tokenRepository,
                                                     UserRepository userRepository,
                                                     PasswordHasher passwordHasher,
                                                     TokenHasher tokenHasher,
                                                     Clock clock) {
        return new ResetPasswordUseCase(tokenRepository, userRepository, passwordHasher, tokenHasher, clock);
    }
}
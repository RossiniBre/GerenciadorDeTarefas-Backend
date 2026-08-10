package application;

import com.taskmanager.application.ForgotPasswordUseCase;
import com.taskmanager.domain.model.User;
import com.taskmanager.domain.repositories.PasswordResetTokenRepository;
import com.taskmanager.domain.repositories.UserRepository;
import com.taskmanager.domain.security.PasswordResetToken;
import com.taskmanager.domain.security.PasswordResetTokenGenerator;
import com.taskmanager.domain.security.PasswordResetTokenSender;
import com.taskmanager.domain.security.TokenHasher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class ForgotPasswordUseCaseTest {

    private static final Instant FIXED_NOW = Instant.parse("2026-08-10T12:00:00Z");

    private UserRepository userRepository;
    private PasswordResetTokenRepository tokenRepository;
    private PasswordResetTokenGenerator tokenGenerator;
    private TokenHasher tokenHasher;
    private PasswordResetTokenSender tokenSender;
    private Clock clock;
    private ForgotPasswordUseCase forgotPasswordUseCase;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        tokenRepository = mock(PasswordResetTokenRepository.class);
        tokenGenerator = mock(PasswordResetTokenGenerator.class);
        tokenHasher = mock(TokenHasher.class);
        tokenSender = mock(PasswordResetTokenSender.class);
        clock = Clock.fixed(FIXED_NOW, ZoneOffset.UTC);
        forgotPasswordUseCase = new ForgotPasswordUseCase(
                userRepository, tokenRepository, tokenGenerator, tokenHasher, tokenSender, clock);
    }

    @Test
    void doesNothingWhenEmailDoesNotExist() {
        when(userRepository.findByEmail("missing@example.com")).thenReturn(Optional.empty());

        forgotPasswordUseCase.execute("missing@example.com");

        verifyNoInteractions(tokenRepository, tokenGenerator, tokenHasher, tokenSender);
    }

    @Test
    void invalidatesExistingTokensBeforeCreatingNewOne() {
        User user = User.rebuiltUser("user-1", "user@example.com", "user", "hashed-pw");
        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));
        when(tokenGenerator.generate()).thenReturn("raw-token");
        when(tokenHasher.hash("raw-token")).thenReturn("hashed-token");

        forgotPasswordUseCase.execute("user@example.com");

        InOrder inOrder = inOrder(tokenRepository, tokenGenerator, tokenHasher, tokenSender);
        inOrder.verify(tokenRepository).invalidateAllForUser("user-1");
        inOrder.verify(tokenGenerator).generate();
        inOrder.verify(tokenHasher).hash("raw-token");
        inOrder.verify(tokenRepository).save(any());
        inOrder.verify(tokenSender).send("user@example.com", "raw-token");
    }

    @Test
    void savesTokenWithHashUserAndExpiration() {
        User user = User.rebuiltUser("user-1", "user@example.com", "user", "hashed-pw");
        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));
        when(tokenGenerator.generate()).thenReturn("raw-token");
        when(tokenHasher.hash("raw-token")).thenReturn("hashed-token");

        forgotPasswordUseCase.execute("user@example.com");

        ArgumentCaptor<PasswordResetToken> captor = ArgumentCaptor.forClass(PasswordResetToken.class);
        verify(tokenRepository).save(captor.capture());

        PasswordResetToken savedToken = captor.getValue();
        assertEquals("user-1", savedToken.getUserId());
        assertEquals("hashed-token", savedToken.getTokenHash());
        assertEquals(FIXED_NOW.plusSeconds(30 * 60), savedToken.getExpiresAt());
    }

    @Test
    void sendsRawTokenNeverTheHashToUserEmail() {
        User user = User.rebuiltUser("user-1", "user@example.com", "user", "hashed-pw");
        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));
        when(tokenGenerator.generate()).thenReturn("raw-token");
        when(tokenHasher.hash("raw-token")).thenReturn("hashed-token");

        forgotPasswordUseCase.execute("user@example.com");

        verify(tokenSender).send("user@example.com", "raw-token");
        verify(tokenSender, never()).send(anyString(), eq("hashed-token"));
    }
}
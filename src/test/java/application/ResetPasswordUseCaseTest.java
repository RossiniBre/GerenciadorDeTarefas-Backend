package application;

import com.taskmanager.application.ResetPasswordUseCase;
import com.taskmanager.domain.exceptions.InvalidFieldException;
import com.taskmanager.domain.exceptions.InvalidOrExpiredTokenException;
import com.taskmanager.domain.model.User;
import com.taskmanager.domain.repositories.PasswordResetTokenRepository;
import com.taskmanager.domain.repositories.UserRepository;
import com.taskmanager.domain.security.PasswordHasher;
import com.taskmanager.domain.security.PasswordResetToken;
import com.taskmanager.domain.security.TokenHasher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

class ResetPasswordUseCaseTest {

    private static final Instant FIXED_NOW = Instant.parse("2026-08-10T12:00:00Z");

    private PasswordResetTokenRepository tokenRepository;
    private UserRepository userRepository;
    private PasswordHasher passwordHasher;
    private TokenHasher tokenHasher;
    private Clock clock;
    private ResetPasswordUseCase resetPasswordUseCase;

    @BeforeEach
    void setUp() {
        tokenRepository = mock(PasswordResetTokenRepository.class);
        userRepository = mock(UserRepository.class);
        passwordHasher = mock(PasswordHasher.class);
        tokenHasher = mock(TokenHasher.class);
        clock = Clock.fixed(FIXED_NOW, ZoneOffset.UTC);
        resetPasswordUseCase = new ResetPasswordUseCase(
                tokenRepository, userRepository, passwordHasher, tokenHasher, clock);
    }

    @Test
    void rejectsNullNewPasswordWithoutTouchingRepositories() {
        assertThrows(InvalidFieldException.class,
                () -> resetPasswordUseCase.execute("raw-token", null));

        verifyNoInteractions(tokenRepository, userRepository, passwordHasher, tokenHasher);
    }

    @Test
    void rejectsBlankNewPasswordWithoutTouchingRepositories() {
        assertThrows(InvalidFieldException.class,
                () -> resetPasswordUseCase.execute("raw-token", "   "));

        verifyNoInteractions(tokenRepository, userRepository, passwordHasher, tokenHasher);
    }

    @Test
    void rejectsTokenThatDoesNotExist() {
        when(tokenHasher.hash("raw-token")).thenReturn("hashed-token");
        when(tokenRepository.findByTokenHash("hashed-token")).thenReturn(Optional.empty());

        assertThrows(InvalidOrExpiredTokenException.class,
                () -> resetPasswordUseCase.execute("raw-token", "new-password-123"));

        verify(userRepository, never()).save(any());
    }

    @Test
    void rejectsTokenAlreadyUsed() {
        PasswordResetToken usedToken = PasswordResetToken.rebuiltToken(
                "token-1", "user-1", "hashed-token", FIXED_NOW.plusSeconds(600), FIXED_NOW.minusSeconds(60));
        when(tokenHasher.hash("raw-token")).thenReturn("hashed-token");
        when(tokenRepository.findByTokenHash("hashed-token")).thenReturn(Optional.of(usedToken));

        assertThrows(InvalidOrExpiredTokenException.class,
                () -> resetPasswordUseCase.execute("raw-token", "new-password-123"));

        verify(userRepository, never()).save(any());
        verify(tokenRepository, never()).markAsUsed(any());
    }

    @Test
    void rejectsExpiredToken() {
        PasswordResetToken expiredToken = PasswordResetToken.rebuiltToken(
                "token-1", "user-1", "hashed-token", FIXED_NOW.minusSeconds(1), null);
        when(tokenHasher.hash("raw-token")).thenReturn("hashed-token");
        when(tokenRepository.findByTokenHash("hashed-token")).thenReturn(Optional.of(expiredToken));

        assertThrows(InvalidOrExpiredTokenException.class,
                () -> resetPasswordUseCase.execute("raw-token", "new-password-123"));

        verify(userRepository, never()).save(any());
    }

    @Test
    void treatsDeletedUserAsInvalidTokenInsteadOfDistinctError() {
        PasswordResetToken validToken = PasswordResetToken.rebuiltToken(
                "token-1", "deleted-user", "hashed-token", FIXED_NOW.plusSeconds(600), null);
        when(tokenHasher.hash("raw-token")).thenReturn("hashed-token");
        when(tokenRepository.findByTokenHash("hashed-token")).thenReturn(Optional.of(validToken));
        when(userRepository.findById("deleted-user")).thenReturn(Optional.empty());

        assertThrows(InvalidOrExpiredTokenException.class,
                () -> resetPasswordUseCase.execute("raw-token", "new-password-123"));
    }

    @Test
    void successfulResetUpdatesPasswordMarksTokenUsedAndInvalidatesRest() {
        PasswordResetToken validToken = PasswordResetToken.rebuiltToken(
                "token-1", "user-1", "hashed-token", FIXED_NOW.plusSeconds(600), null);
        User user = User.rebuiltUser("user-1", "user@example.com", "user", "old-hashed-pw", "Owner");

        when(tokenHasher.hash("raw-token")).thenReturn("hashed-token");
        when(tokenRepository.findByTokenHash("hashed-token")).thenReturn(Optional.of(validToken));
        when(userRepository.findById("user-1")).thenReturn(Optional.of(user));
        when(passwordHasher.hash("new-password-123")).thenReturn("new-hashed-pw");

        resetPasswordUseCase.execute("raw-token", "new-password-123");

        verify(userRepository).save(argThat(updated ->
                updated.getId().equals("user-1") && updated.getPasswordHash().equals("new-hashed-pw")));
        verify(tokenRepository).markAsUsed("token-1");
        verify(tokenRepository).invalidateAllForUser("user-1");
    }

    @Test
    void doesNotHashPasswordWhenTokenIsInvalid() {
        when(tokenHasher.hash("raw-token")).thenReturn("hashed-token");
        when(tokenRepository.findByTokenHash("hashed-token")).thenReturn(Optional.empty());

        assertThrows(InvalidOrExpiredTokenException.class,
                () -> resetPasswordUseCase.execute("raw-token", "new-password-123"));

        verify(passwordHasher, never()).hash(any());
    }
}
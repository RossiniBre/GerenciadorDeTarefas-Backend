package infrastructure;

import com.taskmanager.domain.security.PasswordResetToken;
import com.taskmanager.infrastructure.config.RepositoryException;
import com.taskmanager.infrastructure.persistence.mysql.MySqlPasswordResetTokenRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class MySqlPasswordResetTokenRepositoryTest {

    private Connection connection;
    private PreparedStatement preparedStatement;
    private ResultSet resultSet;
    private MySqlPasswordResetTokenRepository repository;

    @BeforeEach
    void setUp() throws SQLException {
        connection = mock(Connection.class);
        preparedStatement = mock(PreparedStatement.class);
        resultSet = mock(ResultSet.class);
        repository = new MySqlPasswordResetTokenRepository(connection);

        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
    }

    @Test
    void savesTokenWithAllFieldsBoundWhenUsedAtIsNull() throws SQLException {
        PasswordResetToken token = PasswordResetToken.rebuiltToken(
                "token-1", "user-1", "hashed-token", Instant.parse("2026-08-10T12:00:00Z"), null);

        PasswordResetToken result = repository.save(token);

        verify(preparedStatement).setString(1, "token-1");
        verify(preparedStatement).setString(2, "user-1");
        verify(preparedStatement).setString(3, "hashed-token");
        verify(preparedStatement).setTimestamp(eq(4), any(Timestamp.class));
        verify(preparedStatement).setNull(5, Types.TIMESTAMP);
        verify(preparedStatement).executeUpdate();
        assertSame(token, result);
    }

    @Test
    void savesTokenWithUsedAtBoundWhenPresent() throws SQLException {
        PasswordResetToken token = PasswordResetToken.rebuiltToken(
                "token-1", "user-1", "hashed-token",
                Instant.parse("2026-08-10T12:00:00Z"), Instant.parse("2026-08-10T11:00:00Z"));

        repository.save(token);

        verify(preparedStatement).setTimestamp(eq(5), any(Timestamp.class));
        verify(preparedStatement, never()).setNull(eq(5), anyInt());
    }

    @Test
    void saveWrapsSqlExceptionInRepositoryException() throws SQLException {
        PasswordResetToken token = PasswordResetToken.rebuiltToken(
                "token-1", "user-1", "hashed-token", Instant.parse("2026-08-10T12:00:00Z"), null);
        when(preparedStatement.executeUpdate()).thenThrow(new SQLException("boom"));

        RepositoryException ex = assertThrows(RepositoryException.class, () -> repository.save(token));
        assertEquals("Erro ao salvar token de recuperação de senha.", ex.getMessage());
    }

    @Test
    void findByTokenHashReturnsMappedTokenWhenPresent() throws SQLException {
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true);
        when(resultSet.getString("id")).thenReturn("token-1");
        when(resultSet.getString("user_id")).thenReturn("user-1");
        when(resultSet.getString("token_hash")).thenReturn("hashed-token");
        when(resultSet.getTimestamp("expires_at"))
                .thenReturn(Timestamp.from(Instant.parse("2026-08-10T12:00:00Z")));
        when(resultSet.getTimestamp("used_at")).thenReturn(null);

        Optional<PasswordResetToken> result = repository.findByTokenHash("hashed-token");

        assertTrue(result.isPresent());
        assertEquals("token-1", result.get().getId());
        assertEquals("user-1", result.get().getUserId());
        assertEquals("hashed-token", result.get().getTokenHash());
        assertNull(result.get().getUsedAt());
        verify(preparedStatement).setString(1, "hashed-token");
    }

    @Test
    void findByTokenHashMapsUsedAtWhenPresent() throws SQLException {
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true);
        when(resultSet.getString("id")).thenReturn("token-1");
        when(resultSet.getString("user_id")).thenReturn("user-1");
        when(resultSet.getString("token_hash")).thenReturn("hashed-token");
        when(resultSet.getTimestamp("expires_at"))
                .thenReturn(Timestamp.from(Instant.parse("2026-08-10T12:00:00Z")));
        when(resultSet.getTimestamp("used_at"))
                .thenReturn(Timestamp.from(Instant.parse("2026-08-10T11:00:00Z")));

        Optional<PasswordResetToken> result = repository.findByTokenHash("hashed-token");

        assertTrue(result.isPresent());
        assertNotNull(result.get().getUsedAt());
    }

    @Test
    void findByTokenHashReturnsEmptyWhenNotFound() throws SQLException {
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(false);

        Optional<PasswordResetToken> result = repository.findByTokenHash("does-not-exist");

        assertTrue(result.isEmpty());
    }

    @Test
    void findByTokenHashWrapsSqlExceptionInRepositoryException() throws SQLException {
        when(preparedStatement.executeQuery()).thenThrow(new SQLException("boom"));

        RepositoryException ex = assertThrows(RepositoryException.class,
                () -> repository.findByTokenHash("hashed-token"));
        assertEquals("Erro ao buscar token de recuperação de senha", ex.getMessage());
    }

    @Test
    void markAsUsedExecutesUpdateWithGivenId() throws SQLException {
        repository.markAsUsed("token-1");

        verify(preparedStatement).setTimestamp(eq(1), any(Timestamp.class));
        verify(preparedStatement).setString(2, "token-1");
        verify(preparedStatement).executeUpdate();
    }

    @Test
    void markAsUsedWrapsSqlExceptionInRepositoryException() throws SQLException {
        when(preparedStatement.executeUpdate()).thenThrow(new SQLException("boom"));

        RepositoryException ex = assertThrows(RepositoryException.class,
                () -> repository.markAsUsed("token-1"));
        assertEquals("Erro ao marcar token como usado", ex.getMessage());
    }

    @Test
    void invalidateAllForUserExecutesUpdateWithGivenUserId() throws SQLException {
        repository.invalidateAllForUser("user-1");

        verify(preparedStatement).setTimestamp(eq(1), any(Timestamp.class));
        verify(preparedStatement).setString(2, "user-1");
        verify(preparedStatement).executeUpdate();
    }

    @Test
    void invalidateAllForUserWrapsSqlExceptionInRepositoryException() throws SQLException {
        when(preparedStatement.executeUpdate()).thenThrow(new SQLException("boom"));

        RepositoryException ex = assertThrows(RepositoryException.class,
                () -> repository.invalidateAllForUser("user-1"));
        assertEquals("Erro ao invalidar tokens de recuperação de senha", ex.getMessage());
    }
}
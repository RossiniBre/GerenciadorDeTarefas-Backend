package com.taskmanager.infrastructure.persistence.mysql;

import com.taskmanager.domain.security.PasswordResetToken;
import com.taskmanager.domain.repositories.PasswordResetTokenRepository;
import com.taskmanager.infrastructure.config.RepositoryException;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.Instant;
import java.util.Optional;

public class MySqlPasswordResetTokenRepository implements PasswordResetTokenRepository {

    private final Connection connection;

    public MySqlPasswordResetTokenRepository(Connection connection) {
        this.connection = connection;
    }

    @Override
    public PasswordResetToken save(PasswordResetToken token) {
        String sql = """
                INSERT INTO password_reset_tokens (id, user_id, token_hash, expires_at, used_at)
                VALUES (?, ?, ?, ?, ?)
                """;

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, token.getId());
            ps.setString(2, token.getUserId());
            ps.setString(3, token.getTokenHash());
            ps.setTimestamp(4, Timestamp.from(token.getExpiresAt()));
            setNullableTimestamp(ps, 5, token.getUsedAt());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RepositoryException("Erro ao salvar token de recuperação de senha.", e);
        }
        return token;
    }

    @Override
    public Optional<PasswordResetToken> findByTokenHash(String tokenHash) {
        String sql = "SELECT * FROM password_reset_tokens WHERE token_hash = ?";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, tokenHash);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                } else {
                    return Optional.empty();
                }
            }
        } catch (SQLException e) {
            throw new RepositoryException("Erro ao buscar token de recuperação de senha", e);
        }
    }

    @Override
    public void markAsUsed(String id) {
        String sql = "UPDATE password_reset_tokens SET used_at = ? WHERE id = ?";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setTimestamp(1, Timestamp.from(Instant.now()));
            ps.setString(2, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RepositoryException("Erro ao marcar token como usado", e);
        }
    }

    @Override
    public void invalidateAllForUser(String userId) {
        String sql = "UPDATE password_reset_tokens SET used_at = ? WHERE user_id = ? AND used_at IS NULL";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setTimestamp(1, Timestamp.from(Instant.now()));
            ps.setString(2, userId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RepositoryException("Erro ao invalidar tokens de recuperação de senha", e);
        }
    }

    private PasswordResetToken mapRow(ResultSet rs) throws SQLException {
        String id = rs.getString("id");
        String userId = rs.getString("user_id");
        String tokenHash = rs.getString("token_hash");
        Instant expiresAt = rs.getTimestamp("expires_at").toInstant();
        Instant usedAt = readNullableTimestamp(rs, "used_at");
        return PasswordResetToken.rebuiltToken(id, userId, tokenHash, expiresAt, usedAt);
    }

    private void setNullableTimestamp(PreparedStatement ps, int index, Instant value) throws SQLException {
        if (value != null) {
            ps.setTimestamp(index, Timestamp.from(value));
        } else {
            ps.setNull(index, Types.TIMESTAMP);
        }
    }

    private Instant readNullableTimestamp(ResultSet rs, String column) throws SQLException {
        Timestamp ts = rs.getTimestamp(column);
        return ts != null ? ts.toInstant() : null;
    }
}
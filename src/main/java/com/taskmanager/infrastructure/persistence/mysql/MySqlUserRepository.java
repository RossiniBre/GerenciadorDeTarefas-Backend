package com.taskmanager.infrastructure.persistence.mysql;

import com.taskmanager.domain.model.User;
import com.taskmanager.domain.repositories.UserRepository;
import com.taskmanager.infrastructure.config.RepositoryException;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

public class MySqlUserRepository implements UserRepository {

    private final Connection connection;

    public MySqlUserRepository(Connection connection) {
        this.connection = connection;
    }

    @Override
    public User save(User user) {
        String sql = """
            INSERT INTO users (id, email, username, password_hash, display_name)
            VALUES (?, ?, ?, ?, ?)
            ON DUPLICATE KEY UPDATE
                email = VALUES(email),
                username = VALUES(username),
                password_hash = VALUES(password_hash),
                display_name = VALUES(display_name)
            """;

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, user.getId());
            ps.setString(2, user.getEmail());
            ps.setString(3, user.getUsername());
            ps.setString(4, user.getPasswordHash());
            ps.setString(5, user.getDisplayName());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RepositoryException("Erro ao salvar usuário.", e);
        }
        return user;
    }

    @Override
    public Optional<User> findByUsername(String username) {
        String sql = "SELECT * FROM users WHERE username = ?";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, username);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String id = rs.getString("id");
                    String email = rs.getString("email");
                    String passwordHash = rs.getString("password_hash");
                    String displayName = rs.getString("display_name");
                    User user = User.rebuiltUser(id, email, username, passwordHash, displayName);
                    return Optional.of(user);
                } else {
                    return Optional.empty();
                }
            }
        } catch (SQLException e) {
            throw new RepositoryException("Erro ao buscar usuário por username", e);
        }
    }

    @Override
    public Optional<User> findByEmail(String email) {
        String sql = "SELECT * FROM users WHERE email = ?";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, email);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String id = rs.getString("id");
                    String username = rs.getString("username");
                    String passwordHash = rs.getString("password_hash");
                    String displayName = rs.getString("display_name");
                    User user = User.rebuiltUser(id, email, username, passwordHash, displayName);
                    return Optional.of(user);
                } else {
                    return Optional.empty();
                }
            }
        } catch (SQLException e) {
            throw new RepositoryException("Erro ao buscar usuário por email", e);
        }
    }

    @Override
    public Optional<User> findById(String id) {
        String sql = "SELECT * FROM users WHERE id = ?";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, id);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String email = rs.getString("email");
                    String username = rs.getString("username");
                    String passwordHash = rs.getString("password_hash");
                    String displayName = rs.getString("display_name");
                    User user = User.rebuiltUser(id, email, username, passwordHash, displayName);
                    return Optional.of(user);
                } else {
                    return Optional.empty();
                }
            }
        } catch (SQLException e) {
            throw new RepositoryException("Erro ao buscar usuário por id", e);
        }
    }

    @Override
    public void deleteAccount(String id) {
        String sql = "DELETE FROM users WHERE id = ?";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RepositoryException("Erro ao remover usuário", e);
        }
    }
}
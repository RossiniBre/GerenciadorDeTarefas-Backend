package com.taskmanager.infrastructure.persistence.mysql;

import com.taskmanager.domain.model.User;
import com.taskmanager.infrastructure.config.RepositoryException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class MySqlUserRepositoryTest {

    private Connection connection;
    private PreparedStatement preparedStatement;
    private ResultSet resultSet;
    private MySqlUserRepository repository;

    @BeforeEach
    void setUp() throws SQLException {
        connection = mock(Connection.class);
        preparedStatement = mock(PreparedStatement.class);
        resultSet = mock(ResultSet.class);
        repository = new MySqlUserRepository(connection);

        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
    }

    @Test
    void savesUserWithAllFieldsBound() throws SQLException {
        User user = User.rebuiltUser("user-1", "breno@email.com", "breno", "hashed-pw", "Owner");

        User result = repository.save(user);

        verify(preparedStatement).setString(1, "user-1");
        verify(preparedStatement).setString(2, "breno@email.com");
        verify(preparedStatement).setString(3, "breno");
        verify(preparedStatement).setString(4, "hashed-pw");
        verify(preparedStatement).setString(5, "Owner");
        verify(preparedStatement).executeUpdate();
        assertSame(user, result);
    }

    @Test
    void saveWrapsSqlExceptionInRepositoryException() throws SQLException {
        User user = User.rebuiltUser("user-1", "breno@email.com", "breno", "hashed-pw", "Owner");
        when(preparedStatement.executeUpdate()).thenThrow(new SQLException("boom"));

        RepositoryException ex = assertThrows(RepositoryException.class, () -> repository.save(user));
        assertEquals("Erro ao salvar usuário.", ex.getMessage());
    }

    @Test
    void findByUsernameReturnsUserWhenPresent() throws SQLException {
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true);
        when(resultSet.getString("id")).thenReturn("user-1");
        when(resultSet.getString("email")).thenReturn("breno@email.com");
        when(resultSet.getString("password_hash")).thenReturn("hashed-pw");
        when(resultSet.getString("display_name")).thenReturn("Owner");

        Optional<User> result = repository.findByUsername("breno");

        assertTrue(result.isPresent());
        assertEquals("user-1", result.get().getId());
        assertEquals("breno@email.com", result.get().getEmail());
        assertEquals("hashed-pw", result.get().getPasswordHash());
        verify(preparedStatement).setString(1, "breno");
    }

    @Test
    void findByUsernameReturnsEmptyWhenNotFound() throws SQLException {
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(false);

        Optional<User> result = repository.findByUsername("nao-existe");

        assertTrue(result.isEmpty());
    }

    @Test
    void findByUsernameWrapsSqlExceptionInRepositoryException() throws SQLException {
        when(preparedStatement.executeQuery()).thenThrow(new SQLException("boom"));

        RepositoryException ex = assertThrows(RepositoryException.class, () -> repository.findByUsername("breno"));
        assertEquals("Erro ao buscar usuário por username", ex.getMessage());
    }

    @Test
    void findByEmailReturnsUserWhenPresent() throws SQLException {
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true);
        when(resultSet.getString("id")).thenReturn("user-1");
        when(resultSet.getString("username")).thenReturn("breno");
        when(resultSet.getString("password_hash")).thenReturn("hashed-pw");
        when(resultSet.getString("display_name")).thenReturn("Owner");

        Optional<User> result = repository.findByEmail("breno@email.com");

        assertTrue(result.isPresent());
        assertEquals("breno", result.get().getUsername());
        assertEquals("breno@email.com", result.get().getEmail());
        verify(preparedStatement).setString(1, "breno@email.com");
    }

    @Test
    void findByEmailReturnsEmptyWhenNotFound() throws SQLException {
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(false);

        Optional<User> result = repository.findByEmail("nao-existe@email.com");

        assertTrue(result.isEmpty());
    }

    @Test
    void findByEmailWrapsSqlExceptionInRepositoryException() throws SQLException {
        when(preparedStatement.executeQuery()).thenThrow(new SQLException("boom"));

        RepositoryException ex = assertThrows(RepositoryException.class, () -> repository.findByEmail("breno@email.com"));
        assertEquals("Erro ao buscar usuário por email", ex.getMessage());
    }

    @Test
    void findByIdReturnsUserWhenPresent() throws SQLException {
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true);
        when(resultSet.getString("email")).thenReturn("breno@email.com");
        when(resultSet.getString("username")).thenReturn("breno");
        when(resultSet.getString("password_hash")).thenReturn("hashed-pw");
        when(resultSet.getString("display_name")).thenReturn("Owner");

        Optional<User> result = repository.findById("user-1");

        assertTrue(result.isPresent());
        assertEquals("breno", result.get().getUsername());
        assertEquals("breno@email.com", result.get().getEmail());
        verify(preparedStatement).setString(1, "user-1");
    }

    @Test
    void findByIdReturnsEmptyWhenNotFound() throws SQLException {
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(false);

        Optional<User> result = repository.findById("does-not-exist");

        assertTrue(result.isEmpty());
    }

    @Test
    void findByIdWrapsSqlExceptionInRepositoryException() throws SQLException {
        when(preparedStatement.executeQuery()).thenThrow(new SQLException("boom"));

        RepositoryException ex = assertThrows(RepositoryException.class, () -> repository.findById("user-1"));
        assertEquals("Erro ao buscar usuário por id", ex.getMessage());
    }

    @Test
    void deleteAccountExecutesDeleteWithGivenId() throws SQLException {
        repository.deleteAccount("user-1");

        verify(preparedStatement).setString(1, "user-1");
        verify(preparedStatement).executeUpdate();
    }

    @Test
    void deleteAccountWrapsSqlExceptionInRepositoryException() throws SQLException {
        when(preparedStatement.executeUpdate()).thenThrow(new SQLException("boom"));

        RepositoryException ex = assertThrows(RepositoryException.class, () -> repository.deleteAccount("user-1"));
        assertEquals("Erro ao remover usuário", ex.getMessage());
    }
}
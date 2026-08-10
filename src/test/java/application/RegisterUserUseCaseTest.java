package application;

import com.taskmanager.application.RegisterUserUseCase;
import com.taskmanager.domain.security.PasswordHasher;
import com.taskmanager.domain.model.User;
import com.taskmanager.domain.repositories.UserRepository;
import com.taskmanager.infrastructure.persistence.InMemoryUserRepository;
import com.taskmanager.infrastructure.security.Pbkdf2PasswordHasher;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import com.taskmanager.domain.exceptions.InvalidFieldException;
import com.taskmanager.domain.exceptions.DuplicateUsernameException;

class RegisterUserUseCaseTest {

    @Test
    void shouldRegisterUserSuccessfully() {
        // Arrange
        UserRepository repo = new InMemoryUserRepository();
        PasswordHasher hasher = new Pbkdf2PasswordHasher();
        RegisterUserUseCase useCase = new RegisterUserUseCase(repo, hasher);

        // Act
        User registeredUser = useCase.execute("joao123@example.com", "joao123", "minhaSenha123");

        // Assert
        assertNotNull(registeredUser.getId());
        assertEquals("joao123", registeredUser.getUsername());
        assertNotEquals("minhaSenha123", registeredUser.getPasswordHash());
    }

    @Test
    void shouldThrowExceptionWhenUsernameIsBlank() {
        // Arrange
        UserRepository repo = new InMemoryUserRepository();
        PasswordHasher hasher = new Pbkdf2PasswordHasher();
        RegisterUserUseCase useCase = new RegisterUserUseCase(repo, hasher);

        // Act & Assert
        assertThrows(InvalidFieldException.class, () ->
                useCase.execute("joao123@example.com", "", "minhaSenha123"));
    }

    @Test
    void shouldThrowExceptionWhenPasswordIsBlank() {
        // Arrange
        UserRepository repo = new InMemoryUserRepository();
        PasswordHasher hasher = new Pbkdf2PasswordHasher();
        RegisterUserUseCase useCase = new RegisterUserUseCase(repo, hasher);

        // Act & Assert
        assertThrows(InvalidFieldException.class, () ->
                useCase.execute("joao123@example.com", "joao123", ""));
    }

    @Test
    void shouldThrowExceptionWhenUsernameAlreadyExists() {
        // Arrange
        UserRepository repo = new InMemoryUserRepository();
        PasswordHasher hasher = new Pbkdf2PasswordHasher();
        RegisterUserUseCase useCase = new RegisterUserUseCase(repo, hasher);
        useCase.execute("joao123@example.com", "joao123", "minhaSenha123");

        // Act & Assert
        assertThrows(DuplicateUsernameException.class, () ->
                useCase.execute("outro@example.com", "joao123", "outraSenha456"));
    }
}
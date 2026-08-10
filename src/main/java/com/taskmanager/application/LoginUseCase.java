package com.taskmanager.application;

import com.taskmanager.domain.security.CredentialsValidator;
import com.taskmanager.domain.security.PasswordHasher;
import com.taskmanager.domain.model.User;
import com.taskmanager.domain.repositories.UserRepository;
import com.taskmanager.domain.exceptions.InvalidCredentialsException;
import java.util.Optional;

public class LoginUseCase {
    private final UserRepository userRepository;
    private final PasswordHasher passwordHasher;

    public LoginUseCase(UserRepository userRepository, PasswordHasher passwordHasher){
        if (userRepository == null){
            throw new IllegalArgumentException("Repositório de usuário é obrigatório!");
        }
        if (passwordHasher == null){
            throw new IllegalArgumentException("Serviço de hash de senha é obrigatório!");
        }
        this.userRepository = userRepository;
        this.passwordHasher = passwordHasher;
    }

    public User execute(String identifier, String password) {
        // 1
        CredentialsValidator.validate(identifier, password);

        // 2
        Optional<User> foundUser = userRepository.findByEmail(identifier)
                .or(() -> userRepository.findByUsername(identifier));
        if (foundUser.isEmpty()) {
            throw new InvalidCredentialsException();
        }

        User user = foundUser.get();

        // 3
        boolean correctPassword = passwordHasher.verify(password, user.getPasswordHash());

        // 4
        if (!correctPassword){
            throw new InvalidCredentialsException();
        }

        return user;
    }
}
package com.taskmanager.application;

import com.taskmanager.domain.exceptions.DuplicateEmailException;
import com.taskmanager.domain.exceptions.DuplicateUsernameException;
import com.taskmanager.domain.exceptions.UserNotFoundException;
import com.taskmanager.domain.model.User;
import com.taskmanager.domain.repositories.UserRepository;


public class UpdateUserProfileUseCase {
    private final UserRepository repo;

    public UpdateUserProfileUseCase(UserRepository repo){
        this.repo = repo;
    }

    public User execute(String id, String username, String email, String displayName){
        User user = repo.findById(id).orElseThrow(() -> new UserNotFoundException(id));

        if (username != null) {
            repo.findByUsername(username).ifPresent(existingUser -> {
                if (!existingUser.getId().equals(id)) {
                    throw new DuplicateUsernameException(username);
                }
            });
            user.changeUsername(username);
        }

        if (email != null){
            repo.findByEmail(email).ifPresent(existingUser -> {
                if(!existingUser.getId().equals(id)){
                    throw new DuplicateEmailException(email);
                }
            });
            user.changeEmail(email);
        }

        if (displayName != null) user.changeDisplayName(displayName);

        User updatedUser = repo.save(user);
        return updatedUser;
    }
}

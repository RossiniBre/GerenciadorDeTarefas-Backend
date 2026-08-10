package com.taskmanager.domain.repositories;

import com.taskmanager.domain.model.User;

import java.util.Optional;

public interface UserRepository {
    User save(User user);
    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);
    Optional<User> findById(String id);
    void deleteAccount(String id);
}
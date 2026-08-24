package com.taskmanager.domain.exceptions;

public class UserNotFoundException extends DomainException {
    public UserNotFoundException(String id) {
        super("Usuário não encontrado: " + id);
    }
}

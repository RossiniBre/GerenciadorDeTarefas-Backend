package com.taskmanager.domain.exceptions;

public class DuplicateEmailException extends DomainException {

    public DuplicateEmailException(String email) {
        super("O email '" + email + "' já está em uso.");
    }
}
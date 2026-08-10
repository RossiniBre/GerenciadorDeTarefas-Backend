package com.taskmanager.domain.exceptions;

public class InvalidOrExpiredTokenException extends DomainException {
    public InvalidOrExpiredTokenException() {
        super("Token inválido ou expirado.");
    }
}
package com.taskmanager.domain.security;

import com.taskmanager.domain.exceptions.InvalidFieldException;

public class CredentialsValidator {
    public static void validate(String identifier, String password) {
        if (identifier == null || identifier.isBlank()){
            throw new InvalidFieldException("Email ou nome de usuário é obrigatório!");
        }
        if (password == null || password.isBlank()){
            throw new InvalidFieldException("Senha é obrigatória!");
        }
    }
}
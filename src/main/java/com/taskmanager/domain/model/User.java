package com.taskmanager.domain.model;

import java.util.UUID;

public class User {
    private String id;
    private String email;
    private String username;
    private String passwordHash;

    private User(String id, String email, String username, String passwordHash){
        if (email == null || email.isBlank()){
            throw new IllegalArgumentException("Email é obrigatório!");
        }
        if (!email.contains("@")){
            throw new IllegalArgumentException("Email inválido!");
        }
        if (username == null || username.isBlank()){
            throw new IllegalArgumentException("Nome de usuário é obrigatório!");
        }
        if (passwordHash == null || passwordHash.isBlank()){
            throw new IllegalArgumentException("Senha é obrigatória!");
        }
        this.id = id;
        this.email = email;
        this.username = username;
        this.passwordHash = passwordHash;
    }

    public static User newUser(String email, String username, String passwordHash){
        return new User(UUID.randomUUID().toString(), email, username, passwordHash);
    }

    public static User rebuiltUser(String id, String email, String username, String passwordHash){
        return new User(id, email, username, passwordHash);
    }

    //getters
    public String getId(){ return id; }
    public String getEmail(){ return email; }
    public String getPasswordHash(){ return passwordHash; }
    public String getUsername(){ return username; }

}
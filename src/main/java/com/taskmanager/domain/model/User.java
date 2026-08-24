package com.taskmanager.domain.model;

import java.util.UUID;
import java.util.regex.Pattern;

public class User {
    private String id;
    private String email;
    private String username;
    private String passwordHash;
    private String displayName;

    // defining real emails
    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    private User(String id, String email, String username, String passwordHash, String displayName){
        if (passwordHash == null || passwordHash.isBlank()){
            throw new IllegalArgumentException("Senha é obrigatória!");
        }

        this.id = id;
        this.email = validateEmail(email);
        this.username = validateUsername(username);
        this.passwordHash = passwordHash;
        this.displayName = validateDisplayName(displayName);
    }

    private static String validateEmail(String email){
        if (email == null || email.isBlank()){
            throw new IllegalArgumentException("Email é obrigatório!");
        }
        if (!EMAIL_PATTERN.matcher(email).matches()){
            throw new IllegalArgumentException("Email inválido!");
        }
        return email;
    }

    private static String validateUsername(String username){
        if (username == null || username.isBlank()){
            throw new IllegalArgumentException("Nome de usuário é obrigatório!");
        }
        return username;
    }

    private static String validateDisplayName(String displayName){
        if (displayName == null || displayName.isBlank()){
            throw new IllegalArgumentException("Nome de exibição é obrigatório");
        }
        return displayName;
    }

    public static User newUser(String email, String username, String passwordHash, String displayName){
        return new User(UUID.randomUUID().toString(), email, username, passwordHash, displayName);
    }

    public static User rebuiltUser(String id, String email, String username, String passwordHash, String displayName){
        return new User(id, email, username, passwordHash, displayName);
    }

    //getters
    public String getId(){ return id; }
    public String getEmail(){ return email; }
    public String getPasswordHash(){ return passwordHash; }
    public String getUsername(){ return username; }
    public String getDisplayName(){ return displayName; }

    public void changeEmail(String newEmail) {
        this.email = validateEmail(newEmail);
    }

    public void changeUsername(String newUsername) {
        this.username = validateUsername(newUsername);
    }

    public void changeDisplayName(String newDisplayName) {
        this.displayName = validateDisplayName(newDisplayName);
    }
}
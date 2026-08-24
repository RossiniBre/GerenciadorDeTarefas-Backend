package com.taskmanager.infrastructure.http.dto;

public class RegisterUserResponse {
    public String id;
    public String username;
    public String email;
    public String displayName;

    public RegisterUserResponse(String id, String username, String email, String displayName) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.displayName = displayName;
    }
}
package com.taskmanager.infrastructure.http.dto;

public class UserResponse {
    public String id;
    public String username;
    public String email;
    public String displayName;

    public UserResponse(String id, String username, String email, String displayName) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.displayName = displayName;
    }
}


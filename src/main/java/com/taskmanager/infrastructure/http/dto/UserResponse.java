package com.taskmanager.infrastructure.http.dto;

public class UserResponse {
    public String id;
    public String username;

    public UserResponse(String id, String username) {
        this.id = id;
        this.username = username;
    }
}


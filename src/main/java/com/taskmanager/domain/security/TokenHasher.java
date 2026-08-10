package com.taskmanager.domain.security;

public interface TokenHasher {
    String hash(String rawToken);
}
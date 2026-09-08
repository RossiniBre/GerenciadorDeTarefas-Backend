package com.taskmanager.domain.assistant;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

public record ActiveTask(
        UUID id,
        String title,
        LocalDateTime dueDate,
        Instant resolvedAt,
        Instant expiresAt
) {
    private static final long DEFAULT_TTL_SECONDS = 300;

    public static ActiveTask resolveNow(UUID id, String title, LocalDateTime dueDate) {
        Instant now = Instant.now();
        return new ActiveTask(id, title, dueDate, now, now.plusSeconds(DEFAULT_TTL_SECONDS));
    }

    public ActiveTask renew() {
        Instant now = Instant.now();
        return new ActiveTask(id, title, dueDate, resolvedAt, now.plusSeconds(DEFAULT_TTL_SECONDS));
    }

    @JsonIgnore
    public boolean isExpired() {
        return Instant.now().isAfter(expiresAt);
    }
}
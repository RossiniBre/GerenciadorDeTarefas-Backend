package com.taskmanager.infrastructure.http;

import com.taskmanager.domain.exceptions.*;
import com.taskmanager.infrastructure.assistant.AssistantRateLimitExceededException;
import com.taskmanager.infrastructure.assistant.DailyQuotaExceededException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.NoHandlerFoundException;

import java.time.format.DateTimeFormatter;
import java.util.Map;

@RestControllerAdvice
public class ApiExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);
    private static final DateTimeFormatter HOUR_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<Map<String, String>> handleInvalidCredentials(InvalidCredentialsException e) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("error", e.getMessage()));
    }

    @ExceptionHandler(DuplicateUsernameException.class)
    public ResponseEntity<Map<String, String>> handleDuplicateUsername(DuplicateUsernameException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of("error", e.getMessage()));
    }

    @ExceptionHandler(TooManyAttemptsException.class)
    public ResponseEntity<Map<String, String>> handleTooManyAttempts(TooManyAttemptsException e) {
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .body(Map.of("error", e.getMessage()));
    }

    @ExceptionHandler(UnauthorizedTaskAccessException.class)
    public ResponseEntity<Map<String, String>> handleUnauthorizedTaskAccess(UnauthorizedTaskAccessException e) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Map.of("error", e.getMessage()));
    }

    @ExceptionHandler(TaskSuggestionNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleTaskSuggestionNotFound(TaskSuggestionNotFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", e.getMessage()));
    }

    @ExceptionHandler(DailyQuotaExceededException.class)
    public ResponseEntity<Map<String, String>> handleDailyQuotaExceeded(DailyQuotaExceededException e) {
        String hour = e.getResetsAt().format(HOUR_FORMATTER);
        String message = "Você está sem Tokens até " + hour;
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .body(Map.of("error", message));
    }

    @ExceptionHandler(AssistantRateLimitExceededException.class)
    public ResponseEntity<Map<String, String>> handleAssistantRateLimitExceeded(
            AssistantRateLimitExceededException e) {

        String message = "Não foi possível processar sua mensagem no momento. Tente novamente.";

        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .body(Map.of("error", message));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleIllegalArgument(IllegalArgumentException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", "Valor inválido: " + e.getMessage()));
    }

    @ExceptionHandler(DomainException.class)
    public ResponseEntity<Map<String, String>> handleDomain(DomainException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", e.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleGeneric(Exception e) {
        log.error("Internal server error", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Internal server error"));
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<Map<String, String>> handleMethodNotSupported(HttpRequestMethodNotSupportedException e) {
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
                .body(Map.of("error", "Método não suportado para esse recurso."));
    }

    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<Map<String, String>> handleNoHandlerFound(NoHandlerFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", "Recurso não encontrado."));
    }

    @ExceptionHandler(DuplicateEmailException.class)
    public ResponseEntity<Map<String, String>> handleDuplicateEmail(DuplicateEmailException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of("error", e.getMessage()));
    }

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleUserNotFound(UserNotFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", e.getMessage()));
    }
}
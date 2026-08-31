package com.taskmanager.infrastructure.assistant;

public class InvalidIntentResponseException extends AssistantRequestFailedException {

    public InvalidIntentResponseException(String message) {
        super(message);
    }
}
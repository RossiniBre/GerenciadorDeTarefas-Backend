package com.taskmanager.infrastructure.assistant;

public class UnusableModelResponseException extends AssistantRequestFailedException {

    public UnusableModelResponseException(String message) {
        super(message);
    }
}
package com.taskmanager.infrastructure.assistant;

public class ResponseTruncatedException extends AssistantRequestFailedException {

    public ResponseTruncatedException(String message) {
        super(message);
    }
}
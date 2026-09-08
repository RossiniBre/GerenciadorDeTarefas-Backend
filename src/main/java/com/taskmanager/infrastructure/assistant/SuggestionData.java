package com.taskmanager.infrastructure.assistant;

public record SuggestionData(
        String action,
        String targetTaskId,
        boolean referenceActiveTask,
        String title,
        String description,
        String priority,
        String category,
        String dueDate,
        boolean keepDueDate,
        String dueTime,
        String reminderDate
) {}
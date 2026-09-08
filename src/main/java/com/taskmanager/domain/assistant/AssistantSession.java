package com.taskmanager.domain.assistant;

import java.util.List;

public record AssistantSession(
        List<Message> conversationHistory,
        List<TaskSuggestion> pendingSuggestions,
        ActiveTask activeTask
) {
    public ActiveTask validActiveTask() {
        if (activeTask == null || activeTask.isExpired()) {
            return null;
        }
        return activeTask;
    }
}
package com.taskmanager.domain.assistant;

import java.util.List;

public record AssistantContext(
        List<Message> conversationHistory,
        List<TaskSuggestion> pendingSuggestions,
        ActiveTask activeTask,
        String requesterId
) {
    public ActiveTask validActiveTask() {
        if (activeTask == null || activeTask.isExpired()) {
            return null;
        }
        return activeTask;
    }
}
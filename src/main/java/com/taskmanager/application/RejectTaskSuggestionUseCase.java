package com.taskmanager.application;

import com.taskmanager.domain.assistant.AssistantSession;
import com.taskmanager.domain.assistant.TaskSuggestion;
import com.taskmanager.domain.exceptions.TaskSuggestionNotFoundException;
import com.taskmanager.domain.repositories.AssistantSessionRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class RejectTaskSuggestionUseCase {

    private final AssistantSessionRepository assistantSessionRepository;

    public RejectTaskSuggestionUseCase(AssistantSessionRepository assistantSessionRepository) {
        this.assistantSessionRepository = assistantSessionRepository;
    }

    public void execute(String token, UUID suggestionId) {
        AssistantSession session = assistantSessionRepository.find(token)
                .orElseThrow(TaskSuggestionNotFoundException::new);

        TaskSuggestion suggestion = session.pendingSuggestions().stream()
                .filter(s -> s.id().equals(suggestionId))
                .findFirst()
                .orElseThrow(TaskSuggestionNotFoundException::new);

        List<TaskSuggestion> remaining = new ArrayList<>(session.pendingSuggestions());
        remaining.remove(suggestion);

        assistantSessionRepository.save(
                token,
                new AssistantSession(
                        session.conversationHistory(),
                        remaining,
                        session.activeTask()
                )
        );
    }
}
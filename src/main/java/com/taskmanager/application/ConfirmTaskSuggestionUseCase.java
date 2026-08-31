package com.taskmanager.application;

import com.taskmanager.domain.assistant.AssistantSession;
import com.taskmanager.domain.assistant.TaskSuggestion;
import com.taskmanager.domain.exceptions.TaskSuggestionNotFoundException;
import com.taskmanager.domain.model.User;
import com.taskmanager.domain.repositories.AssistantSessionRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ConfirmTaskSuggestionUseCase {

    private final AssistantSessionRepository assistantSessionRepository;
    private final CreateTaskUseCase createTaskUseCase;
    private final UpdateTaskDetailsUseCase updateTaskDetailsUseCase;
    private final DeleteTaskUseCase deleteTaskUseCase;
    private final StartTaskUseCase startTaskUseCase;
    private final CompleteTaskUseCase completeTaskUseCase;

    public ConfirmTaskSuggestionUseCase(
            AssistantSessionRepository assistantSessionRepository,
            CreateTaskUseCase createTaskUseCase,
            UpdateTaskDetailsUseCase updateTaskDetailsUseCase,
            DeleteTaskUseCase deleteTaskUseCase,
            StartTaskUseCase startTaskUseCase,
            CompleteTaskUseCase completeTaskUseCase
    ) {
        this.assistantSessionRepository = assistantSessionRepository;
        this.createTaskUseCase = createTaskUseCase;
        this.updateTaskDetailsUseCase = updateTaskDetailsUseCase;
        this.deleteTaskUseCase = deleteTaskUseCase;
        this.startTaskUseCase = startTaskUseCase;
        this.completeTaskUseCase = completeTaskUseCase;
    }

    public void execute(String token, User loggedUser, UUID suggestionId) {
        String requesterId = loggedUser.getId();

        AssistantSession session = assistantSessionRepository.find(token)
                .orElseThrow(TaskSuggestionNotFoundException::new);

        TaskSuggestion suggestion = session.pendingSuggestions().stream()
                .filter(s -> s.id().equals(suggestionId))
                .findFirst()
                .orElseThrow(TaskSuggestionNotFoundException::new);

        switch (suggestion) {
            case TaskSuggestion.Create s ->
                    createTaskUseCase.execute(
                            s.title(),
                            s.description(),
                            loggedUser,
                            s.priority(),
                            s.category(),
                            s.dueDate(),
                            s.reminderDate()
                    );

            case TaskSuggestion.Update s ->
                    updateTaskDetailsUseCase.execute(
                            s.title(),
                            s.description(),
                            s.priority(),
                            s.category(),
                            s.dueDate(),
                            s.reminderDate(),
                            s.targetTaskId(),
                            requesterId
                    );

            case TaskSuggestion.Delete s ->
                    deleteTaskUseCase.execute(
                            s.targetTaskId(),
                            requesterId
                    );

            case TaskSuggestion.Start s ->
                    startTaskUseCase.execute(
                            s.targetTaskId(),
                            loggedUser
                    );

            case TaskSuggestion.Complete s ->
                    completeTaskUseCase.execute(
                            s.targetTaskId(),
                            loggedUser
                    );
        }

        List<TaskSuggestion> remaining = new ArrayList<>(session.pendingSuggestions());
        remaining.remove(suggestion);

        assistantSessionRepository.save(
                token,
                new AssistantSession(
                        session.conversationHistory(),
                        remaining
                )
        );
    }
}
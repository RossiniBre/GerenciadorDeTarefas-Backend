package com.taskmanager.application;

import com.taskmanager.application.ListTasksUseCase.TaskFilter;
import com.taskmanager.domain.assistant.*;
import com.taskmanager.domain.model.Task;
import com.taskmanager.domain.repositories.AssistantSessionRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class SendMessageToAssistantUseCase {

    private final AssistantSessionRepository assistantSessionRepository;
    private final TaskAssistant taskAssistant;
    private final ListTasksUseCase listTasksUseCase;

    public SendMessageToAssistantUseCase(
            AssistantSessionRepository assistantSessionRepository,
            TaskAssistant taskAssistant,
            ListTasksUseCase listTasksUseCase
    ) {
        this.assistantSessionRepository = assistantSessionRepository;
        this.taskAssistant = taskAssistant;
        this.listTasksUseCase = listTasksUseCase;
    }

    public AssistantResponse execute(String token, String userMessageText) {

        AssistantSession session =
                assistantSessionRepository
                        .find(token)
                        .orElse(new AssistantSession(
                                new ArrayList<>(),
                                new ArrayList<>(),
                                null // activeTask
                        ));

        List<Message> history = new ArrayList<>(session.conversationHistory());
        history.add(new Message(MessageAuthor.USER, userMessageText));

        List<TaskSuggestion> pendingSuggestions = new ArrayList<>(session.pendingSuggestions());

        AssistantResponse response =
                taskAssistant.process(
                        new AssistantContext(
                                history,
                                pendingSuggestions,
                                session.validActiveTask(),
                                token
                        )
                );

        String assistantMessageText;
        List<UUID> suggestionIds = List.of();
        ActiveTask newActiveTask = session.validActiveTask();

        switch (response) {

            case AssistantResponse.ValidSuggestions valid -> {
                pendingSuggestions.addAll(valid.suggestions());

                List<Task> tasks = listTasksUseCase.execute(token, TaskFilter.none());
                assistantMessageText = summarize(valid.suggestions(), tasks);

                suggestionIds = valid.suggestions().stream().map(TaskSuggestion::id).toList();

                newActiveTask = resolveNewActiveTask(valid.suggestions(), tasks, session.validActiveTask());
            }

            case AssistantResponse.MissingInfos missing -> assistantMessageText = missing.question();
            case AssistantResponse.OutOfScope outOfScope -> assistantMessageText = outOfScope.reason();
            case AssistantResponse.InformationalAnswer informational -> assistantMessageText = informational.answer();
        }

        history.add(new Message(MessageAuthor.ASSISTANT, assistantMessageText, suggestionIds));

        assistantSessionRepository.save(
                token,
                new AssistantSession(history, pendingSuggestions, newActiveTask)
        );

        return response;
    }

    private ActiveTask resolveNewActiveTask(List<TaskSuggestion> suggestions, List<Task> tasks, ActiveTask current) {
        for (int i = suggestions.size() - 1; i >= 0; i--) {
            String targetId = switch (suggestions.get(i)) {
                case TaskSuggestion.Update s -> s.targetTaskId();
                case TaskSuggestion.Delete s -> s.targetTaskId();
                case TaskSuggestion.Start s -> s.targetTaskId();
                case TaskSuggestion.Complete s -> s.targetTaskId();
                case TaskSuggestion.Create ignored -> null; // ainda não existe, ver nota acima
            };

            if (targetId != null) {
                return tasks.stream()
                        .filter(t -> t.getId().equals(targetId))
                        .findFirst()
                        .map(t -> current != null && targetId.equals(current.id().toString())
                                ? current.renew()
                                : ActiveTask.resolveNow(UUID.fromString(t.getId()), t.getTitle(), t.getDueDate()))
                        .orElse(current); // não achou a tarefa (raro) — mantém o que já tinha
            }
        }
        return current; // nenhuma suggestion desta rodada referenciou uma tarefa concreta
    }


    private String summarize(List<TaskSuggestion> suggestions, List<Task> tasks) {
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < suggestions.size(); i++) {
            sb.append(describe(suggestions.get(i), tasks));
            if (i < suggestions.size() - 1) {
                sb.append("; ");
            }
        }

        return sb.toString();
    }


    private String describe(
            TaskSuggestion suggestion,
            List<Task> tasks
    ) {

        return switch (suggestion) {

            case TaskSuggestion.Create s ->
                    "Sugeri criar a tarefa: \""
                            + s.title()
                            + "\". Aguardando confirmação.";


            case TaskSuggestion.Update s ->
                    "Sugeri atualizar a tarefa \""
                            + findTitle(s.targetTaskId(), tasks)
                            + "\". Aguardando confirmação.";


            case TaskSuggestion.Delete s ->
                    "Sugeri apagar a tarefa \""
                            + findTitle(s.targetTaskId(), tasks)
                            + "\". Aguardando confirmação.";


            case TaskSuggestion.Start s ->
                    "Sugeri iniciar a tarefa \""
                            + findTitle(s.targetTaskId(), tasks)
                            + "\". Aguardando confirmação.";


            case TaskSuggestion.Complete s ->
                    "Sugeri concluir a tarefa \""
                            + findTitle(s.targetTaskId(), tasks)
                            + "\". Aguardando confirmação.";
        };
    }


    private String findTitle(
            String taskId,
            List<Task> tasks
    ) {
        return tasks.stream()
                .filter(task -> task.getId().equals(taskId))
                .map(Task::getTitle)
                .findFirst()
                .orElse("tarefa desconhecida");
    }
}
package com.taskmanager.domain.assistant;

import com.taskmanager.application.ListTasksUseCase;
import com.taskmanager.domain.model.Task;
import com.taskmanager.domain.model.TaskCategory;
import com.taskmanager.domain.model.TaskPriority;
import com.taskmanager.infrastructure.assistant.IntentExtractionResult;
import com.taskmanager.infrastructure.assistant.SuggestionData;
import com.taskmanager.infrastructure.http.json.JsonMapper;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public class TaskAssistantOrchestrator implements TaskAssistant {

    private final IntentExtractor intentExtractor;
    private final AnswerFormatter answerFormatter;
    private final TaskFilterResolver taskFilterResolver;
    private final ListTasksUseCase listTasksUseCase;
    private final JsonMapper jsonMapper;
    private final String systemInstructions;
    private final String answerFormatterInstructions;
    private final Clock clock;

    public TaskAssistantOrchestrator(
            IntentExtractor intentExtractor,
            AnswerFormatter answerFormatter,
            TaskFilterResolver taskFilterResolver,
            ListTasksUseCase listTasksUseCase,
            JsonMapper jsonMapper,
            String systemInstructions, String answerFormatterInstructions,
            Clock clock
    ) {
        if (intentExtractor == null) {
            throw new IllegalArgumentException("IntentExtractor é obrigatório!");
        }
        if (answerFormatter == null) {
            throw new IllegalArgumentException("AnswerFormatter é obrigatório!");
        }
        if (taskFilterResolver == null) {
            throw new IllegalArgumentException("TaskFilterResolver é obrigatório!");
        }
        if (listTasksUseCase == null) {
            throw new IllegalArgumentException("ListTasksUseCase é obrigatório!");
        }
        if (jsonMapper == null) {
            throw new IllegalArgumentException("JsonMapper é obrigatório!");
        }
        if (systemInstructions == null || systemInstructions.isBlank()) {
            throw new IllegalArgumentException("Instruções do sistema são obrigatórias!");
        }
        if (answerFormatterInstructions == null || answerFormatterInstructions.isBlank()) {
            throw new IllegalArgumentException("Instruções do AnswerFormatter são obrigatórias!");
        }
        if (clock == null) {
            throw new IllegalArgumentException("Clock é obrigatório!");
        }

        this.intentExtractor = intentExtractor;
        this.answerFormatter = answerFormatter;
        this.taskFilterResolver = taskFilterResolver;
        this.listTasksUseCase = listTasksUseCase;
        this.jsonMapper = jsonMapper;
        this.systemInstructions = systemInstructions;
        this.answerFormatterInstructions = answerFormatterInstructions;
        this.clock = clock;
    }

    @Override
    public AssistantResponse process(AssistantContext context) {
        List<Task> currentTasks = listTasksUseCase.execute(context.requesterId(), ListTasksUseCase.TaskFilter.none());
        String enrichedPrompt = buildEnrichedContext(context, currentTasks);
        String rawJson = intentExtractor.extract(systemInstructions, enrichedPrompt);

        try {
            IntentExtractionResult result = jsonMapper.fromJson(stripCodeFences(rawJson), IntentExtractionResult.class);

            if (result.type() == null || result.type().isBlank()) {
                return new AssistantResponse.OutOfScope(
                        "Não consegui interpretar sua mensagem. Pode reformular?");
            }

            return switch (result.type()) {
                case "SUGGESTIONS" -> new AssistantResponse.ValidSuggestions(toSuggestions(result.suggestions()));
                case "LISTING" -> handleListing(result.filter(), context.requesterId());
                case "MISSING_INFO" -> new AssistantResponse.MissingInfos(result.question());
                case "OUT_OF_SCOPE" -> new AssistantResponse.OutOfScope(result.reason());
                case "INFORMATIONAL" -> new AssistantResponse.InformationalAnswer(result.answer());
                default -> new AssistantResponse.OutOfScope(
                        "Não consegui interpretar sua mensagem. Pode reformular?");
            };

        } catch (RuntimeException e) {
            System.out.println("Falha ao interpretar resposta da IA. rawJson=" + rawJson);
            e.printStackTrace();
            return new AssistantResponse.OutOfScope(
                    "Não consegui interpretar sua mensagem. Pode reformular?");
        }
    }

    private String buildEnrichedContext(AssistantContext context, List<Task> currentTasks) {
        List<Message> history = context.conversationHistory();
        List<Message> priorMessages = history.isEmpty() ? List.of() : history.subList(0, history.size() - 1);
        Message currentMessage = history.isEmpty() ? null : history.get(history.size() - 1);

        StringBuilder sb = new StringBuilder();

        sb.append("=== Data atual ===\n\n");
        sb.append(LocalDate.now(clock)).append("\n\n");

        sb.append("=== Histórico ===\n\n");
        if (priorMessages.isEmpty()) {
            sb.append("Nenhum.\n\n");
        } else {
            for (Message message : priorMessages) {
                String author = message.author() == MessageAuthor.USER ? "Usuário" : "Assistente";
                sb.append(author).append(":\n").append(message.content()).append("\n\n");
            }
        }

        sb.append("=== Sugestões pendentes ===\n\n");
        List<TaskSuggestion> pending = context.pendingSuggestions();
        if (pending.isEmpty()) {
            sb.append("Nenhuma.\n\n");
        } else {
            int i = 1;
            for (TaskSuggestion suggestion : pending) {
                sb.append(i++).append(".\n").append(describeSuggestion(suggestion)).append("\n\n");
            }
        }

        sb.append("=== Tarefas existentes ===\n\n");
        if (currentTasks.isEmpty()) {
            sb.append("Nenhuma.\n\n");
        } else {
            for (Task task : currentTasks) {
                sb.append("- id=").append(task.getId())
                        .append(", título=\"").append(task.getTitle()).append("\"")
                        .append(", status=").append(task.getStatus())
                        .append(", prioridade=").append(task.getPriority());

                if (task.getDueDate() != null) {
                    sb.append(", prazo=").append(task.getDueDate());
                }

                sb.append("\n");
            }
            sb.append("\n");
        }

        sb.append("=== Nova mensagem ===\n\n");
        sb.append(currentMessage != null ? currentMessage.content() : "");

        return sb.toString();
    }

    private String describeSuggestion(TaskSuggestion suggestion) {
        return switch (suggestion) {
            case TaskSuggestion.Create s -> """
        Ação: CREATE
        Título: %s
        Prioridade: %s
        Categoria: %s
        Prazo: %s
        Lembrete: %s""".formatted(s.title(), s.priority(), s.category(),
                    s.dueDate() != null ? s.dueDate() : "não definido",
                    s.reminderDate() != null ? s.reminderDate() : "não definido");

            case TaskSuggestion.Update s -> {
                StringBuilder b = new StringBuilder("Ação: UPDATE\nTarefa alvo: ").append(s.targetTaskId()).append("\n");
                if (s.title() != null) b.append("Novo título: ").append(s.title()).append("\n");
                if (s.description() != null) b.append("Nova descrição: ").append(s.description()).append("\n");
                if (s.priority() != null) b.append("Nova prioridade: ").append(s.priority()).append("\n");
                if (s.category() != null) b.append("Nova categoria: ").append(s.category()).append("\n");
                yield b.toString().stripTrailing();
            }

            case TaskSuggestion.Delete s ->
                    "Ação: DELETE\nTarefa alvo: " + s.targetTaskId();

            case TaskSuggestion.Start s ->
                    "Ação: START\nTarefa alvo: " + s.targetTaskId();

            case TaskSuggestion.Complete s ->
                    "Ação: COMPLETE\nTarefa alvo: " + s.targetTaskId();
        };
    }

    private String stripCodeFences(String raw) {
        String trimmed = raw.strip();
        if (trimmed.startsWith("```")) {
            int firstNewline = trimmed.indexOf('\n');
            if (firstNewline != -1) {
                trimmed = trimmed.substring(firstNewline + 1);
            }
            int lastFence = trimmed.lastIndexOf("```");
            if (lastFence != -1) {
                trimmed = trimmed.substring(0, lastFence);
            }
        }
        return trimmed.strip();
    }

    private AssistantResponse handleListing(TaskFilterIntent filterIntent, String requesterId) {
        ListTasksUseCase.TaskFilter filter = taskFilterResolver.resolve(filterIntent);
        List<Task> tasks = listTasksUseCase.execute(requesterId, filter);

        String formatted;
        try {
            formatted = answerFormatter.format(answerFormatterInstructions, jsonMapper.toJson(tasks));

            if (isInvalidFormatterResponse(formatted, tasks)) {
                System.out.println("Resposta inválida do AnswerFormatter, usando fallback. raw=" + formatted);
                formatted = fallbackFormat(tasks);
            }
        } catch (RuntimeException e) {
            System.out.println("AnswerFormatter falhou completamente, usando fallback. erro=" + e.getMessage());
            formatted = fallbackFormat(tasks);
        }

        return new AssistantResponse.InformationalAnswer(formatted);
    }

    private String fallbackFormat(List<Task> tasks) {
        if (tasks.isEmpty()) {
            return "Você não tem nenhuma tarefa nessa consulta.";
        }

        StringBuilder sb = new StringBuilder("Aqui está sua lista de tarefas:\n\n");
        for (Task task : tasks) {
            sb.append("- ").append(task.getTitle());

            if (task.getDueDate() != null) {
                sb.append(" (vence em ").append(task.getDueDate().toLocalDate()).append(")");
            }

            sb.append("\n");
        }

        return sb.toString();
    }

    private boolean isInvalidFormatterResponse(String formatted, List<Task> tasks) {
        if (formatted == null || formatted.isBlank()) {
            return true;
        }
        String normalized = formatted.strip();
        if (normalized.regionMatches(true, 0, "User Safety:", 0, "User Safety:".length())) {
            return true;
        }
        if (normalized.length() < 3) {
            return true;
        }
        if (!tasks.isEmpty()) {
            boolean containsAnyTitle = tasks.stream()
                    .anyMatch(task -> normalized.contains(task.getTitle()));
            if (!containsAnyTitle) {
                return true;
            }
        }
        return false;
    }

    private List<TaskSuggestion> toSuggestions(List<SuggestionData> raw) {
        return raw.stream().map(this::toSuggestion).toList();
    }

    private TaskSuggestion toSuggestion(SuggestionData data) {
        UUID id = UUID.randomUUID();
        LocalDateTime dueDate = parseDateOrNull(data.dueDate());
        LocalDateTime reminderDate = parseDateOrNull(data.reminderDate());

        return switch (data.action()) {
            case "CREATE" -> new TaskSuggestion.Create(id, data.title(), data.description(),
                    EnumParser.parse(TaskPriority.class, data.priority()),
                    EnumParser.parse(TaskCategory.class, data.category()),
                    dueDate, reminderDate);
            case "UPDATE" -> new TaskSuggestion.Update(id, data.targetTaskId(), data.title(), data.description(),
                    EnumParser.parse(TaskPriority.class, data.priority()),
                    EnumParser.parse(TaskCategory.class, data.category()),
                    dueDate, reminderDate);
            case "DELETE" -> new TaskSuggestion.Delete(id, data.targetTaskId());
            case "START" -> new TaskSuggestion.Start(id, data.targetTaskId());
            case "COMPLETE" -> new TaskSuggestion.Complete(id, data.targetTaskId());
            default -> throw new IllegalStateException("Ação desconhecida: " + data.action());
        };
    }

    private LocalDateTime parseDateOrNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return LocalDateTime.parse(value);
        } catch (java.time.format.DateTimeParseException e) {
            System.out.println("Data inválida retornada pela IA, ignorando: " + value);
            return null;
        }
    }

    private String lastUserMessage(List<Message> history) {
        for (int i = history.size() - 1; i >= 0; i--) {
            Message message = history.get(i);
            if (message.author() == MessageAuthor.USER) {
                return message.content();
            }
        }
        throw new IllegalArgumentException("Histórico não contém nenhuma mensagem do usuário!");
    }
}
package domain;

import com.taskmanager.application.ListTasksUseCase;
import com.taskmanager.domain.assistant.*;
import com.taskmanager.domain.model.Task;
import com.taskmanager.domain.repositories.TaskRepository;
import com.taskmanager.infrastructure.http.json.GsonJsonMapper;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class TaskAssistantOrchestratorErrorHandlingTest {

    private TaskAssistantOrchestrator orchestratorWith(String fakeExtractorResponse) {
        IntentExtractor fakeExtractor = (instructions, userMessage) -> fakeExtractorResponse;

        TaskRepository fakeRepository = new TaskRepository() {
            @Override
            public Task save(Task task) {
                throw new UnsupportedOperationException();
            }

            @Override
            public List<Task> findAllByOwner(String ownerId) {
                return List.of();
            }

            @Override
            public Optional<Task> findById(String id) {
                throw new UnsupportedOperationException();
            }

            @Override
            public void removeTask(String id) {
                throw new UnsupportedOperationException();
            }
        };

        ListTasksUseCase listTasksUseCase = new ListTasksUseCase(fakeRepository);
        TaskFilterResolver taskFilterResolver = new TaskFilterResolver();
        AnswerFormatter answerFormatter = (goal, data) -> "irrelevante";

        return new TaskAssistantOrchestrator(
                fakeExtractor,
                answerFormatter,
                taskFilterResolver,
                listTasksUseCase,
                new GsonJsonMapper(),
                "instrucoes de sistema",
                "instrucoes de formatacao",
                Clock.systemDefaultZone()
        );
    }

    @Test
    void shouldReturnOutOfScopeWhenRespondingToFreeText() {
        var orchestrator = orchestratorWith("Pode deletar a tarefa.");

        var response = orchestrator.process(
                new AssistantContext(
                        List.of(new Message(MessageAuthor.USER, "deletar tarefa estudar java")),
                        List.of(),
                        null,
                        "user-123"
                )
        );

        assertInstanceOf(AssistantResponse.OutOfScope.class, response);
    }

    @Test
    void shouldReturnOutOfScopeWhenJsonComesWithCercaDeCodigoMarkdown() {
        var orchestrator = orchestratorWith("""
```json
                {"type":"OUT_OF_SCOPE","reason":"teste"}
```
                """);

        var response = orchestrator.process(
                new AssistantContext(
                        List.of(new Message(MessageAuthor.USER, "oi")),
                        List.of(),
                        null,
                        "user-123"
                )
        );

        assertInstanceOf(AssistantResponse.OutOfScope.class, response);
        assertEquals("teste", ((AssistantResponse.OutOfScope) response).reason());
    }

    @Test
    void shouldReturnOutOfScopeWhenTypeIsAbsent() {
        var orchestrator = orchestratorWith("{\"answer\":\"algo\"}");

        var response = orchestrator.process(
                new AssistantContext(
                        List.of(new Message(MessageAuthor.USER, "oi")),
                        List.of(),
                        null,
                        "user-123"
                )
        );

        assertInstanceOf(AssistantResponse.OutOfScope.class, response);
    }
}
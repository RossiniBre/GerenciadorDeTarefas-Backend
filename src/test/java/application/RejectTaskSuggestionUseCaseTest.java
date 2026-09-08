package application.usecases;

import com.taskmanager.application.RejectTaskSuggestionUseCase;
import com.taskmanager.domain.assistant.AssistantSession;
import com.taskmanager.domain.assistant.TaskSuggestion;
import com.taskmanager.domain.repositories.AssistantSessionRepository;
import com.taskmanager.infrastructure.persistence.InMemoryAssistantSessionRepository;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class RejectTaskSuggestionUseCaseTest {

    @Test
    void shouldRemoveSuggestionFromSession() {

        AssistantSessionRepository repository =
                new InMemoryAssistantSessionRepository();

        UUID suggestionId = UUID.randomUUID();

        TaskSuggestion suggestion =
                new TaskSuggestion.Delete(
                        suggestionId,
                        "task-1"
                );

        repository.save(
                "token",
                new AssistantSession(
                        List.of(),
                        List.of(suggestion),
                        null
                )
        );

        RejectTaskSuggestionUseCase useCase =
                new RejectTaskSuggestionUseCase(repository);

        useCase.execute("token", suggestionId);

        AssistantSession session =
                repository.find("token").orElseThrow();

        assertTrue(session.pendingSuggestions().isEmpty());
    }

    @Test
    void shouldThrowWhenSuggestionDoesNotExist() {

        AssistantSessionRepository repository =
                new InMemoryAssistantSessionRepository();

        repository.save(
                "token",
                new AssistantSession(
                        List.of(),
                        List.of(),
                        null
                )
        );

        RejectTaskSuggestionUseCase useCase =
                new RejectTaskSuggestionUseCase(repository);

        assertThrows(
                Exception.class,
                () -> useCase.execute("token", UUID.randomUUID())
        );
    }
}
package infrastructure;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.taskmanager.domain.assistant.AssistantSession;
import com.taskmanager.domain.assistant.Message;
import com.taskmanager.domain.assistant.MessageAuthor;
import com.taskmanager.domain.assistant.TaskSuggestion;
import com.taskmanager.domain.model.TaskCategory;
import com.taskmanager.domain.model.TaskPriority;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class AssistantSessionSerializationTest {

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
    }

    @Test
    void shouldPreserveConcreteTypeAndFieldsWhenRoundTrippingCreateSuggestion() throws Exception {
        UUID suggestionId = UUID.randomUUID();
        TaskSuggestion.Create createSuggestion = new TaskSuggestion.Create(
                suggestionId,
                "Revisar relatório",
                "Revisar o relatório mensal antes do fechamento",
                TaskPriority.HIGH,
                TaskCategory.WORK,
                LocalDateTime.of(2026, 9, 9, 18, 0),
                LocalDateTime.of(2026, 9, 9, 17, 0)
        );

        Message userMessage = new Message(MessageAuthor.USER, "cria uma tarefa pra revisar o relatório amanhã");
        Message assistantMessage = new Message(
                MessageAuthor.ASSISTANT,
                "Criei a sugestão de tarefa. Confirma?",
                List.of(suggestionId)
        );

        AssistantSession originalSession = new AssistantSession(
                List.of(userMessage, assistantMessage),
                List.of(createSuggestion),
                null
        );

        String json = objectMapper.writeValueAsString(originalSession);

        assertTrue(json.contains("\"action\""), "JSON deveria conter o discriminador 'action': " + json);
        assertTrue(json.contains("\"CREATE\""), "JSON deveria conter o valor 'CREATE' do discriminador: " + json);

        AssistantSession deserializedSession = objectMapper.readValue(json, AssistantSession.class);

        assertEquals(1, deserializedSession.pendingSuggestions().size());

        TaskSuggestion deserializedSuggestion = deserializedSession.pendingSuggestions().getFirst();
        assertInstanceOf(TaskSuggestion.Create.class, deserializedSuggestion,
                "Tipo concreto deveria ser preservado como Create após desserialização");

        TaskSuggestion.Create result = (TaskSuggestion.Create) deserializedSuggestion;
        assertEquals(suggestionId, result.id());
        assertEquals("Revisar relatório", result.title());
        assertEquals("Revisar o relatório mensal antes do fechamento", result.description());
        assertEquals(TaskPriority.HIGH, result.priority());
        assertEquals(TaskCategory.WORK, result.category());
        assertEquals(LocalDateTime.of(2026, 9, 9, 18, 0), result.dueDate());
        assertEquals(LocalDateTime.of(2026, 9, 9, 17, 0), result.reminderDate());

        assertEquals(2, deserializedSession.conversationHistory().size());
        assertEquals(MessageAuthor.USER, deserializedSession.conversationHistory().get(0).author());
        assertEquals(MessageAuthor.ASSISTANT, deserializedSession.conversationHistory().get(1).author());
        assertEquals(List.of(suggestionId), deserializedSession.conversationHistory().get(1).suggestionIds());
    }

    @Test
    void shouldPreserveConcreteTypeWhenRoundTrippingDeleteSuggestion() throws Exception {
        UUID suggestionId = UUID.randomUUID();
        TaskSuggestion.Delete deleteSuggestion = new TaskSuggestion.Delete(suggestionId, "task-123");

        AssistantSession originalSession = new AssistantSession(
                List.of(),
                List.of(deleteSuggestion),
                null
        );

        String json = objectMapper.writeValueAsString(originalSession);
        AssistantSession deserializedSession = objectMapper.readValue(json, AssistantSession.class);

        TaskSuggestion deserializedSuggestion = deserializedSession.pendingSuggestions().getFirst();
        assertInstanceOf(TaskSuggestion.Delete.class, deserializedSuggestion);

        TaskSuggestion.Delete result = (TaskSuggestion.Delete) deserializedSuggestion;
        assertEquals(suggestionId, result.id());
        assertEquals("task-123", result.targetTaskId());
    }

    @Test
    void shouldPreserveEmptySessionRoundTrip() throws Exception {
        AssistantSession emptySession = new AssistantSession(List.of(), List.of(), null);

        String json = objectMapper.writeValueAsString(emptySession);
        AssistantSession deserializedSession = objectMapper.readValue(json, AssistantSession.class);

        assertTrue(deserializedSession.conversationHistory().isEmpty());
        assertTrue(deserializedSession.pendingSuggestions().isEmpty());
        assertNull(deserializedSession.activeTask());
    }
}
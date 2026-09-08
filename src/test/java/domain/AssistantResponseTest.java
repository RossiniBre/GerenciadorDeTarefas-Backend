package domain;

import com.taskmanager.domain.assistant.*;
import com.taskmanager.domain.model.TaskCategory;
import com.taskmanager.domain.model.TaskPriority;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class AssistantResponseTest {

    @Test
    void shouldCreateValidSuggestionsWithSuggestionList() {
        TaskSuggestion suggestion = new TaskSuggestion.Create(
                UUID.randomUUID(),
                "Study Java",
                "Review sealed interfaces",
                TaskPriority.MEDIUM,
                TaskCategory.STUDY,
                null,
                null
        );

        AssistantResponse response = new AssistantResponse.ValidSuggestions(
                List.of(suggestion)
        );

        assertTrue(response instanceof AssistantResponse.ValidSuggestions);
    }

    @Test
    void shouldCreateOutOfScopeWithReason() {
        AssistantResponse response = new AssistantResponse.OutOfScope(
                "This is not about creating tasks."
        );

        assertTrue(response instanceof AssistantResponse.OutOfScope);
    }

    @Test
    void shouldCreateMissingInfosWithQuestion() {
        AssistantResponse response = new AssistantResponse.MissingInfos(
                "What is the task title?"
        );

        assertTrue(response instanceof AssistantResponse.MissingInfos);
    }

    @Test
    void shouldHandleAllAssistantResponseVariants() {
        AssistantResponse response = new AssistantResponse.InformationalAnswer(
                "Java uses object-oriented programming."
        );

        String result = switch (response) {
            case AssistantResponse.ValidSuggestions vs ->
                    "suggestions: " + vs.suggestions().size();

            case AssistantResponse.OutOfScope oos ->
                    "rejected: " + oos.reason();

            case AssistantResponse.MissingInfos mi ->
                    "question: " + mi.question();

            case AssistantResponse.InformationalAnswer ia ->
                    "answer: " + ia.answer();
        };

        assertEquals(
                "answer: Java uses object-oriented programming.",
                result
        );
    }

    @Test
    void shouldAllowTaskAssistantStrategyImplementation() {
        TaskAssistant fakeAssistant = context ->
                new AssistantResponse.MissingInfos(
                        "What is the task title?"
                );

        Message userMessage = new Message(
                MessageAuthor.USER,
                "create a task"
        );

        AssistantResponse response = fakeAssistant.process(
                new AssistantContext(
                        List.of(userMessage),
                        List.of(),
                        null,
                        UUID.randomUUID().toString()
                )
        );

        assertInstanceOf(
                AssistantResponse.MissingInfos.class,
                response
        );
    }
}
package application;

import com.taskmanager.application.*;
import com.taskmanager.domain.assistant.AssistantSession;
import com.taskmanager.domain.assistant.TaskSuggestion;
import com.taskmanager.domain.exceptions.TaskSuggestionNotFoundException;
import com.taskmanager.domain.exceptions.UnauthorizedTaskAccessException;
import com.taskmanager.domain.model.Task;
import com.taskmanager.domain.model.TaskCategory;
import com.taskmanager.domain.model.TaskPriority;
import com.taskmanager.domain.model.TaskStatus;
import com.taskmanager.domain.model.User;
import com.taskmanager.infrastructure.persistence.InMemoryAssistantSessionRepository;
import com.taskmanager.infrastructure.persistence.InMemoryTaskRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import com.taskmanager.domain.notification.NotificationScheduleCalculator;
import com.taskmanager.infrastructure.persistence.InMemoryNotificationRepository;

import java.time.Clock;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ConfirmTaskSuggestionUseCaseTest {

    private static final String TOKEN = "fake-token-123";

    private InMemoryTaskRepository taskRepository;
    private InMemoryAssistantSessionRepository sessionRepository;
    private ConfirmTaskSuggestionUseCase useCase;
    private User owner;

    @BeforeEach
    void setUp() {
        taskRepository = new InMemoryTaskRepository();
        sessionRepository = new InMemoryAssistantSessionRepository();

        Clock clock = Clock.systemDefaultZone();

        InMemoryNotificationRepository notificationRepository = new InMemoryNotificationRepository();
        NotificationScheduleCalculator scheduleCalculator = new NotificationScheduleCalculator(clock);
        CreateNotificationUseCase createNotificationUseCase =
                new CreateNotificationUseCase(notificationRepository, scheduleCalculator);
        RescheduleNotificationsUseCase rescheduleNotificationsUseCase =
                new RescheduleNotificationsUseCase(notificationRepository, createNotificationUseCase);
        CancelNotificationsUseCase cancelNotificationsUseCase =
                new CancelNotificationsUseCase(notificationRepository);

        CreateTaskUseCase createTaskUseCase = new CreateTaskUseCase(taskRepository, clock, createNotificationUseCase);
        UpdateTaskDetailsUseCase updateTaskDetailsUseCase = new UpdateTaskDetailsUseCase(taskRepository, clock, rescheduleNotificationsUseCase);
        DeleteTaskUseCase deleteTaskUseCase = new DeleteTaskUseCase(taskRepository, cancelNotificationsUseCase);
        StartTaskUseCase startTaskUseCase = new StartTaskUseCase(taskRepository);
        CompleteTaskUseCase completeTaskUseCase = new CompleteTaskUseCase(taskRepository, cancelNotificationsUseCase);

        useCase = new ConfirmTaskSuggestionUseCase(
                sessionRepository,
                createTaskUseCase,
                updateTaskDetailsUseCase,
                deleteTaskUseCase,
                startTaskUseCase,
                completeTaskUseCase
        );

        owner = User.newUser("owner@example.com", "ownerUser", "hash-fake-123");
    }

    @Test
    void shouldCreateTaskWhenConfirmingCreateSuggestion() {
        UUID suggestionId = UUID.randomUUID();
        TaskSuggestion.Create suggestion = new TaskSuggestion.Create(
                suggestionId, "Titulo sugerido", "Descricao sugerida",
                TaskPriority.HIGH, TaskCategory.WORK, null, null
        );
        sessionRepository.save(TOKEN, new AssistantSession(List.of(), List.of(suggestion)));

        useCase.execute(TOKEN, owner, suggestionId);

        List<Task> tasks = taskRepository.findAllByOwner(owner.getId());
        assertEquals(1, tasks.size());
        assertEquals("Titulo sugerido", tasks.get(0).getTitle());
        assertEquals(TaskPriority.HIGH, tasks.get(0).getPriority());
        assertEquals(TaskCategory.WORK, tasks.get(0).getCategory());
    }

    @Test
    void shouldUpdateTaskWhenConfirmingUpdateSuggestion() {
        Task existing = Task.newTask("Titulo antigo", "Descricao antiga", owner.getId(), null, null);
        taskRepository.save(existing);

        UUID suggestionId = UUID.randomUUID();
        TaskSuggestion.Update suggestion = new TaskSuggestion.Update(
                suggestionId, existing.getId(), "Titulo novo", "Descricao nova",
                TaskPriority.MEDIUM, TaskCategory.STUDY, null, null
        );
        sessionRepository.save(TOKEN, new AssistantSession(List.of(), List.of(suggestion)));

        useCase.execute(TOKEN, owner, suggestionId);

        Task updated = taskRepository.findById(existing.getId()).orElseThrow();
        assertEquals("Titulo novo", updated.getTitle());
        assertEquals(TaskPriority.MEDIUM, updated.getPriority());
    }

    @Test
    void shouldDeleteTaskWhenConfirmingDeleteSuggestion() {
        Task existing = Task.newTask("Titulo", "Descricao", owner.getId(), null, null);
        taskRepository.save(existing);

        UUID suggestionId = UUID.randomUUID();
        TaskSuggestion.Delete suggestion = new TaskSuggestion.Delete(suggestionId, existing.getId());
        sessionRepository.save(TOKEN, new AssistantSession(List.of(), List.of(suggestion)));

        useCase.execute(TOKEN, owner, suggestionId);

        assertTrue(taskRepository.findById(existing.getId()).isEmpty());
    }

    @Test
    void shouldStartTaskWhenConfirmingStartSuggestion() {
        Task existing = Task.newTask("Titulo", "Descricao", owner.getId(), null, null);
        taskRepository.save(existing);

        UUID suggestionId = UUID.randomUUID();
        TaskSuggestion.Start suggestion = new TaskSuggestion.Start(suggestionId, existing.getId());
        sessionRepository.save(TOKEN, new AssistantSession(List.of(), List.of(suggestion)));

        useCase.execute(TOKEN, owner, suggestionId);

        Task updated = taskRepository.findById(existing.getId()).orElseThrow();
        assertEquals(TaskStatus.IN_PROGRESS, updated.getStatus());
    }

    @Test
    void shouldCompleteTaskWhenConfirmingCompleteSuggestion() {
        Task existing = Task.newTask("Titulo", "Descricao", owner.getId(), null, null);
        existing.startTask();
        taskRepository.save(existing);

        UUID suggestionId = UUID.randomUUID();
        TaskSuggestion.Complete suggestion = new TaskSuggestion.Complete(suggestionId, existing.getId());
        sessionRepository.save(TOKEN, new AssistantSession(List.of(), List.of(suggestion)));

        useCase.execute(TOKEN, owner, suggestionId);

        Task updated = taskRepository.findById(existing.getId()).orElseThrow();
        assertEquals(TaskStatus.COMPLETED, updated.getStatus());
    }

    @Test
    void shouldRemoveConfirmedSuggestionFromSessionButKeepOthersAndHistory() {
        Task existing = Task.newTask("Titulo", "Descricao", owner.getId(), null, null);
        taskRepository.save(existing);

        UUID confirmedId = UUID.randomUUID();
        UUID otherId = UUID.randomUUID();
        TaskSuggestion.Start confirmed = new TaskSuggestion.Start(confirmedId, existing.getId());
        TaskSuggestion.Delete other = new TaskSuggestion.Delete(otherId, existing.getId());

        sessionRepository.save(TOKEN, new AssistantSession(List.of(), List.of(confirmed, other)));

        useCase.execute(TOKEN, owner, confirmedId);

        AssistantSession updatedSession = sessionRepository.find(TOKEN).orElseThrow();
        assertEquals(1, updatedSession.pendingSuggestions().size());
        assertEquals(otherId, updatedSession.pendingSuggestions().get(0).id());
    }

    @Test
    void shouldThrowWhenTokenHasNoSession() {
        UUID suggestionId = UUID.randomUUID();

        assertThrows(TaskSuggestionNotFoundException.class,
                () -> useCase.execute(TOKEN, owner, suggestionId));
    }

    @Test
    void shouldThrowWhenSuggestionIdDoesNotMatchAnyPending() {
        TaskSuggestion.Start suggestion = new TaskSuggestion.Start(UUID.randomUUID(), "some-task-id");
        sessionRepository.save(TOKEN, new AssistantSession(List.of(), List.of(suggestion)));

        UUID unknownId = UUID.randomUUID();

        assertThrows(TaskSuggestionNotFoundException.class,
                () -> useCase.execute(TOKEN, owner, unknownId));
    }

    @Test
    void shouldThrowWhenConfirmingSuggestionForTaskOwnedByAnotherUser() {
        User anotherUser = User.newUser("owner@example.com", "ownerUser", "hash-fake-123");
        Task existing = Task.newTask("Titulo", "Descricao", anotherUser.getId(), null, null);
        taskRepository.save(existing);

        UUID suggestionId = UUID.randomUUID();
        TaskSuggestion.Start suggestion = new TaskSuggestion.Start(suggestionId, existing.getId());
        sessionRepository.save(TOKEN, new AssistantSession(List.of(), List.of(suggestion)));

        assertThrows(UnauthorizedTaskAccessException.class,
                () -> useCase.execute(TOKEN, owner, suggestionId));
    }
}
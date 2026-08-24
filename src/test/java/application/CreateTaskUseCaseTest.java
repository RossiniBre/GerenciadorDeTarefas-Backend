package application;

import com.taskmanager.application.CreateNotificationUseCase;
import com.taskmanager.application.CreateTaskUseCase;
import com.taskmanager.domain.model.Task;
import com.taskmanager.domain.model.TaskPriority;
import com.taskmanager.domain.model.TaskCategory;
import com.taskmanager.domain.model.TaskStatus;
import com.taskmanager.domain.model.User;
import com.taskmanager.domain.notification.NotificationScheduleCalculator;
import com.taskmanager.infrastructure.persistence.InMemoryNotificationRepository;
import com.taskmanager.infrastructure.persistence.InMemoryTaskRepository;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.*;

class CreateTaskUseCaseTest {

    @Test
    void shouldCreateTaskWithPendingStatus() {
        // Arrange
        InMemoryTaskRepository repo = new InMemoryTaskRepository();
        Clock fixedClock = Clock.fixed(Instant.parse("2026-08-04T10:00:00Z"), ZoneOffset.UTC);

        InMemoryNotificationRepository notificationRepository = new InMemoryNotificationRepository();
        NotificationScheduleCalculator scheduleCalculator = new NotificationScheduleCalculator(fixedClock);
        CreateNotificationUseCase createNotificationUseCase =
                new CreateNotificationUseCase(notificationRepository, scheduleCalculator);

        CreateTaskUseCase useCase = new CreateTaskUseCase(repo, fixedClock, createNotificationUseCase);
        User owner = User.newUser("owner@example.com", "ownerUser", "hash-fake-123", "Owner");

        LocalDateTime dueDate = LocalDateTime.of(2026, 8, 10, 18, 0);
        LocalDateTime reminderDate = LocalDateTime.of(2026, 8, 9, 9, 0);

        // Act
        Task task = useCase.execute(
                "Titulo original", "Descricao original",
                owner,
                TaskPriority.MEDIUM, TaskCategory.WORK,
                dueDate, reminderDate
        );

        // Assert
        assertEquals("Titulo original", task.getTitle());
        assertEquals("Descricao original", task.getDescription());
        assertEquals(TaskStatus.PENDING, task.getStatus());
        assertEquals(dueDate, task.getDueDate());
        assertEquals(reminderDate, task.getReminderDate());
    }
}
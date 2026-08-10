package application;

import com.taskmanager.application.CancelNotificationsUseCase;
import com.taskmanager.application.CompleteTaskUseCase;
import com.taskmanager.domain.model.Task;
import com.taskmanager.domain.model.TaskStatus;
import com.taskmanager.domain.model.User;
import com.taskmanager.infrastructure.persistence.InMemoryNotificationRepository;
import com.taskmanager.infrastructure.persistence.InMemoryTaskRepository;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class CompleteTaskUseCaseTest {

    @Test
    void shouldCompleteInProgressTask(){
        // Arrange
        InMemoryNotificationRepository notificationRepository = new InMemoryNotificationRepository();
        CancelNotificationsUseCase cancelNotificationsUseCase = new CancelNotificationsUseCase(notificationRepository);
        InMemoryTaskRepository repo = new InMemoryTaskRepository();

        User user = User.newUser("owner@example.com", "owner123", "senhaHash");

        Task existingTask = Task.newTask(
                "Titulo original",
                "Descricao Original",
                user.getId(),
                null,
                null
        );

        existingTask.startTask();

        repo.save(existingTask);

        String existingId = existingTask.getId();

        // Act
        CompleteTaskUseCase useCase = new CompleteTaskUseCase(repo, cancelNotificationsUseCase);
        useCase.execute(existingId, user);

        // Assert
        TaskStatus status = repo.findById(existingId)
                .orElseThrow()
                .getStatus();

        assertEquals(TaskStatus.COMPLETED, status);
    }
}
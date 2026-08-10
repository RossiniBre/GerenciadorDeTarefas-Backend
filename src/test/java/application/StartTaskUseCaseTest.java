package application;

import com.taskmanager.application.StartTaskUseCase;
import com.taskmanager.domain.model.Task;
import com.taskmanager.domain.model.TaskStatus;
import com.taskmanager.domain.model.User;
import com.taskmanager.infrastructure.persistence.InMemoryTaskRepository;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class StartTaskUseCaseTest {

    @Test
    void shouldStartPendingTask(){
        // Arrange
        InMemoryTaskRepository repo = new InMemoryTaskRepository();

        User user = User.newUser("owner@example.com", "ownerUser", "senhaHash");

        Task existingTask = Task.newTask(
                "Titulo original",
                "Descricao Original",
                user.getId(),
                null,
                null
        );

        repo.save(existingTask);

        String existingId = existingTask.getId();

        // Act
        StartTaskUseCase useCase = new StartTaskUseCase(repo);
        useCase.execute(existingId, user);

        // Assert
        TaskStatus status = repo.findById(existingId)
                .orElseThrow()
                .getStatus();

        assertEquals(TaskStatus.IN_PROGRESS, status);
    }
}
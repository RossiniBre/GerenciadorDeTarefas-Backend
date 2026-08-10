package application;

import com.taskmanager.application.DeleteAccountUseCase;
import com.taskmanager.application.DeleteTaskUseCase;
import com.taskmanager.domain.model.Task;
import com.taskmanager.domain.repositories.TaskRepository;
import com.taskmanager.domain.repositories.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import java.util.List;

import static org.mockito.Mockito.*;

class DeleteAccountUseCaseTest {

    private UserRepository userRepository;
    private TaskRepository taskRepository;
    private DeleteTaskUseCase deleteTaskUseCase;
    private DeleteAccountUseCase deleteAccountUseCase;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        taskRepository = mock(TaskRepository.class);
        deleteTaskUseCase = mock(DeleteTaskUseCase.class);
        deleteAccountUseCase = new DeleteAccountUseCase(userRepository, taskRepository, deleteTaskUseCase);
    }

    @Test
    void deletesEachTaskBeforeDeletingTheAccount() {
        String userId = "user-1";
        Task task1 = mock(Task.class);
        Task task2 = mock(Task.class);
        when(task1.getId()).thenReturn("task-1");
        when(task2.getId()).thenReturn("task-2");
        when(taskRepository.findAllByOwner(userId)).thenReturn(List.of(task1, task2));

        deleteAccountUseCase.execute(userId);

        InOrder inOrder = inOrder(deleteTaskUseCase, userRepository);
        inOrder.verify(deleteTaskUseCase).execute("task-1", userId);
        inOrder.verify(deleteTaskUseCase).execute("task-2", userId);
        inOrder.verify(userRepository).deleteAccount(userId);
    }

    @Test
    void deletesAccountEvenWhenUserHasNoTasks() {
        String userId = "user-1";
        when(taskRepository.findAllByOwner(userId)).thenReturn(List.of());

        deleteAccountUseCase.execute(userId);

        verify(deleteTaskUseCase, never()).execute(any(), any());
        verify(userRepository).deleteAccount(userId);
    }

    @Test
    void doesNotDeleteAccountIfDeletingATaskFails() {
        String userId = "user-1";
        Task task1 = mock(Task.class);
        when(task1.getId()).thenReturn("task-1");
        when(taskRepository.findAllByOwner(userId)).thenReturn(List.of(task1));
        doThrow(new RuntimeException("boom")).when(deleteTaskUseCase).execute("task-1", userId);

        try {
            deleteAccountUseCase.execute(userId);
        } catch (RuntimeException ignored) {
        }

        verify(userRepository, never()).deleteAccount(userId);
    }
}
package infrastructure;

import com.taskmanager.domain.model.Task;
import com.taskmanager.domain.model.TaskCategory;
import com.taskmanager.domain.model.TaskPriority;
import com.taskmanager.domain.model.TaskStatus;
import com.taskmanager.infrastructure.config.RepositoryException;
import com.taskmanager.infrastructure.persistence.mysql.MySqlTaskRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class MySqlTaskRepositoryTest {

    private Connection connection;
    private PreparedStatement preparedStatement;
    private ResultSet resultSet;
    private MySqlTaskRepository repository;

    @BeforeEach
    void setUp() throws SQLException {
        connection = mock(Connection.class);
        preparedStatement = mock(PreparedStatement.class);
        resultSet = mock(ResultSet.class);
        repository = new MySqlTaskRepository(connection);

        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
    }

    @Test
    void savesTaskWithAllFieldsBound() throws SQLException {
        Task task = Task.rebuiltTask(
                "Estudar Spring",
                "Revisar beans",
                TaskStatus.PENDING,
                "task-1",
                TaskPriority.HIGH,
                TaskCategory.WORK,
                "owner-1",
                LocalDateTime.of(2026, 8, 20, 14, 0),
                LocalDateTime.of(2026, 8, 20, 12, 0)
        );

        Task result = repository.save(task);

        verify(preparedStatement).setString(1, "task-1");
        verify(preparedStatement).setString(2, "Estudar Spring");
        verify(preparedStatement).setString(3, "Revisar beans");
        verify(preparedStatement).setString(4, "PENDING");
        verify(preparedStatement).setString(5, "HIGH");
        verify(preparedStatement).setString(6, "WORK");
        verify(preparedStatement).setString(7, "owner-1");
        verify(preparedStatement).setTimestamp(eq(8), any(Timestamp.class));
        verify(preparedStatement).setTimestamp(eq(9), any(Timestamp.class));
        verify(preparedStatement).executeUpdate();
        assertSame(task, result);
    }

    @Test
    void savesTaskWithNullDueDateAndReminderDateAsSqlNull() throws SQLException {
        Task task = Task.rebuiltTask(
                "Sem datas",
                "desc",
                TaskStatus.PENDING,
                "task-2",
                TaskPriority.LOW,
                TaskCategory.PERSONAL,
                "owner-1",
                null,
                null
        );

        repository.save(task);

        verify(preparedStatement).setNull(eq(8), anyInt());
        verify(preparedStatement).setNull(eq(9), anyInt());
    }

    @Test
    void saveWrapsSqlExceptionInRepositoryException() throws SQLException {
        Task task = Task.rebuiltTask(
                "X", "Y", TaskStatus.PENDING, "task-3", TaskPriority.LOW,
                TaskCategory.WORK, "owner-1", null, null
        );
        when(preparedStatement.executeUpdate()).thenThrow(new SQLException("boom"));

        RepositoryException ex = assertThrows(RepositoryException.class, () -> repository.save(task));
        assertEquals("Erro ao inserir valores.", ex.getMessage());
    }

    @Test
    void findAllByOwnerReturnsMappedTasks() throws SQLException {
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true, true, false);
        when(resultSet.getString("id")).thenReturn("task-1", "task-2");
        when(resultSet.getString("title")).thenReturn("Tarefa 1", "Tarefa 2");
        when(resultSet.getString("description")).thenReturn("desc1", "desc2");
        when(resultSet.getString("owner_id")).thenReturn("owner-1", "owner-1");
        when(resultSet.getString("status")).thenReturn("PENDING", "PENDING");
        when(resultSet.getString("priority")).thenReturn("HIGH", "LOW");
        when(resultSet.getString("category")).thenReturn("WORK", "PERSONAL");
        when(resultSet.getTimestamp("due_date")).thenReturn(null);
        when(resultSet.getTimestamp("reminder_date")).thenReturn(null);

        List<Task> tasks = repository.findAllByOwner("owner-1");

        assertEquals(2, tasks.size());
        assertEquals("task-1", tasks.get(0).getId());
        assertEquals("task-2", tasks.get(1).getId());
        verify(preparedStatement).setString(1, "owner-1");
    }

    @Test
    void findAllByOwnerReturnsEmptyListWhenNoRows() throws SQLException {
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(false);

        List<Task> tasks = repository.findAllByOwner("owner-1");

        assertTrue(tasks.isEmpty());
    }

    @Test
    void findAllByOwnerWrapsSqlExceptionInRepositoryException() throws SQLException {
        when(preparedStatement.executeQuery()).thenThrow(new SQLException("boom"));

        assertThrows(RepositoryException.class, () -> repository.findAllByOwner("owner-1"));
    }

    @Test
    void findByIdReturnsTaskWhenPresent() throws SQLException {
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true);
        when(resultSet.getString("id")).thenReturn("task-1");
        when(resultSet.getString("title")).thenReturn("Tarefa 1");
        when(resultSet.getString("description")).thenReturn("desc1");
        when(resultSet.getString("owner_id")).thenReturn("owner-1");
        when(resultSet.getString("status")).thenReturn("PENDING");
        when(resultSet.getString("priority")).thenReturn("HIGH");
        when(resultSet.getString("category")).thenReturn("WORK");
        when(resultSet.getTimestamp("due_date")).thenReturn(Timestamp.valueOf(LocalDateTime.of(2026, 8, 20, 14, 0)));
        when(resultSet.getTimestamp("reminder_date")).thenReturn(null);

        Optional<Task> result = repository.findById("task-1");

        assertTrue(result.isPresent());
        assertEquals("task-1", result.get().getId());
        assertNotNull(result.get().getDueDate());
        assertNull(result.get().getReminderDate());
    }

    @Test
    void findByIdReturnsEmptyWhenNotFound() throws SQLException {
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(false);

        Optional<Task> result = repository.findById("does-not-exist");

        assertTrue(result.isEmpty());
    }

    @Test
    void findByIdWrapsSqlExceptionInRepositoryException() throws SQLException {
        when(preparedStatement.executeQuery()).thenThrow(new SQLException("boom"));

        assertThrows(RepositoryException.class, () -> repository.findById("task-1"));
    }

    @Test
    void removeTaskExecutesDeleteWithGivenId() throws SQLException {
        repository.removeTask("task-1");

        verify(preparedStatement).setString(1, "task-1");
        verify(preparedStatement).executeUpdate();
    }

    @Test
    void removeTaskWrapsSqlExceptionInRepositoryException() throws SQLException {
        when(preparedStatement.executeUpdate()).thenThrow(new SQLException("boom"));

        RepositoryException ex = assertThrows(RepositoryException.class, () -> repository.removeTask("task-1"));
        assertEquals("Erro ao remover tarefa", ex.getMessage());
    }
}
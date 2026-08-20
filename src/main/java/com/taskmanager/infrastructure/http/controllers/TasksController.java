package com.taskmanager.infrastructure.http.controllers;

import com.taskmanager.application.*;
import com.taskmanager.domain.exceptions.TaskNotFoundException;
import com.taskmanager.domain.model.*;
import com.taskmanager.domain.notification.Notification;
import com.taskmanager.domain.repositories.TaskRepository;
import com.taskmanager.infrastructure.http.AuthenticatedUser;
import com.taskmanager.infrastructure.http.dto.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/tasks")
public class TasksController {

    private final CreateTaskUseCase createTaskUseCase;
    private final ListTasksUseCase listTasksUseCase;
    private final UpdateTaskDetailsUseCase updateTaskUseCase;
    private final DeleteTaskUseCase deleteTaskUseCase;
    private final ListNotificationsUseCase listNotificationsUseCase;
    private final TaskRepository taskRepository;
    private final StartTaskUseCase startTaskUseCase;
    private final CompleteTaskUseCase completeTaskUseCase;

    public TasksController(CreateTaskUseCase createTaskUseCase,
                           ListTasksUseCase listTasksUseCase,
                           UpdateTaskDetailsUseCase updateTaskUseCase,
                           DeleteTaskUseCase deleteTaskUseCase,
                           ListNotificationsUseCase listNotificationsUseCase,
                           TaskRepository taskRepository,
                           StartTaskUseCase startTaskUseCase,
                           CompleteTaskUseCase completeTaskUseCase) {
        this.createTaskUseCase = createTaskUseCase;
        this.listTasksUseCase = listTasksUseCase;
        this.updateTaskUseCase = updateTaskUseCase;
        this.deleteTaskUseCase = deleteTaskUseCase;
        this.listNotificationsUseCase = listNotificationsUseCase;
        this.taskRepository = taskRepository;
        this.startTaskUseCase = startTaskUseCase;
        this.completeTaskUseCase = completeTaskUseCase;
    }

    @PostMapping
    public ResponseEntity<CreateTaskResponse> create(
            @AuthenticatedUser User user,
            @RequestBody CreateTaskRequest request) {

        TaskPriority priority = request.priority != null ? TaskPriority.valueOf(request.priority) : null;
        TaskCategory category = request.category != null ? TaskCategory.valueOf(request.category) : null;
        LocalDateTime dueDate = request.dueDate != null ? LocalDateTime.parse(request.dueDate) : null;
        LocalDateTime reminderDate = request.reminderDate != null ? LocalDateTime.parse(request.reminderDate) : null;

        Task task = createTaskUseCase.execute(
                request.title, request.description, user, priority, category, dueDate, reminderDate);

        var response = new CreateTaskResponse(
                task.getId(), task.getTitle(), task.getStatus().name(),
                task.getPriority().name(), task.getCategory().name());

        return ResponseEntity.status(201).body(response);
    }

    @GetMapping
    public ResponseEntity<List<TaskItemDto>> list(
            @AuthenticatedUser User user,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String priority,
            @RequestParam(required = false) String category) {

        TaskStatus taskStatus = status != null ? TaskStatus.valueOf(status) : null;
        TaskPriority taskPriority = priority != null ? TaskPriority.valueOf(priority) : null;
        TaskCategory taskCategory = category != null ? TaskCategory.valueOf(category) : null;

        ListTasksUseCase.TaskFilter filter =
                (taskStatus == null && taskPriority == null && taskCategory == null)
                        ? null
                        : new ListTasksUseCase.TaskFilter(taskStatus, taskPriority, taskCategory, Set.of(), null, null);

        List<Task> tasks = listTasksUseCase.execute(user.getId(), filter);

        List<TaskItemDto> items = tasks.stream()
                .map(t -> new TaskItemDto(
                        t.getId(), t.getTitle(), t.getDescription(), t.getStatus().name(), t.getPriority().name(), t.getCategory().name(), t.getDueDate()))
                .toList();

        return ResponseEntity.ok(items);
    }

    @PatchMapping("/{id}")
    public ResponseEntity<UpdateTaskResponse> update(
            @AuthenticatedUser User user,
            @PathVariable String id,
            @RequestBody UpdateTaskRequest request) {

        TaskPriority priority = request.priority != null ? TaskPriority.valueOf(request.priority) : null;
        TaskCategory category = request.category != null ? TaskCategory.valueOf(request.category) : null;
        LocalDateTime dueDate = request.dueDate != null ? LocalDateTime.parse(request.dueDate) : null;
        LocalDateTime reminderDate = request.reminderDate != null ? LocalDateTime.parse(request.reminderDate) : null;

        Task task = updateTaskUseCase.execute(
                request.title, request.description, priority, category, dueDate, reminderDate, id, user.getId());

        var response = new UpdateTaskResponse(
                task.getId(), task.getTitle(), task.getStatus().name(),
                task.getPriority().name(), task.getCategory().name());

        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @AuthenticatedUser User user,
            @PathVariable String id) {

        deleteTaskUseCase.execute(id, user.getId());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/notifications")
    public ResponseEntity<ListNotificationsResponse> listNotifications(
            @AuthenticatedUser User user,
            @PathVariable String id) {

        Task task = taskRepository.findById(id).orElseThrow(() -> new TaskNotFoundException(id));
        task.verifyOwnership(user.getId());

        List<Notification> notifications = listNotificationsUseCase.execute(id);

        List<ListNotificationsResponse.NotificationItem> items = notifications.stream()
                .map(ListNotificationsResponse.NotificationItem::from)
                .toList();

        return ResponseEntity.ok(new ListNotificationsResponse(items));
    }

    @PostMapping("/{id}/start")
    public ResponseEntity<TaskItemDto> start(@AuthenticatedUser User user, @PathVariable String id) {
        Task task = startTaskUseCase.execute(id, user);
        var response = new TaskItemDto(
                task.getId(), task.getTitle(), task.getDescription(), task.getStatus().name(),
                task.getPriority().name(), task.getCategory().name(), task.getDueDate());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/complete")
    public ResponseEntity<TaskItemDto> complete(@AuthenticatedUser User user, @PathVariable String id) {
        Task task = completeTaskUseCase.execute(id, user);
        var response = new TaskItemDto(
                task.getId(), task.getTitle(), task.getDescription(), task.getStatus().name(),
                task.getPriority().name(), task.getCategory().name(), task.getDueDate());
        return ResponseEntity.ok(response);
    }
}
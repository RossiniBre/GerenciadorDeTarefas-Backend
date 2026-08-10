package com.taskmanager.infrastructure.config;

import com.taskmanager.application.CreateTaskUseCase;
import com.taskmanager.application.CreateNotificationUseCase;
import com.taskmanager.application.ListTasksUseCase;
import com.taskmanager.application.UpdateTaskDetailsUseCase;
import com.taskmanager.application.DeleteTaskUseCase;
import com.taskmanager.application.DeleteAccountUseCase;
import com.taskmanager.application.RescheduleNotificationsUseCase;
import com.taskmanager.application.CancelNotificationsUseCase;
import com.taskmanager.domain.repositories.TaskRepository;
import com.taskmanager.domain.repositories.UserRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class TaskUseCaseConfig {

    @Bean
    public CreateTaskUseCase createTaskUseCase(TaskRepository taskRepository, Clock clock, CreateNotificationUseCase createNotificationUseCase) {
        return new CreateTaskUseCase(taskRepository, clock, createNotificationUseCase);
    }

    @Bean
    public ListTasksUseCase listTasksUseCase(TaskRepository taskRepository) {
        return new ListTasksUseCase(taskRepository);
    }

    @Bean
    public UpdateTaskDetailsUseCase updateTaskDetailsUseCase(TaskRepository taskRepository, Clock clock, RescheduleNotificationsUseCase rescheduleNotificationsUseCase) {
        return new UpdateTaskDetailsUseCase(taskRepository, clock, rescheduleNotificationsUseCase);
    }

    @Bean
    public DeleteTaskUseCase deleteTaskUseCase(TaskRepository taskRepository, CancelNotificationsUseCase cancelNotificationsUseCase) {
        return new DeleteTaskUseCase(taskRepository, cancelNotificationsUseCase);
    }

    @Bean
    public DeleteAccountUseCase deleteAccountUseCase(UserRepository userRepository, TaskRepository taskRepository, DeleteTaskUseCase deleteTaskUseCase) {
        return new DeleteAccountUseCase(userRepository, taskRepository, deleteTaskUseCase);
    }
}
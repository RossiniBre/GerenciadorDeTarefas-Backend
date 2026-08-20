package com.taskmanager.infrastructure.config;

import com.taskmanager.application.*;
import com.taskmanager.domain.notification.*;
import com.taskmanager.domain.repositories.NotificationRepository;
import com.taskmanager.domain.repositories.TaskRepository;
import com.taskmanager.infrastructure.persistence.InMemoryNotificationRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.ZoneId;

@Configuration
public class NotificationConfig {

    @Bean
    public Clock clock() {
        return Clock.system(ZoneId.of("America/Sao_Paulo"));
    }

    @Bean
    public NotificationRepository notificationRepository() {
        return new InMemoryNotificationRepository();
    }

    @Bean
    public NotificationScheduleCalculator notificationScheduleCalculator(Clock clock) {
        return new NotificationScheduleCalculator(clock);
    }

    @Bean
    public NotificationSender notificationSender() {
        return new ConsoleNotificationSender();
    }

    @Bean
    public CreateNotificationUseCase createNotificationUseCase(NotificationRepository notificationRepository, NotificationScheduleCalculator scheduleCalculator) {
        return new CreateNotificationUseCase(notificationRepository, scheduleCalculator);
    }

    @Bean
    public RescheduleNotificationsUseCase rescheduleNotificationsUseCase(NotificationRepository notificationRepository, CreateNotificationUseCase createNotificationUseCase) {
        return new RescheduleNotificationsUseCase(notificationRepository, createNotificationUseCase);
    }

    @Bean
    public CancelNotificationsUseCase cancelNotificationsUseCase(NotificationRepository notificationRepository) {
        return new CancelNotificationsUseCase(notificationRepository);
    }

    @Bean
    public ListNotificationsUseCase listNotificationsUseCase(NotificationRepository notificationRepository) {
        return new ListNotificationsUseCase(notificationRepository);
    }

    @Bean
    public SendNotificationUseCase sendNotificationUseCase(NotificationRepository notificationRepository, TaskRepository taskRepository, NotificationSender notificationSender) {
        return new SendNotificationUseCase(notificationRepository, taskRepository, notificationSender);
    }

    @Bean(initMethod = "start", destroyMethod = "stop")
    public NotificationScheduler notificationScheduler(NotificationRepository notificationRepository, SendNotificationUseCase sendNotificationUseCase, Clock clock) {
        return new NotificationScheduler(notificationRepository, sendNotificationUseCase, clock);
    }
}
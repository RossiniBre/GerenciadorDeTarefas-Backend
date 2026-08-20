package com.taskmanager.infrastructure.http.dto;

import java.time.LocalDateTime;

public class TaskItemDto {
    public String id;
    public String title;
    public String description;
    public String status;
    public String priority;
    public String category;
    public LocalDateTime dueDate;

    public TaskItemDto(String id, String title, String description, String status, String priority, String category, LocalDateTime dueDate) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.status = status;
        this.priority = priority;
        this.category = category;
        this.dueDate = dueDate;
    }
}
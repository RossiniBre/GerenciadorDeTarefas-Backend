package com.taskmanager.domain.assistant;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.taskmanager.domain.model.TaskPriority;
import com.taskmanager.domain.model.TaskCategory;

import java.time.LocalDateTime;
import java.util.UUID;

@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "action"
)
@JsonSubTypes({
        @JsonSubTypes.Type(value = TaskSuggestion.Create.class, name = "CREATE"),
        @JsonSubTypes.Type(value = TaskSuggestion.Update.class, name = "UPDATE"),
        @JsonSubTypes.Type(value = TaskSuggestion.Delete.class, name = "DELETE"),
        @JsonSubTypes.Type(value = TaskSuggestion.Start.class, name = "START"),
        @JsonSubTypes.Type(value = TaskSuggestion.Complete.class, name = "COMPLETE")
})

public sealed interface TaskSuggestion permits
        TaskSuggestion.Create,
        TaskSuggestion.Update,
        TaskSuggestion.Delete,
        TaskSuggestion.Start,
        TaskSuggestion.Complete {

    UUID id();

    record Create(
            UUID id,
            String title,
            String description,
            TaskPriority priority,
            TaskCategory category,
            LocalDateTime dueDate,
            LocalDateTime reminderDate
    ) implements TaskSuggestion {}

    record Update(
            UUID id,
            String targetTaskId,
            String title,
            String description,
            TaskPriority priority,
            TaskCategory category,
            LocalDateTime dueDate,
            LocalDateTime reminderDate
    ) implements TaskSuggestion {}

    record Delete(
            UUID id,
            String targetTaskId
    ) implements TaskSuggestion {}

    record Start(
            UUID id,
            String targetTaskId
    ) implements TaskSuggestion {}

    record Complete(
            UUID id,
            String targetTaskId
    ) implements TaskSuggestion {}
}
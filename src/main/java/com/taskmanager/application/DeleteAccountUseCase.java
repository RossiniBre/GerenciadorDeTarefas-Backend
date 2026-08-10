package com.taskmanager.application;

import com.taskmanager.domain.model.Task;
import com.taskmanager.domain.repositories.TaskRepository;
import com.taskmanager.domain.repositories.UserRepository;

import java.util.List;

public class DeleteAccountUseCase {
    private final UserRepository userRepository;
    private final TaskRepository taskRepository;
    private final DeleteTaskUseCase deleteTaskUseCase;

    public DeleteAccountUseCase(UserRepository userRepository,
                                TaskRepository taskRepository,
                                DeleteTaskUseCase deleteTaskUseCase) {
        this.userRepository = userRepository;
        this.taskRepository = taskRepository;
        this.deleteTaskUseCase = deleteTaskUseCase;
    }

    public void execute(String userId) {
        List<Task> tasks = taskRepository.findAllByOwner(userId);
        for (Task task : tasks) {
            deleteTaskUseCase.execute(task.getId(), userId);
        }
        userRepository.deleteAccount(userId);
    }
}
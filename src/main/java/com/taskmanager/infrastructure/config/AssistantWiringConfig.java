package com.taskmanager.infrastructure.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.taskmanager.application.*;
import com.taskmanager.domain.assistant.*;
import com.taskmanager.domain.repositories.AssistantSessionRepository;
import com.taskmanager.domain.repositories.TaskRepository;
import com.taskmanager.infrastructure.assistant.*;
import com.taskmanager.infrastructure.http.json.GsonJsonMapper;
import com.taskmanager.infrastructure.persistence.LocalDateTimeTypeAdapter;
import com.taskmanager.infrastructure.persistence.RedisAssistantSessionRepository;
import com.taskmanager.infrastructure.http.json.JsonMapper;
import com.taskmanager.infrastructure.persistence.TaskSuggestionAdapter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import redis.clients.jedis.JedisPool;

import java.io.IOException;
import java.net.http.HttpClient;
import java.time.Clock;
import java.time.LocalDateTime;

@Configuration
public class AssistantWiringConfig {

    private static String loadInstructions(String resourcePath) {
        try (var input = AssistantWiringConfig.class.getClassLoader().getResourceAsStream(resourcePath)) {
            if (input == null) {
                throw new IllegalStateException("Arquivo não encontrado: " + resourcePath);
            }
            return new String(input.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("Erro ao carregar o prompt: " + resourcePath, e);
        }
    }

    @Bean
    public HttpClient assistantHttpClient() {
        return HttpClient.newHttpClient();
    }

    @Bean
    public RateLimiter assistantRateLimiter(Clock clock) {
        return new InMemoryRateLimiter(AssistantConfig.DAILY_LIMIT, clock);
    }

    @Bean
    public IntentExtractor intentExtractor(HttpClient assistantHttpClient, JsonMapper jsonMapper,
                                           AssistantConfig assistantConfig, RateLimiter assistantRateLimiter) {
        return new RateLimitedIntentExtractor(
                new AssistantIntentExtractor(assistantHttpClient, jsonMapper, assistantConfig.getApiKey()),
                assistantRateLimiter
        );
    }

    @Bean
    public AnswerFormatter answerFormatter(HttpClient assistantHttpClient, JsonMapper jsonMapper,
                                           AssistantConfig assistantConfig, RateLimiter assistantRateLimiter) {
        return new RateLimitedAnswerFormatter(
                new AssistantAnswerFormatter(assistantHttpClient, jsonMapper, assistantConfig.getApiKey()),
                assistantRateLimiter
        );
    }

    @Bean
    public TaskFilterResolver taskFilterResolver() {
        return new TaskFilterResolver();
    }

    @Bean
    public TaskAssistantOrchestrator taskAssistantOrchestrator(IntentExtractor intentExtractor,
                                                               AnswerFormatter answerFormatter,
                                                               TaskFilterResolver taskFilterResolver,
                                                               ListTasksUseCase listTasksUseCase,
                                                               JsonMapper jsonMapper,
                                                               Clock clock) {
        String systemInstructions = loadInstructions("prompts/task-assistant-system-instructions.txt");
        String answerFormatterInstructions = loadInstructions("prompts/task-assistant-answer-formatter-instructions.txt");

        return new TaskAssistantOrchestrator(
                intentExtractor, answerFormatter, taskFilterResolver,
                listTasksUseCase, jsonMapper,
                systemInstructions, answerFormatterInstructions, clock
        );
    }

    @Bean
    public Gson assistantGson() {
        return new GsonBuilder()
                .registerTypeAdapter(TaskSuggestion.class, new TaskSuggestionAdapter())
                .registerTypeAdapter(LocalDateTime.class, new LocalDateTimeTypeAdapter())
                .create();
    }

    @Bean(destroyMethod = "close")
    public JedisPool jedisPool() {
        return new JedisPool("localhost", 6379);
    }

    @Bean
    public ObjectMapper objectMapper() {
        var mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return mapper;
    }

    @Bean
    public AssistantSessionRepository assistantSessionRepository(JedisPool jedisPool, ObjectMapper objectMapper) {
        return new RedisAssistantSessionRepository(jedisPool, objectMapper);
    }

    @Bean
    public SendMessageToAssistantUseCase sendMessageToAssistantUseCase(AssistantSessionRepository assistantSessionRepository,
                                                                       TaskAssistantOrchestrator taskAssistantOrchestrator,
                                                                       ListTasksUseCase listTasksUseCase) {
        return new SendMessageToAssistantUseCase(assistantSessionRepository, taskAssistantOrchestrator, listTasksUseCase);
    }

    @Bean
    public StartTaskUseCase startTaskUseCase(TaskRepository taskRepository) {
        return new StartTaskUseCase(taskRepository);
    }

    @Bean
    public CompleteTaskUseCase completeTaskUseCase(TaskRepository taskRepository, CancelNotificationsUseCase cancelNotificationsUseCase) {
        return new CompleteTaskUseCase(taskRepository, cancelNotificationsUseCase);
    }

    @Bean
    public ConfirmTaskSuggestionUseCase confirmTaskSuggestionUseCase(AssistantSessionRepository assistantSessionRepository,
                                                                     CreateTaskUseCase createTaskUseCase,
                                                                     UpdateTaskDetailsUseCase updateTaskDetailsUseCase,
                                                                     DeleteTaskUseCase deleteTaskUseCase,
                                                                     StartTaskUseCase startTaskUseCase,
                                                                     CompleteTaskUseCase completeTaskUseCase) {
        return new ConfirmTaskSuggestionUseCase(
                assistantSessionRepository, createTaskUseCase, updateTaskDetailsUseCase,
                deleteTaskUseCase, startTaskUseCase, completeTaskUseCase
        );
    }

    @Bean
    public RejectTaskSuggestionUseCase rejectTaskSuggestionUseCase(AssistantSessionRepository assistantSessionRepository) {
        return new RejectTaskSuggestionUseCase(assistantSessionRepository);
    }

    @Bean
    public JsonMapper jsonMapper() {
        return new GsonJsonMapper();
    }
}
package com.taskmanager.infrastructure.assistant;

import com.taskmanager.domain.assistant.IntentExtractor;
import com.taskmanager.infrastructure.config.AssistantConfig;
import com.taskmanager.infrastructure.http.json.JsonMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;

public class AssistantIntentExtractor implements IntentExtractor {

    private static final String ENDPOINT =
            "https://openrouter.ai/api/v1/chat/completions";

    private static final int MAX_ATTEMPTS = 2;

    private final HttpClient httpClient;
    private final JsonMapper jsonMapper;
    private final String apiKey;

    public AssistantIntentExtractor(HttpClient httpClient, JsonMapper jsonMapper, String apiKey) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException(
                    "ASSISTANT_API_KEY não configurada. Defina a variável de ambiente antes de iniciar a aplicação.");
        }
        this.httpClient = httpClient;
        this.jsonMapper = jsonMapper;
        this.apiKey = apiKey;
    }

    @Override
    public String extract(String instructions, String userMessage) {
        RuntimeException lastFailure = null;

        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            try {
                return callModel(instructions, userMessage);
            } catch (InvalidIntentResponseException e) {
                lastFailure = e;
                System.out.println("Tentativa " + attempt + " de " + MAX_ATTEMPTS
                        + " retornou resposta inválida (não-JSON). " +
                        (attempt < MAX_ATTEMPTS ? "Tentando novamente..." : "Sem mais tentativas."));
            }
        }

        throw lastFailure;
    }

    private String callModel(String instructions, String userMessage) {
        String prompt = instructions + "\n\nMensagem do usuário: " + userMessage;

        Map<String, Object> body = Map.of(
                "model", AssistantConfig.MODEL,
                "messages", List.of(
                        Map.of(
                                "role", "user",
                                "content", prompt
                        )
                ),
                "reasoning", Map.of("exclude", true)
        );

        // debug
        System.out.println(jsonMapper.toJson(body));

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(ENDPOINT))
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .timeout(Duration.ofSeconds(15))
                .POST(HttpRequest.BodyPublishers.ofString(jsonMapper.toJson(body)))
                .build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            // debug
            System.out.println("ASSISTANT raw response: " + response.body());

            if (response.statusCode() == 429) {
                throw buildRateLimitException(response.body());
            }

            if (response.statusCode() != 200) {
                throw new AssistantRequestFailedException(
                        "ASSISTANT retornou status " + response.statusCode() + ": " + response.body());
            }

            return extractTextFromResponse(response.body());

        } catch (java.io.IOException | InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AssistantRequestFailedException("Falha ao chamar ASSISTANT API", e);
        }
    }

    @SuppressWarnings("unchecked")
    private AssistantRateLimitExceededException buildRateLimitException(String rawJson) {
        try {
            Map<String, Object> parsed = jsonMapper.fromJson(rawJson, Map.class);
            Map<String, Object> error = (Map<String, Object>) parsed.get("error");
            Map<String, Object> metadata = (Map<String, Object>) error.get("metadata");
            Map<String, Object> headers = (Map<String, Object>) metadata.get("headers");
            String resetMillisString = (String) headers.get("X-RateLimit-Reset");

            long resetMillis = Long.parseLong(resetMillisString);
            LocalDateTime resetsAt = Instant.ofEpochMilli(resetMillis)
                    .atZone(ZoneId.systemDefault())
                    .toLocalDateTime();

            return new AssistantRateLimitExceededException(
                    "Limite de requisições do provedor atingido", resetsAt);

        } catch (Exception parseError) {
            return new AssistantRateLimitExceededException(
                    "Limite de requisições do provedor atingido", null);
        }
    }

    @SuppressWarnings("unchecked")
    private String extractTextFromResponse(String rawJson) {

        Map<String, Object> parsed =
                jsonMapper.fromJson(rawJson, Map.class);

        List<Map<String, Object>> choices =
                (List<Map<String, Object>>) parsed.get("choices");

        if (choices == null || choices.isEmpty()) {
            throw new AssistantRequestFailedException(
                    "ASSISTANT não retornou choices na resposta"
            );
        }

        Map<String, Object> message =
                (Map<String, Object>) choices.get(0).get("message");

        String content = (String) message.get("content");

        if (!looksLikeJson(content)) {
            throw new InvalidIntentResponseException(
                    "ASSISTANT retornou conteúdo que não parece JSON: " + content);
        }

        return content;
    }

    private boolean looksLikeJson(String content) {
        if (content == null) {
            return false;
        }
        String trimmed = content.strip();
        return trimmed.startsWith("{") || trimmed.startsWith("```");
    }
}
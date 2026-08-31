package com.taskmanager.infrastructure.assistant;

import com.taskmanager.domain.assistant.AnswerFormatter;
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

public class AssistantAnswerFormatter implements AnswerFormatter {

    private static final String ENDPOINT =
            "https://openrouter.ai/api/v1/chat/completions";

    private static final int MAX_ATTEMPTS = 2;

    private final HttpClient httpClient;
    private final JsonMapper jsonMapper;
    private final String apiKey;

    public AssistantAnswerFormatter(HttpClient httpClient, JsonMapper jsonMapper, String apiKey) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException(
                    "ASSISTANT_API_KEY não configurada. Defina a variável de ambiente antes de iniciar a aplicação.");
        }
        this.httpClient = httpClient;
        this.jsonMapper = jsonMapper;
        this.apiKey = apiKey;
    }

    @Override
    public String format(String instructions, String data) {
        RuntimeException lastFailure = null;

        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            try {
                return callModel(instructions, data);
            } catch (ResponseTruncatedException e) {
                lastFailure = e;
                System.out.println("Tentativa " + attempt + " de " + MAX_ATTEMPTS
                        + " truncada por limite de tokens. " +
                        (attempt < MAX_ATTEMPTS ? "Tentando novamente..." : "Sem mais tentativas."));
            } catch (UnusableModelResponseException e) {
                lastFailure = e;
                System.out.println("Tentativa " + attempt + " de " + MAX_ATTEMPTS
                        + " retornou resposta de modelo inadequado (ex: classificador de safety). " +
                        (attempt < MAX_ATTEMPTS ? "Tentando novamente..." : "Sem mais tentativas."));
            }
        }

        throw lastFailure;
    }

    private String callModel(String instructions, String data) {
        String prompt = instructions + "\n\nDados: " + data;

        Map<String, Object> body = Map.of(
                "model", AssistantConfig.MODEL,
                "messages", List.of(
                        Map.of(
                                "role", "user",
                                "content", prompt
                        )
                ),
                "max_tokens", 1024,
                // Pede ao provedor para pular a etapa de raciocínio estendido,
                // caso o modelo sorteado pelo auto-router a suporte. Isso evita
                // que uma fatia grande do max_tokens seja consumida "pensando"
                // antes de gerar a resposta final, reduzindo o risco de corte.
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
        Map<String, Object> parsed = jsonMapper.fromJson(rawJson, Map.class);
        List<Map<String, Object>> choices = (List<Map<String, Object>>) parsed.get("choices");

        if (choices == null || choices.isEmpty()) {
            throw new AssistantRequestFailedException("ASSISTANT não retornou choices na resposta");
        }

        Map<String, Object> choice = choices.get(0);
        String finishReason = (String) choice.get("finish_reason");

        if ("length".equals(finishReason)) {
            throw new ResponseTruncatedException(
                    "Resposta truncada por limite de tokens (finish_reason=length)");
        }

        Map<String, Object> message = (Map<String, Object>) choice.get("message");
        String content = (String) message.get("content");

        if (looksLikeSafetyModelResponse(content)) {
            throw new UnusableModelResponseException(
                    "ASSISTANT retornou resposta de modelo inadequado (não é um formatador de texto): " + content);
        }

        return content;
    }

    private boolean looksLikeSafetyModelResponse(String content) {
        if (content == null) {
            return true;
        }
        String normalized = content.strip();
        return normalized.regionMatches(true, 0, "User Safety:", 0, "User Safety:".length())
                || normalized.length() < 3;
    }
}
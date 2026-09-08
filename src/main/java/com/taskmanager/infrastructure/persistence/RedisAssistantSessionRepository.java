package com.taskmanager.infrastructure.persistence;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taskmanager.domain.assistant.AssistantSession;
import com.taskmanager.domain.repositories.AssistantSessionRepository;
import redis.clients.jedis.JedisPool;

import java.util.Optional;

public class RedisAssistantSessionRepository implements AssistantSessionRepository {

    private static final String KEY_PREFIX = "assistant:session:";
    private static final int TTL_SECONDS = 1800;

    private final JedisPool jedisPool;
    private final ObjectMapper objectMapper;

    public RedisAssistantSessionRepository(JedisPool jedisPool, ObjectMapper objectMapper) {
        this.jedisPool = jedisPool;
        this.objectMapper = objectMapper;
    }

    @Override
    public Optional<AssistantSession> find(String token) {
        try (var jedis = jedisPool.getResource()) {
            String json = jedis.get(KEY_PREFIX + token);
            if (json == null) {
                return Optional.empty();
            }
            return Optional.of(objectMapper.readValue(json, AssistantSession.class));
        } catch (Exception e) {
            throw new IllegalStateException("Falha ao desserializar sessão do assistente. token=" + token, e);
        }
    }

    @Override
    public void save(String token, AssistantSession session) {
        try (var jedis = jedisPool.getResource()) {
            String json = objectMapper.writeValueAsString(session);
            jedis.setex(KEY_PREFIX + token, TTL_SECONDS, json);
        } catch (Exception e) {
            throw new IllegalStateException("Falha ao serializar sessão do assistente. token=" + token, e);
        }
    }

    @Override
    public void delete(String token) {
        try (var jedis = jedisPool.getResource()) {
            jedis.del(KEY_PREFIX + token);
        }
    }
}
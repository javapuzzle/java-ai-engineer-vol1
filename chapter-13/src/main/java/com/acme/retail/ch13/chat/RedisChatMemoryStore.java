package com.acme.retail.ch13.chat;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Chat memory backed by Redis.
 *
 * <p>Each session's turns live in a Redis LIST at key
 * {@code chat:memory:<sessionId>}, one JSON-encoded {@link ChatMessage} per
 * element, oldest first. Redis is a good fit here for the same reason it
 * was a good fit for caching in Chapter 03: it is fast, it is shared across
 * every instance of the application, and a list with push/trim/range
 * operations maps directly onto "recent conversation turns."
 *
 * <p>Every {@link #append} call does three things:
 * <ol>
 *   <li>Pushes the new turn onto the end of the list.</li>
 *   <li>Trims the list down to {@code chat.memory.max-turns} entries, so
 *       the history cannot grow without bound.</li>
 *   <li>Refreshes the key's time-to-live (TTL), so an abandoned session is
 *       cleaned up automatically instead of sitting in Redis forever.</li>
 * </ol>
 */
public class RedisChatMemoryStore implements ChatMemoryStore {

    private static final String KEY_PREFIX = "chat:memory:";

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final ChatMemoryProperties properties;

    public RedisChatMemoryStore(StringRedisTemplate redisTemplate,
                                 ObjectMapper objectMapper,
                                 ChatMemoryProperties properties) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.properties = properties;
    }

    @Override
    public List<ChatMessage> load(String sessionId) {
        List<String> raw = redisTemplate.opsForList().range(keyFor(sessionId), 0, -1);
        if (raw == null || raw.isEmpty()) {
            return List.of();
        }
        List<ChatMessage> turns = new ArrayList<>(raw.size());
        for (String json : raw) {
            turns.add(readMessage(json));
        }
        return turns;
    }

    @Override
    public void append(String sessionId, ChatMessage message) {
        String key = keyFor(sessionId);

        redisTemplate.opsForList().rightPush(key, writeMessage(message));

        // Keep only the most recent maxTurns entries. Negative indexes in
        // Redis count from the end of the list, so (-maxTurns, -1) means
        // "the last maxTurns elements."
        redisTemplate.opsForList().trim(key, -properties.getMaxTurns(), -1);

        redisTemplate.expire(key, properties.getTtl().toSeconds(), TimeUnit.SECONDS);
    }

    private String keyFor(String sessionId) {
        return KEY_PREFIX + sessionId;
    }

    private String writeMessage(ChatMessage message) {
        try {
            return objectMapper.writeValueAsString(message);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Could not serialize chat message for Redis", e);
        }
    }

    private ChatMessage readMessage(String json) {
        try {
            return objectMapper.readValue(json, ChatMessage.class);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Could not deserialize chat message from Redis", e);
        }
    }
}

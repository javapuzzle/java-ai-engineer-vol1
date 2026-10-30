package com.acme.retail.ch13.chat;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Proves the two things {@link RedisChatMemoryStore} exists to do:
 * <ul>
 *   <li>Turns come back out in the order they were appended.</li>
 *   <li>History never grows past {@code chat.memory.max-turns} -- the
 *       oldest turns fall off once the cap is reached.</li>
 * </ul>
 *
 * <p>This uses a real Redis container via Testcontainers (requires Docker),
 * the same way a chapter-11-style test would -- Chapter 13 introduces Redis
 * as a dependency the code actually talks to, so the test should talk to a
 * real Redis rather than an in-memory fake that could quietly drift from
 * real Redis LIST semantics.
 */
@Testcontainers(disabledWithoutDocker = true)
class RedisChatMemoryStoreTest {

    static final GenericContainer<?> REDIS =
            new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
                    .withExposedPorts(6379);

    static LettuceConnectionFactory connectionFactory;
    static StringRedisTemplate redisTemplate;

    @BeforeAll
    static void startRedis() {
        REDIS.start();
        connectionFactory = new LettuceConnectionFactory(REDIS.getHost(), REDIS.getMappedPort(6379));
        connectionFactory.afterPropertiesSet();
        redisTemplate = new StringRedisTemplate(connectionFactory);
    }

    @AfterAll
    static void stopRedis() {
        connectionFactory.destroy();
        REDIS.stop();
    }

    private RedisChatMemoryStore newStore(int maxTurns) {
        ChatMemoryProperties properties = new ChatMemoryProperties();
        properties.setMaxTurns(maxTurns);
        properties.setTtl(Duration.ofMinutes(5));
        ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
        return new RedisChatMemoryStore(redisTemplate, objectMapper, properties);
    }

    @Test
    void appendsAndLoadsTurnsInOrder() {
        RedisChatMemoryStore store = newStore(10);
        String sessionId = "session-abc";

        store.append(sessionId, new ChatMessage(ChatMessage.Role.USER, "I want black running shoes.", Instant.now()));
        store.append(sessionId, new ChatMessage(ChatMessage.Role.ASSISTANT, "Sure.", Instant.now()));
        store.append(sessionId, new ChatMessage(ChatMessage.Role.USER, "Only size 8.", Instant.now()));

        List<ChatMessage> history = store.load(sessionId);

        assertThat(history).hasSize(3);
        assertThat(history.get(0).content()).isEqualTo("I want black running shoes.");
        assertThat(history.get(0).role()).isEqualTo(ChatMessage.Role.USER);
        assertThat(history.get(1).content()).isEqualTo("Sure.");
        assertThat(history.get(2).content()).isEqualTo("Only size 8.");
    }

    @Test
    void capsHistoryAtConfiguredMaxTurns() {
        RedisChatMemoryStore store = newStore(4);
        String sessionId = "session-cap";

        for (int i = 1; i <= 10; i++) {
            store.append(sessionId, new ChatMessage(ChatMessage.Role.USER, "message-" + i, Instant.now()));
        }

        List<ChatMessage> history = store.load(sessionId);

        // Only the most recent 4 of the 10 appended turns should survive.
        assertThat(history).hasSize(4);
        assertThat(history.get(0).content()).isEqualTo("message-7");
        assertThat(history.get(3).content()).isEqualTo("message-10");
    }

    @Test
    void unknownSessionHasEmptyHistory() {
        RedisChatMemoryStore store = newStore(10);

        List<ChatMessage> history = store.load("session-never-seen");

        assertThat(history).isEmpty();
    }
}

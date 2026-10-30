package com.acme.retail.ch13.chat;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * Wires the Redis-backed {@link ChatMemoryStore} bean.
 *
 * <p>{@code StringRedisTemplate} and the {@code ObjectMapper} are already
 * auto-configured by {@code spring-boot-starter-data-redis} and
 * {@code spring-boot-starter-web} respectively, so this class only has to
 * combine them with {@link ChatMemoryProperties}.
 */
@Configuration
@EnableConfigurationProperties(ChatMemoryProperties.class)
public class ChatMemoryConfig {

    @Bean
    public ChatMemoryStore chatMemoryStore(StringRedisTemplate redisTemplate,
                                            ObjectMapper objectMapper,
                                            ChatMemoryProperties properties) {
        return new RedisChatMemoryStore(redisTemplate, objectMapper, properties);
    }
}

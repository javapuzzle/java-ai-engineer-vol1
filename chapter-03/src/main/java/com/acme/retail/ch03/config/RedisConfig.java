package com.acme.retail.ch03.config;

import com.acme.retail.ch03.product.ProductView;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class RedisConfig {

    /**
     * Keys are plain strings ("product:42"). Values are serialized as JSON
     * so anyone can read what is in Redis with a plain {@code redis-cli GET},
     * instead of opaque Java-serialized bytes.
     */
    @Bean
    public RedisTemplate<String, ProductView> productRedisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, ProductView> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new GenericJackson2JsonRedisSerializer());
        return template;
    }
}

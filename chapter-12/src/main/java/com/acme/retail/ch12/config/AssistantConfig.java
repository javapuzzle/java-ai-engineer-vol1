package com.acme.retail.ch12.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Builds the {@link ChatClient} Sarah's assistant calls.
 *
 * <p>Spring AI's OpenAI starter auto-configures a {@link ChatModel} bean
 * from the {@code spring.ai.openai.*} properties in application.yml. This
 * class wraps that {@code ChatModel} in a {@code ChatClient}, the fluent,
 * higher-level API {@code AssistantService} actually calls, and the same
 * API that exposes both {@code .call()} (wait for the full answer) and
 * {@code .stream()} (get the answer as it is generated).
 */
@Configuration
public class AssistantConfig {

    @Bean
    public ChatClient chatClient(ChatModel chatModel) {
        return ChatClient.builder(chatModel).build();
    }
}

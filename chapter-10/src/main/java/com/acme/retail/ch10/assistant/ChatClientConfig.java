package com.acme.retail.ch10.assistant;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Builds the {@link ChatClient} bean used by {@link ShoppingAssistantController}.
 *
 * <p>Spring AI auto-configures a {@code ChatClient.Builder} for us from the
 * {@code spring.ai.openai.*} properties in {@code application.yml}. We only
 * need to call {@code .build()} on it once and expose the result as a bean,
 * so the controller can depend on a plain {@link ChatClient} -- which is
 * also what makes the controller easy to unit test with a mock.
 */
@Configuration
public class ChatClientConfig {

    @Bean
    ChatClient shoppingAssistantChatClient(ChatClient.Builder chatClientBuilder) {
        return chatClientBuilder.build();
    }
}

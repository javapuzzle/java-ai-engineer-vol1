package com.acme.retail.ch16.assistant;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;

/**
 * The real {@link AiModelClient}: calls the AI provider through Spring
 * AI's {@code ChatClient}.
 *
 * <p>This class knows nothing about timeouts, retries, or circuit
 * breakers. That separation matters: resilience is applied one layer up,
 * in {@link ShoppingAssistantService}, so this class stays a plain,
 * readable wrapper around the AI call.
 */
@Component
public class SpringAiModelClient implements AiModelClient {

    private final ChatClient chatClient;

    public SpringAiModelClient(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.build();
    }

    @Override
    public String complete(String systemPrompt, String question) {
        return chatClient.prompt()
                .system(systemPrompt)
                .user(question)
                .call()
                .content();
    }
}
